"""Install the pinned optional engine with official PyPI SHA-256 verification."""
import hashlib
import json
import os
from pathlib import Path
import sys
import tarfile
import urllib.request

version = '1.10.10'
base = Path(os.environ.get('SQLNEXT_TOOLS', str(Path.home() / '.sqlnext-tools')))
base.mkdir(parents=True, exist_ok=True)
with urllib.request.urlopen(f'https://pypi.org/pypi/sqlmap/{version}/json', timeout=30) as response:
    metadata = json.load(response)
package = next(item for item in metadata['urls'] if item['packagetype'] == 'sdist')
archive = base / package['filename']
with urllib.request.urlopen(package['url'], timeout=60) as response:
    payload = response.read()
if hashlib.sha256(payload).hexdigest() != package['digests']['sha256']:
    raise RuntimeError('Official SHA-256 verification failed')
archive.write_bytes(payload)
with tarfile.open(archive) as bundle:
    bundle.extractall(base, filter='data')
scripts = list((base / f'sqlmap-{version}').rglob('sqlmap.py'))
if len(scripts) != 1:
    raise RuntimeError('Expected exactly one sqlmap.py entry point')
print('Python executable:', sys.executable)
print('Verified sqlmap script:', scripts[0])
print('Enter these paths in Settings. This installer does not change application settings.')
