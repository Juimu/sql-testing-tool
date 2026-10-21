#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /workspace/tools
python3 - <<'PY'
import hashlib, os, pathlib, urllib.request, urllib.parse, xml.etree.ElementTree as ET
p=pathlib.Path('/workspace/tools')
def download(url, name, algorithm):
    archive=p/name
    with urllib.request.urlopen(url, timeout=120) as response, archive.open('wb') as output:
        import shutil
        shutil.copyfileobj(response,output)
    with urllib.request.urlopen(url+'.'+algorithm,timeout=30) as response:
        expected=response.read().decode().strip().split()[0]
    actual=hashlib.file_digest(archive.open('rb'),algorithm).hexdigest()
    if actual!=expected: archive.unlink(); raise RuntimeError('Official artifact checksum mismatch')
if not (p/'apache-maven-3.9.11/bin/mvn').exists():
    download('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz','maven.tar.gz','sha512')
if not list(p.glob('jdk-25*/bin/javac')):
    download('https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz','jdk25.tar.gz','sha256')
root=ET.Element('settings',xmlns='http://maven.apache.org/SETTINGS/1.2.0')
ET.SubElement(root,'localRepository').text='/workspace/.m2/repository'
proxy=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''))
if proxy.hostname:
    if proxy.username or proxy.password: raise RuntimeError('Credential-bearing proxy URLs require secure Maven configuration')
    node=ET.SubElement(ET.SubElement(root,'proxies'),'proxy')
    for key,value in dict(id='cloud-proxy',active='true',protocol='http',host=proxy.hostname,port=str(proxy.port or 80),nonProxyHosts='localhost|127.0.0.1').items():
        ET.SubElement(node,key).text=value
ET.ElementTree(root).write(p/'maven-settings.xml',encoding='unicode')
PY
if [ ! -x /workspace/tools/apache-maven-3.9.11/bin/mvn ]; then tar -xzf /workspace/tools/maven.tar.gz -C /workspace/tools; fi
if ! compgen -G '/workspace/tools/jdk-25*/bin/javac' >/dev/null; then tar -xzf /workspace/tools/jdk25.tar.gz -C /workspace/tools; fi
bash scripts/cloud-mvn.sh -B -ntp -Pheadless verify
