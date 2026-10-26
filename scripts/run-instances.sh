#!/usr/bin/env bash
cd "$(dirname "$0")/.."

trap 'kill 0' EXIT

./mvnw -q -DskipTests package
mkdir -p logs

PORT=8081 java -jar target/mpesa-spring-0.0.1-SNAPSHOT.jar > logs/8081.log 2>&1 &
PORT=8082 java -jar target/mpesa-spring-0.0.1-SNAPSHOT.jar > logs/8082.log 2>&1 &

echo "Running on :8081 and :8082 (Ctrl-C to stop)"
wait
