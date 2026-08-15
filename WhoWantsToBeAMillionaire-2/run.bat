@echo off
chcp 65001 >nul
rem Сборка и запуск игры "Кто хочет стать миллионером?"
rem Требуется JDK 11+ и файл драйвера lib\sqlite-jdbc-*.jar

if not exist out mkdir out

echo Компиляция...
javac -encoding UTF-8 -d out -cp "lib/*" src/millionaire/*.java
if errorlevel 1 (
    echo Ошибка компиляции. Проверьте, что установлен JDK 11+ и драйвер в папке lib.
    pause
    exit /b 1
)

echo Запуск...
java -Dfile.encoding=UTF-8 -cp "out;lib/*" millionaire.Main
pause
