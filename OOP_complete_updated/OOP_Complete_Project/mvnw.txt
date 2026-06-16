#!/usr/bin/env bash
set -e
script="$0"
while [ -h "$script" ]; do
  ls=$(ls -ld "$script")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    script="$link"
  else
    script=$(dirname "$script")/"$link"
  fi
done
scriptDir=$(cd "$(dirname "$script")" && pwd)
java -classpath "$scriptDir/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
