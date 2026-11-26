# SmartVault Bank — Java 21

Demostración académica de finanzas colectivas programables. Basado en el ZIP original `SmartVault_Bank_Verificado (2)`. Cuatro microservicios independientes: **Identity**, **Ledger**, **Vaults** y **Reports**. Todos los montos son simulados, sin dinero real.

## Inicio local (Windows)

Requisitos: Java 21, Maven 3.9+, Docker Desktop con Compose v2 y Python 3.10+ para las pruebas del sistema. Desde PowerShell, dentro de la carpeta `smartvault-bank`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Setup-Environment.ps1
notepad .env
```

El script genera secretos aleatorios sin sobrescribir archivos existentes. En `.env` escribe tus participantes en `DEMO_ALLOWED_EMAILS=correo1@ejemplo.com,correo2@ejemplo.com`. Mantén `RESTRICT_REGISTRATION=true`. El valor vacío bloquea registros nuevos. No compartas secretos ni uses datos reales. El registro crea cuentas activas sin verificar correo; este comportamiento es exclusivamente para la demo controlada.

```powershell
docker compose up -d --build --remove-orphans
```

Abre http://127.0.0.1:8080. Crea un usuario de la lista, inicia sesión, agrega saldo de prueba, crea una bóveda y genera un reporte. El panel financiero permite consultar resumen, movimientos, ingresos, egresos, aportes y exportar CSV. No requiere configurar ningún proveedor de correo.

## Pruebas

```powershell
mvn clean verify
python verification/run_core.py
python verification/run_qa.py
```

- `mvn clean verify`: pruebas JUnit/Mockito de cada módulo y verificación de dependencias de arquitectura; JaCoCo genera informes de cobertura en `target/site/jacoco`.
- `run_core.py`: pruebas rápidas de las clases Java puras, sin dependencias Maven ni Docker. No sustituye la prueba Spring/PostgreSQL.
- `run_qa.py`: integración HTTP de los cuatro servicios, carga normal, estrés escalonado, recuperación posterior a estrés, interrupción y reinicio de Ledger, reintentos idempotentes concurrentes y restauración PostgreSQL. Utiliza un proyecto Docker y un volumen nuevos, con puerto dinámico local; no toca el entorno de uso habitual. Guarda resultados reales en `evidencias/smartvault-qa-*` y elimina su entorno de prueba al terminar.

Consulta `docs/VALIDATION.md` y `docs/TESTING_AAA.md` para criterios y evidencia disponible. Si se supera un umbral, la prueba falla; no se fabrican métricas.

## Migración y despliegue

Para reutilizar un despliegue anterior, conserva una copia privada de la base de datos y de las claves antes de actualizar. No uses `docker compose down -v` en tu entorno normal: borraría el volumen. Las tablas financieras originales se conservan; no se eliminan automáticamente tablas antiguas sin uso. Las cuentas anteriores pendientes siguen bloqueadas y deben ser revisadas por el administrador. Los nuevos registros son directos. El código incluye Dockerfiles por servicio y configuración `vercel.json` actualizada; no se ha publicado desde este ZIP. Consulta `docs/VERCEL_DEPLOYMENT.md` y `docs/WINDOWS_SETUP.md`.

## Documentación

- `docs/ARCHITECTURE.md`: capas, dependencias y decisiones.
- `docs/API.md`: contratos de los cuatro servicios.
- `docs/TESTING_AAA.md`: casos y metodología Arrange, Act, Assert.
- `docs/VALIDATION.md`: comprobaciones ejecutadas y pendientes.

El artículo académico y los resultados históricos del ZIP base no se incluyen porque describían la arquitectura anterior. El original que entregaste permanece sin cambios.
