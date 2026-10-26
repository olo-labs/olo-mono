"""Ephemeral local TLS fixture. Keytool passwords are read from the environment, not command arguments."""
import os, pathlib, subprocess
def prepare(folder, password):
    folder=pathlib.Path(folder);folder.mkdir(parents=True,exist_ok=True)
    keytool=pathlib.Path(os.environ['JAVA_HOME'])/'bin/keytool.exe' if os.name=='nt' and os.environ.get('JAVA_HOME') else 'keytool'
    env=dict(os.environ,KNOWLEDGE_PKI_PASSWORD=password)
    def run(*args):
        subprocess.run([str(keytool),*map(str,args)],env=env,check=True,capture_output=True)
    run('-genkeypair','-alias','root','-keyalg','RSA','-keysize','2048','-dname','CN=Knowledge fixture root',
        '-ext','bc=ca:true','-validity','2','-keystore',folder/'root.p12','-storetype','PKCS12','-storepass:env','KNOWLEDGE_PKI_PASSWORD')
    run('-exportcert','-alias','root','-keystore',folder/'root.p12','-storepass:env','KNOWLEDGE_PKI_PASSWORD','-file',folder/'root.cer')
    run('-importcert','-noprompt','-alias','root','-file',folder/'root.cer','-keystore',folder/'trust.p12','-storetype','PKCS12','-storepass:env','KNOWLEDGE_PKI_PASSWORD')
    for name in ['server','executor']:
        run('-genkeypair','-alias','identity','-keyalg','RSA','-keysize','2048','-dname',f'CN={name}',
            '-validity','2','-keystore',folder/f'{name}.p12','-storetype','PKCS12','-storepass:env','KNOWLEDGE_PKI_PASSWORD')
        run('-certreq','-alias','identity','-keystore',folder/f'{name}.p12','-storepass:env','KNOWLEDGE_PKI_PASSWORD','-file',folder/f'{name}.csr')
        run('-gencert','-alias','root','-keystore',folder/'root.p12','-storepass:env','KNOWLEDGE_PKI_PASSWORD',
            '-infile',folder/f'{name}.csr','-outfile',folder/f'{name}.cer','-validity','2','-ext','SAN=DNS:localhost,IP:127.0.0.1','-ext','EKU=serverAuth,clientAuth')
        run('-importcert','-noprompt','-alias','root','-file',folder/'root.cer','-keystore',folder/f'{name}.p12','-storepass:env','KNOWLEDGE_PKI_PASSWORD')
        run('-importcert','-alias','identity','-file',folder/f'{name}.cer','-keystore',folder/f'{name}.p12','-storepass:env','KNOWLEDGE_PKI_PASSWORD')
    return str(folder)
