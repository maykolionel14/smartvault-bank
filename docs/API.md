# API de la demostración

Las rutas públicas usan JSON y el prefijo `/api/v1`. Las tablas indican el método y el resto de la ruta. La columna Acceso distingue libre, activa (Bearer firmado con vigencia de 20 minutos) e interna (`X-Service-Key` por un binding privado). Los importes se expresan en centavos COP enteros. Los historiales se consultan por titular. Ninguna ruta permite modificar o borrar asientos ya confirmados.

## Identity • /identity

| Método y ruta | Entrada | Salida y regla | Acceso |
|---|---|---|---|
| POST /register | email, password | 201 con user_id, email y mensaje. Crea cuenta de demostración activa; no verifica titularidad del correo | Libre, lista autorizada |
| POST /login | email, password | access_token, token_type, user_id, activated. Las cuentas anteriores pendientes siguen bloqueadas | Libre |
| GET /me | — | id, email, suspended, activated | Activa |
| POST /change-password | previous_password, new_password | Mensaje. Cambia el hash BCrypt e invalida sesiones anteriores | Activa |
| POST /freeze | — | suspended. Suspende las operaciones | Activa |
| POST /unfreeze | — | suspended y mensaje. Requiere sesión de versión vigente del titular suspendido | Titular suspendido |
| POST /revoke-sessions | — | Mensaje. Incrementa la versión de sesión | Activa |
| POST /evaluate-action | — | allowed si el titular puede operar | Activa |

## Ledger • /ledger

| Método y ruta | Entrada | Salida y regla | Acceso |
|---|---|---|---|
| POST /demo-credit | cents, idempotency_key | transfer_id, simulated_cents, kind. Abona fondos simulados en una transacción | Activa |
| GET /balance | — | wallet_id, balance_cents del titular; cero para billetera nueva | Activa |
| GET /statement | — | wallet_id, entries con referencia, importe firmado y fecha; últimos 100 asientos | Activa |
| GET /verify-integrity | — | valid, transfers, anomalous_transfer_ids. Contrasta transferencias, pares de asientos y saldos | Activa |
| GET /fx/quote | query base, quote, amount | Monedas, tasa, importe convertido y proveedor. Consulta informativa sin asiento | Activa |
| GET /calendar/next-business-day | query country, on (AAAA-MM-DD) | after, next_business_day, country, provider. Omite fin de semana y festivos nacionales | Activa |

## Vaults • /vaults

| Método y ruta | Entrada | Salida y regla | Acceso |
|---|---|---|---|
| POST / | name, member_ids, quorum, milestones (títulos) | 201 con vault_id, quorum y milestones; creador incluido y miembros existentes | Activa |
| GET / | — | Bóvedas del solicitante | Activa |
| GET /{id} | id de bóveda | Identidad, miembros, saldo, hitos y propuestas | Miembro activo |
| POST /{id}/contribute | cents, idempotency_key | transfer_id, amount_cents. Requiere saldo; el reintento no vuelve a transferir | Miembro activo |
| POST /{id}/proposals | milestone_id, beneficiary_id, amount_cents, purpose | 201 con estado de la propuesta. Requiere hito de la bóveda y beneficiario existente | Miembro activo |
| POST /milestones/{id}/attest | note | attestations, quorum, ready. Una validación por miembro y por hito | Miembro activo |
| POST /proposals/{id}/approve | — | Estado y recuentos. Un voto por miembro | Miembro activo |
| POST /proposals/{id}/reject | — | Estado y recuentos tras el voto negativo | Miembro activo |
| GET /proposals/{id}/status | id de propuesta | Votos, validaciones, quórum y releasable | Miembro activo |
| POST /proposals/{id}/execute | — | status, transfer_id, idempotent. Requiere votos y atestaciones suficientes; referencia única por propuesta | Miembro activo |
| GET /{id}/verify-audit | id de bóveda | valid, event_count, anomalous_sequences, note. Recalcula la cadena de hashes | Miembro activo |

## Reports • /reports

Todas las operaciones requieren sesión activa. Se calculan sobre los últimos 100 movimientos confirmados de la billetera personal, ordenados por fecha descendente y referencia. Los aportes son egresos y no se suman otra vez al neto. No representan la propiedad ni el saldo actual de una bóveda colectiva.

| Método y ruta | Salida |
|---|---|
| GET /summary | Totales `income_cents`, `expense_cents`, `contribution_cents`, `net_cents`, `movement_count`, `limit`, `currency`, `movements` |
| GET /movements | Lista de transferencias propias con `transfer_id`, `signed_cents`, `kind`, `created_at` |
| GET /income | Total positivo y movimientos de ingreso |
| GET /expenses | Total absoluto y movimientos de egreso |
| GET /contributions | Total de aportes a bóvedas y movimientos asociados |
| GET /export | CSV UTF-8 con detalle de movimientos, solo titular |

Un reporte sin movimientos vale cero; si Ledger o Identity no están disponibles se devuelve 503, no un saldo falso.

## Contratos entre servicios

Estas rutas usan el prefijo `/internal` sin `/api/v1`. El proxy público sirve únicamente las rutas de negocio y los archivos del frontend. Todas las filas siguientes requieren clave de servicio.

| Método y ruta | Entrada | Salida y regla |
|---|---|---|
| POST /identity/verify | token | active si cuenta y versión habilitan operaciones |
| POST /identity/known-users | arreglo de IDs | Mapa de ID a correo para cuentas activadas existentes |
| POST /ledger/open-vault | vault_id | wallet_id. Abre una billetera de bóveda idempotentemente |
| GET /ledger/vault-balance/{id} | id | balance_cents |
| POST /ledger/contribute | actor_id, vault_id, cents, idempotency_key | transfer_id, amount_cents. Asientos opuestos |
| GET /ledger/report-movements/{userId} | ID obtenido por Reports del token validado | Últimas 100 transferencias del titular, solo comunicación interna |
| POST /ledger/payout | vault_id, beneficiary_id, proposal_id, cents | transfer_id. La misma propuesta identifica un solo desembolso |


Registro y acceso admiten diez solicitudes por dirección normalizada y operación en cada ventana de un minuto, con un tope global de sesenta por operación. El exceso recibe HTTP 429. Los fallos de los proveedores de referencia reciben HTTP 503.

Cada servicio tiene `GET /health` propio. Nginx deniega rutas `/internal/` desde el cliente.
