#!/usr/bin/env bash
set -eu; cd "$(dirname "$0")"
M2=/mnt/MistiqueSSD/minecraft/m2; mkdir -p $M2
docker run --rm -v "$PWD":/w -v $M2:/root/.m2 -w /w maven:3.9-eclipse-temurin-17 mvn -q -B package 2>&1 | grep -vE '^\[INFO\]|Downloading|Downloaded|Progress' | tail -40 || true
ls -la target/RookBot.jar
