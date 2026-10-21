#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tool_root=/workspace/tools
if [ -d "$tool_root/jdk-25" ]; then
  export JAVA_HOME="$tool_root/jdk-25"
else
  jdk_path=$(find "$tool_root" -maxdepth 1 -type d -name 'jdk-25*' -print -quit)
  if [ -z "$jdk_path" ]; then echo 'Run scripts/cloud-setup.sh first.' >&2; exit 1; fi
  export JAVA_HOME="$jdk_path"
fi
export PATH="$JAVA_HOME/bin:$PATH"
export XDG_CACHE_HOME=/workspace/.cache
export SQLNEXT_DATA="${SQLNEXT_DATA:-/workspace/sqlnext-data}"
mkdir -p "$XDG_CACHE_HOME"
# Retain the platform's managed CA trust when using the downloaded JDK.
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
exec "$tool_root/apache-maven-3.9.11/bin/mvn" -s "$tool_root/maven-settings.xml" "$@"
