"""Stage SDK packages with Gradle, then pack them with the CLI in the owned qualification image."""
import argparse
import json
import pathlib
import subprocess

ROOT=pathlib.Path(__file__).resolve().parents[1]


def build(gradle, image):
    info=json.loads(subprocess.check_output(['docker','image','inspect',image],text=True))[0]
    if (info['Config'].get('Labels') or {}).get('io.olo.qualification.owner')!='olo-knowledge':
        raise ValueError('Package fixture builds require an owned qualification image')
    directory=ROOT/'build/qualification'
    directory.mkdir(parents=True,exist_ok=True)
    init=directory/'stage-packages.init.gradle'
    # Retain the SDK's dependency graph/checks and staging implementation, while using the container CLI for packing.
    init.write_text('''gradle.projectsEvaluated {
    allprojects {
        tasks.matching { it.name == 'toolgatePackage' }.configureEach { task ->
            def arguments = task.argumentProviders.collectMany { it.asArguments().toList() }
            def boundary = arguments.indexOf('--pack')
            if (boundary < 0) throw new GradleException('SDK package arguments do not expose a pack boundary')
            task.argumentProviders.clear()
            task.setArgs(arguments.take(boundary))
        }
    }
}
''')
    subprocess.run([gradle,'-p',str(ROOT/'packages'),'-I',str(init),'build',':memory:toolgatePackage',
                    ':rag:toolgatePackage','--no-daemon','--no-configuration-cache'],check=True)
    for name in ['memory','rag']:
        folder=ROOT/'packages'/name/'build/toolgate'
        manifest=json.loads((folder/'staging/toolgate-package.json').read_text())
        output=manifest['id']+'-'+manifest['version']+'.tgpkg'
        subprocess.run(['docker','run','--rm','--label','io.olo.qualification.owner=olo-knowledge',
            '--network','none','--user','0:0','--entrypoint','/usr/local/bin/toolgate','-v',str(folder)+':/work',image,
            'pack','/work/staging','-o','/work/'+output],check=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gradle',default='gradle')
    parser.add_argument('--image',default='olo-toolgate-quickstart:olo-m8')
    args=parser.parse_args()
    build(args.gradle,args.image)
