# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Prepare the exact source-verified, disposable README tour environment; no runtime credentials are built in."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid

MONO=Path(__file__).resolve().parents[2]
OWNER='io.olo.qualification.owner=olo-knowledge'


def run(args,cwd=MONO,env=None,capture=False):
    return subprocess.run(list(map(str,args)),cwd=cwd,env=env,check=True,text=True,encoding='utf-8',capture_output=capture)


def image_info(image):
    result=subprocess.run(['docker','image','inspect',image],capture_output=True,text=True,encoding='utf-8')
    return json.loads(result.stdout)[0] if result.returncode==0 else None


def own_tag(tag):
    info=image_info(tag)
    if info and (info['Config'].get('Labels') or {}).get('io.olo.qualification.owner')!='olo-knowledge':
        raise ValueError('Refusing to replace an unowned image: '+tag)


def prepare(source,gradle8=None,gradle9=None):
    source=source.resolve(strict=True)
    gradle8=gradle8 or MONO/'olo-worker/gradlew.bat'
    gradle9=gradle9 or source/'gradlew.bat'
    env=dict(os.environ,TOOLGATE_SOURCE=str(source),PYTHONUTF8='1')
    logs=MONO/'build/ui-tour-images';logs.mkdir(parents=True,exist_ok=True)
    for folder in ('olo-chat','olo-ui/olo-ui'):
        npm=shutil.which('npm.cmd' if os.name=='nt' else 'npm')
        if not npm:raise RuntimeError('Node.js/npm is required')
        build_env=dict(env,VITE_API_BASE=' ') if folder=='olo-chat' else env
        run([npm,'ci'],cwd=MONO/folder,env=build_env)
        run([npm,'run','build'],cwd=MONO/folder,env=build_env)
    run([gradle8,'-p',MONO/'olo-ui/olo-be','bootJar'],env=env)
    run([gradle9,'publishJavaSdkLocally',':contracts-java:publishMavenJavaPublicationToContractProofRepository',
         '-PcontractsPublicationVersion=0.10.0-dev'],cwd=source,env=env)
    run([gradle8,'-p',MONO/'olo-knowledge',':olo-memory:installDist',':olo-rag:installDist','--refresh-dependencies'],env=env)

    inputs=['olo-spi','olo-annotation','olo-definition','olo-workflow-input','olo-toolgate-client','gradle']
    apps=[('ololab/olo:latest','olo-backend:olo-qualification',['olo',*inputs]),
          ('ololab/olo-worker:latest','olo-worker:olo-qualification',
           ['olo-worker','olo-core','olo-kernel','olo-kernel-context','olo-bootstrap','olo-worker-configuration',*inputs])]
    for published,tag,paths in apps:
        own_tag(tag);run(['docker','pull',published])
        revision=(image_info(published)['Config'].get('Labels') or {}).get('org.opencontainers.image.revision')
        if not revision:raise ValueError('Published image has no source revision')
        run(['git','cat-file','-e',revision+'^{commit}'])
        diff=run(['git','diff',revision,'--',*paths],capture=True).stdout
        dockerfile=logs/(tag.split(':')[0]+'.Dockerfile')
        context=[]
        recipe='FROM '+published+'\n'
        if diff:
            if tag.startswith('olo-backend:'):
                run([gradle8,'-p',MONO/'olo','test','bootJar'],env=env)
                context=['--build-context','runtime-build='+str(MONO/'olo/build/libs')]
                recipe+='COPY --from=runtime-build /olo-backend-0.0.1-SNAPSHOT.jar /app/app.jar\n'
            else:
                run([gradle8,'-p',MONO/'olo-worker','test','installDist'],env=env)
                context=['--build-context','runtime-build='+str(MONO/'olo-worker/build/install/olo-worker')]
                recipe+='COPY --from=runtime-build / /app/\n'
        dockerfile.write_text(recipe,encoding='utf-8')
        run(['docker','build',*context,'--label',OWNER,'--label','io.olo.qualification.source-revision='+revision,
             '--label','io.olo.qualification.source-diff-sha256='+hashlib.sha256(diff.encode()).hexdigest(),
             '-f',dockerfile,'-t',tag,logs])

    # The pinned runtime supplies unchanged OS/Python infrastructure. All native binaries, Java code and core packages rebuild.
    run(['git','diff','--exit-code','4da79ab2','HEAD','--','apps/quickstart','apps/control-plane/gradle.lockfile'],cwd=source,capture=True)
    # Build local fixes as well as committed sources; record their identity on the image.
    source_diff=run(['git','diff','HEAD','--','apps/control-plane','apps/admin-ui','apps/gateway','apps/tool-host',
         'packages','tool-sdk','core-mcp','config/initial'],cwd=source,capture=True).stdout
    run(['node','tools/ui/build.mjs'],cwd=source,env=env)
    run([gradle9,'-PprebuiltAdminUi=true','-Dquarkus.profile=quickstart',':control-plane:quarkusBuild'],cwd=source,env=env)
    tag='olo-toolgate-quickstart:olo-m8';own_tag(tag)
    revision=run(['git','rev-parse','HEAD'],cwd=source,capture=True).stdout.strip()
    run(['docker','build','--build-context','control-app=apps/control-plane/build/quarkus-app',
         '-f','tools/docs/ui-tour.Dockerfile','--label',OWNER,'--label','io.olo.qualification.source-revision='+revision,
         '--label','io.olo.qualification.source-diff-sha256='+hashlib.sha256(source_diff.encode()).hexdigest(),
         '--label','org.opencontainers.image.revision='+revision,'--label','org.opencontainers.image.version=0.10.0-dev','-t',tag,'.'],cwd=source,env=env)
    container='olo-ui-tour-artifact-'+uuid.uuid4().hex[:12]
    target=source/'examples/mocked-tools/server-recovery/build/toolgate/server-recovery-0.1.0.tgpkg'
    target.parent.mkdir(parents=True,exist_ok=True)
    try:
        run(['docker','create','--label',OWNER,'--name',container,tag])
        run(['docker','cp',container+':/opt/toolgate/ui-tour/server-recovery-0.1.0.tgpkg',target])
    finally:
        subprocess.run(['docker','rm','-f','-v',container],check=False,capture_output=True)
    print('Current-source UI tour images, clients, services and official incident package prepared.',flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--toolgate-source',type=Path,required=True)
    parser.add_argument('--gradle8');parser.add_argument('--gradle9')
    args=parser.parse_args();prepare(args.toolgate_source,args.gradle8,args.gradle9)
