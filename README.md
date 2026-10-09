Matrix
Proyecto Gradle del plugin Matrix 7.25.1. Los fuentes se encuentran en src/main/java, los recursos y plantillas de configuración en src/main/resources.
Compilación en Windows
Se necesitan JDK 8 y JDK 21 completos. Indique las rutas reales de los JDK instalados:
PowerShell.\build.bat 'C:\Java\jdk8' 'C:\Java\jdk21'
El script ejecuta assemble mediante Gradle 8.14.3 incluido, sin acceso a la red.

Plugin listo: build/libs/Matrix-7.25.1-source-clean.jar.
JDK 8 inicia Gradle y proporciona la API de Java 8. El compilador de JDK 21 es necesario para los SDK de servidor modernos; el bytecode del propio plugin tiene target Java 8.

Los JDK no están incluidos en el proyecto.
También se puede usar el wrapper estándar:
PowerShell$env:JAVA_HOME = 'C:\Java\jdk8'
$env:MATRIX_JAVA8_HOME = $env:JAVA_HOME
$env:MATRIX_COMPILER_JAVA_HOME = 'C:\Java\jdk21'
.\gradlew.bat --no-daemon assemble
En el primer uso el wrapper puede descargar Gradle. build.bat utiliza la distribución ya incluida y funciona sin descargar dependencias.
Estructura

src/main/java — código del plugin;
src/main/resources — descriptores del plugin, recursos y plantillas de configuración;
lib/matrix-thirdparty-7.25.1.jar — bibliotecas de runtime;
lib/compile — SDK y bibliotecas locales solo para compilación;
build-support — herramientas de preparación de las bibliotecas de compilación;
gradle/wrapper — wrapper estándar;
tools/gradle-8.14.3 — Gradle para compilación autónoma;
DEPENDENCIES.json — composición y sumas de control de las dependencias.

Los adaptadores de servidor antiguos se compilan por separado con los SDK correspondientes.

Los SDK de servidor no se incluyen en el plugin resultante. El JAR original de Matrix no se utiliza como dependencia de compilación. Se ha conservado la configuración offline coherente. La compilación se realizó correctamente; no se garantiza la equivalencia completa en todos los escenarios.
