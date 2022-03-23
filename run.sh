#!/bin/sh

exec java ${JAVA_OPTS} \
  -javaagent:/app/agent.jar \
  -jar /app/app.jar