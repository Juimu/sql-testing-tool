"""Package the current OS build. Run Maven verify first. No signing/installer claims."""
from pathlib import Path
import hashlib
import json
import os
import platform
import shutil
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
version = ET.parse(root / 'pom.xml').getroot().find('{*}version').text
base = root / 'target' / 'distribution'
if base.exists():
    shutil.rmtree(base)
(base / 'lib').mkdir(parents=True)
shutil.copy2(root / 'target' / f'sqlnext-{version}.jar', base / 'lib')
for jar in (root / 'target' / 'lib').glob('*.jar'):
    shutil.copy2(jar, base / 'lib')
shutil.copy2(root / 'README.md', base)
for sbom in (root / 'target').glob('bom.*'):
    shutil.copy2(sbom, base)
shutil.copytree(root / 'docs', base / 'docs')
(base / 'SQLNext.bat').write_text('@echo off\r\ncd /d "%~dp0"\r\njava --enable-native-access=ALL-UNNAMED -cp "lib/*" dev.sqlnext.Launcher\r\nif errorlevel 1 pause\r\n')
(base / 'SQLNext.sh').write_text('#!/usr/bin/env sh\nset -eu\ncd "$(dirname "$0")"\nexec java --enable-native-access=ALL-UNNAMED -cp "lib/*" dev.sqlnext.Launcher "$@"\n')
(base / 'SQLNext.sh').chmod(0o755)
manifest = {str(p.relative_to(base)): hashlib.sha256(p.read_bytes()).hexdigest() for p in base.rglob('*') if p.is_file()}
(base / 'SHA256SUMS.json').write_text(json.dumps(manifest, indent=2))
archive = root / 'target' / f'SQLNext-{version}-{platform.system().lower()}-{platform.machine()}.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as out:
    for path in sorted(base.rglob('*')):
        if path.is_file():
            out.write(path, Path('SQLNext') / path.relative_to(base))
print(archive)
if '--native' in sys.argv:
    destination = root / 'target' / 'native'
    destination.mkdir(exist_ok=True)
    image = destination / ('SQLNext.app' if platform.system() == 'Darwin' else 'SQLNext')
    if image.exists():
        shutil.rmtree(image)
    jpackage = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('jpackage.exe' if platform.system() == 'Windows' else 'jpackage')) if os.environ.get('JAVA_HOME') else 'jpackage'
    subprocess.run([jpackage, '--type', 'app-image', '--name', 'SQLNext', '--app-version', version,
                    '--input', str(base / 'lib'), '--main-jar', f'sqlnext-{version}.jar',
                    '--main-class', 'dev.sqlnext.Launcher', '--dest', str(destination),
                    '--java-options', '--enable-native-access=ALL-UNNAMED'], check=True)
    print(image)
    native_name = f'SQLNext-{version}-{platform.system().lower()}-native'
    if platform.system() == 'Darwin':
        native_archive = root / 'target' / (native_name + '.zip')
        subprocess.run(['ditto', '-c', '-k', '--sequesterRsrc', '--keepParent', str(image), str(native_archive)], check=True)
    else:
        native_archive = shutil.make_archive(str(root / 'target' / native_name), 'zip' if platform.system() == 'Windows' else 'gztar', root_dir=destination, base_dir=image.name)
    print(native_archive)
