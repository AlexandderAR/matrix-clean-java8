# Matrix

Gradle-проект плагина Matrix 7.25.1. Исходники находятся в src/main/java,
ресурсы и шаблоны конфигурации — в src/main/resources.

## Сборка на Windows

Нужны полные JDK 8 и JDK 21. Задайте реальные пути к установленным JDK:

```powershell
.\build.bat 'C:\Java\jdk8' 'C:\Java\jdk21'
```

Скрипт запускает assemble через включённый Gradle 8.14.3 без доступа к сети.
Готовый плагин: build/libs/Matrix-7.25.1-source-clean.jar.

JDK 8 запускает Gradle и предоставляет Java 8 API. Компилятор JDK 21 нужен
для современных серверных SDK; байткод самого плагина имеет target Java 8.
JDK в проект не включены.

Также можно использовать стандартный wrapper:

```powershell
$env:JAVA_HOME = 'C:\Java\jdk8'
$env:MATRIX_JAVA8_HOME = $env:JAVA_HOME
$env:MATRIX_COMPILER_JAVA_HOME = 'C:\Java\jdk21'
.\gradlew.bat --no-daemon assemble
```

При первом использовании wrapper может загрузить Gradle. build.bat использует
уже включённый дистрибутив и работает без скачивания зависимостей.

## Структура

- src/main/java — код плагина;
- src/main/resources — plugin descriptors, ресурсы и шаблоны настроек;
- lib/matrix-thirdparty-7.25.1.jar — runtime-библиотеки;
- lib/compile — локальные SDK и библиотеки только для компиляции;
- build-support — инструменты подготовки библиотек компиляции;
- gradle/wrapper — стандартный wrapper;
- tools/gradle-8.14.3 — Gradle для автономной сборки;
- DEPENDENCIES.json — состав и контрольные суммы зависимостей.

Старые серверные адаптеры собираются отдельно с соответствующими SDK.
Серверные SDK не включаются в результирующий плагин. Оригинальный JAR Matrix
как зависимость сборки не используется. Согласованная offline-конфигурация
сохранена. Сборка прошла; полная эквивалентность во всех сценариях не заявляется.
