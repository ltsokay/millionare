#!/usr/bin/env bash
# Сборка и запуск игры "Кто хочет стать миллионером?"
# Требуется JDK 11+ и файл драйвера lib/sqlite-jdbc-*.jar
set -e

mkdir -p out

echo "Компиляция..."
javac -encoding UTF-8 -d out -cp "lib/*" src/millionaire/*.java

echo "Запуск..."
java -Dfile.encoding=UTF-8 -cp "out:lib/*" millionaire.Main
