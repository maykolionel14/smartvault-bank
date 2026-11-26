# Despliegue Vercel / Neon de la demostración

Se conserva el formato de configuración de servicios Vercel del proyecto original. Se sustituyó el cuarto servicio por Reports y se retiró el daemon. Esta configuración se revisó estáticamente; no se realizó un despliegue ni se comprobaron cuotas, disponibilidad de funciones o precios actuales. No se autoriza ningún cargo ni se asume que el plan gratuito admite todos los recursos.

## Servicios

`identity`, `ledger`, `vaults`, `reports` y `web` (frontend). Reports usa bindings a Identity y Ledger. No usa PostgreSQL directamente. Los Dockerfiles individuales compilan Java 21 con Maven y exponen puerto 80 en el entorno Vercel.

## Variables de entorno privadas

| Variable | Uso |
|---|---|
| DATABASE_URL | Neon, para Identity/Ledger/Vaults; el adaptador original valida TLS y host Neon |
| DB_URL / DB_USER / DB_PASSWORD | Alternativa JDBC explícita |
| JWT_SECRET | Firma HMAC de sesiones; 32 caracteres o más |
| SERVICE_KEY | Clave distinta, común a los servicios internos; 32 caracteres o más |
| RESTRICT_REGISTRATION | true |
| DEMO_ALLOWED_EMAILS | Correos autorizados separados por coma |
| DB_POOL_SIZE | Opcional, valor inicial 3 |

Los bindings gestionan `IDENTITY_URL` y `LEDGER_URL`. Las credenciales no deben pasar al frontend. Retira variables antiguas del entorno si ya no se usan, sin borrar las claves financieras. Restringe los participantes y no expongas datos reales: el registro demo no verifica la titularidad del correo.

Antes de publicar, ejecuta las pruebas locales, revisa el presupuesto y disponibilidad de servicios en la cuenta, confirma que las rutas internas estén inaccesibles desde el gateway y usa una base de pruebas. Si se reutiliza una base anterior, respáldala antes de desplegar; no se borran tablas históricas. El flujo de despliegue no requiere un proveedor de correo ni un worker permanente.
