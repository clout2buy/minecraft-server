#!/bin/sh
# Rebuild QuestBoard against the 1.20.1 server libs and drop the jar into the mistique repo + local server.
cd "$(dirname "$0")"
L=../../mc-server-1201/libraries
CP="$L/io/papermc/paper/paper-api/1.20.1-R0.1-SNAPSHOT/paper-api-1.20.1-R0.1-SNAPSHOT.jar;$L/net/kyori/adventure-api/4.14.0/adventure-api-4.14.0.jar;$L/net/kyori/adventure-key/4.14.0/adventure-key-4.14.0.jar;$L/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar;$L/net/md-5/bungeecord-chat/1.20-R0.1-deprecated+build.14/bungeecord-chat-1.20-R0.1-deprecated+build.14.jar;$L/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar;../lib/annotations.jar"
rm -rf build && mkdir -p build/classes
javac -encoding UTF-8 -Xlint:-options --release 17 -cp "$CP" -d build/classes $(find src/main/java -name "*.java") || exit 1
cp src/main/resources/*.yml build/classes/
(cd build/classes && jar cf ../../QuestBoard-1.0.0.jar .)
cp QuestBoard-1.0.0.jar ../../mc-server-1201/plugins/
cp QuestBoard-1.0.0.jar ../../minecraft-server/plugins/QuestBoard.jar
echo "built + copied to local server and minecraft-server/plugins/"
