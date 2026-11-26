# Validación de esta entrega

Consulta `evidencias/validacion_local.txt` para el resultado de las comprobaciones ejecutadas durante la preparación de este ZIP. Las evidencias de la versión anterior se retiraron para evitar atribuir sus resultados a Reports.

## Pendientes de ejecución

El entorno de preparación dispone de Java 21, Python y Node, pero no de Maven ni Docker; el repositorio Maven tampoco fue accesible desde el entorno. Por ello no se ha ejecutado la compilación Spring completa, JUnit/Mockito, JaCoCo, la integración PostgreSQL ni las pruebas reales de carga, estrés, recuperación y restauración. El código, scripts y criterios están incluidos para ejecutarlos con las herramientas requeridas. No se declaran porcentajes de cobertura, latencias ni resultados de recuperación que no se hayan medido.

La prueba de Java puro compila las clases de dominio/aplicación originales y nuevas y algunos componentes sin dependencias. No certifica los adaptadores Spring/JPA ni el comportamiento de PostgreSQL. Las comprobaciones de sintaxis y configuración tampoco equivalen a un despliegue funcional.
