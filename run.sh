#!/bin/sh

exec java ${JAVA_OPTS} \
  -XX:+AlwaysActAsServerClassMachine \
  -javaagent:/app/agent.jar \
  -jar /app/app.jar