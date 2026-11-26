# Estrategia de pruebas — Arrange, Act, Assert

**Arrange:** preparar dependencias, datos y resultado esperado. **Act:** ejecutar la función o escenario bajo prueba. **Assert:** comprobar el comportamiento. En excepciones, `assertThrows` combina ejecución y comprobación de la excepción, y por eso se marca Act / Assert. Mockito simula las interfaces; no simula la regla de negocio que se prueba.

## Pruebas unitarias

| Módulo | Clases y escenarios principales |
|---|---|
| Identity | `IdentityUseCasesTest`, `IdentityMockTest`, `RegistrationAaaTest`, `UserAccountTest`: registro activo, contraseña delegada a hash, duplicados, inicio de sesión, suspensión, revocación, cuenta anterior pendiente |
| Ledger | `LedgerUseCasesTest`, `LedgerMockTest`, `LedgerAaaTest`, `LedgerRulesTest`, `ReferenceUseCasesTest`, `LedgerReportReaderTest`: montos, idempotencia, integridad, saldo insuficiente, cotización, fechas hábiles, filtros de reporte y clave interna |
| Vaults | `VaultUseCasesTest`, `VaultsMockTest`, `VaultAaaTest`, `VaultDomainTest`: acceso, quórum, voto, hitos, aportes, repetición de desembolsos, auditoría |
| Reports | `FinancialReportTest`, `ReportUseCasesTest`, `ReportsControllerTest`, `ReportCsvWriterTest`: totales, filtro, duplicados, límites, autenticación, recuperación, CSV |
| Platform | `ApiErrorsTest`, `NeonDatabaseInitializerTest`: tokens, expiración, clave interna y configuración |
| Verification | `SystemVerificationTest`: las capas internas no importan Spring/JPA ni infraestructura |

Los nuevos casos usan comentarios AAA explícitos; los casos originales de regresión se conservaron y ajustaron solo cuando su contrato cambió. JaCoCo mide cobertura de código real al ejecutar Maven. No se fija un porcentaje obtenido de la arquitectura anterior.

## Pruebas del sistema (Docker aislado)

| Escenario | Arrange | Act | Assert / criterio |
|---|---|---|---|
| Integración | Dos cuentas y una base nueva | Registro, login, crédito, aporte, votos, hitos, desembolso, reportes | Saldos exactos, idempotencia, quórum, integridad, aislamiento de titulares, rechazo HTTP no autorizado |
| Carga | 10 trabajadores, 100 lecturas por endpoint, cuatro endpoints, calentamiento | Solicitudes concurrentes | 0 errores y p95 <= 500 ms (ajustable con `--p95-ms`) |
| Estrés | Reportes autenticados; 20, 40 y 80 trabajadores; 100, 200 y 400 solicitudes | Escalamiento por etapas | Se registra p95, p99, máximo, media, rendimiento y error%; criterio <= 5% errores por etapa |
| Recuperación tras estrés | Carga reducida | 10 trabajadores, 100 lecturas | 0 errores, p95 dentro del objetivo |
| Recuperación de servicio | Reporte conocido | Detener Ledger, observar 503, reiniciar | RTO observado, mismo reporte, 16 reintentos concurrentes sin duplicar crédito, integridad válida |
| Restauración | Copia PostgreSQL en formato custom | Restaurar en BD nueva dentro del proyecto QA | Igual conteo y huella de todas las filas de cinco tablas de negocio; sin sobrescribir origen |

El runner permite solo HTTP local y crea un proyecto Docker desechable. Las pruebas de recuperación no se hacen en una base real. No se someten los proveedores externos de cambio de moneda o festivos a carga. La recuperación de base verifica contenido restaurado, no un failover distribuido completo.

**Métricas:** p95 es el percentil 95; p99, el 99; máximo es la peor latencia; rendimiento = solicitudes/segundo; error% = fallos/total. El p95 de 500 ms es una meta de aceptación propuesta, no una promesa ni una medición ya obtenida. El CSV mantiene también las solicitudes lentas y fallidas. La latencia se mide de extremo a extremo desde el cliente local.

## Reproducción

`mvn clean verify` ejecuta JUnit y produce informes JaCoCo en cada módulo de negocio. `python verification/run_qa.py` ejecuta las pruebas reales e imprime resultados; `--skip-performance` permite ejecutar integración y recuperación únicamente. Los archivos de resultados se crean durante la ejecución: `performance.csv` y `recovery.json`. No se incluye información privada en esos archivos.
