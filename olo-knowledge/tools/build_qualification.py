"""Build owner-labelled current-source fixture images, so qualification.ps1 stop can remove them."""
import argparse,pathlib,subprocess,json,os

ROOT=pathlib.Path(__file__).resolve().parents[1]
TOOLGATE=pathlib.Path(os.environ.get('TOOLGATE_SOURCE', str(ROOT.parent.parent / 'olo-toolgate')))
CONTROL='olo-toolgate-control:olo-qualification'
QUICKSTART='olo-toolgate-quickstart:olo-m8'
OWNER='io.olo.qualification.owner=olo-knowledge'

def build(source=TOOLGATE):
    source=source.resolve(strict=True)
    if subprocess.check_output(['git','status','--porcelain'],cwd=source,text=True).strip():
        raise ValueError('Qualification requires a clean ToolGate source checkout')
    for image in [CONTROL,QUICKSTART]:
        result=subprocess.run(['docker','image','inspect',image],capture_output=True,text=True)
        if result.returncode==0:
            labels=json.loads(result.stdout)[0]['Config'].get('Labels') or {}
            if labels.get('io.olo.qualification.owner')!='olo-knowledge':
                raise ValueError('Refusing to overwrite an unowned qualification image: '+image)
    version=(source/'VERSION').read_text(encoding='utf-8').strip()
    revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip()
    common=['--label',OWNER,'--build-arg','VERSION='+version,'--build-arg','REVISION='+revision]
    # Build the UI from tracked sources as well; ignored prebuilt assets cannot
    # establish that the tested image matches the recorded Git revision.
    subprocess.run(['docker','build','-f','apps/control-plane/Dockerfile',*common,'--build-arg','QUARKUS_PROFILE=quickstart','-t',CONTROL,'.'],cwd=source,check=True)
    subprocess.run(['docker','build','-f','apps/quickstart/Dockerfile',*common,'--build-arg','CONTROL_IMAGE='+CONTROL,'-t',QUICKSTART,'.'],cwd=source,check=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=pathlib.Path,default=TOOLGATE)
    build(parser.parse_args().source)
