# Instalación en Windows 11

1. Verifica `java -version` (21), `mvn -v`, `docker compose version` y `python --version` (3.10 o superior). Docker Desktop debe estar iniciado.
2. Extrae el ZIP en una carpeta nueva. Abre PowerShell en `smartvault-bank`, junto a `pom.xml`.
3. Ejecuta `powershell -ExecutionPolicy Bypass -File .\scripts\Setup-Environment.ps1`. El archivo `.env` debe quedar en la raíz.
4. Edita `DEMO_ALLOWED_EMAILS` con los correos de prueba que autorizas. Mantén el registro restringido; no verifica el dominio ni titularidad del correo.
5. Ejecuta `docker compose up -d --build --remove-orphans` y revisa `docker compose ps` y `docker compose logs --tail 80` si falla.
6. Visita `http://127.0.0.1:8080` y crea los usuarios de prueba. El registro ya no depende de correo. El nuevo panel está en Reportes financieros.
7. Para detener sin borrar datos usa `docker compose stop`. Para iniciar otra vez, `docker compose start`. No uses `down -v` en el entorno habitual.
8. Para pruebas aisladas: `mvn clean verify`, `python verification/run_core.py` y `python verification/run_qa.py`. Los resultados del runner se guardan en `evidencias/`.

Si ya existe `.env` de una versión anterior, no se sobrescribe; elimina las variables antiguas de correo y conserva las claves que utiliza la base de datos. Evita cambiar la clave de servicio o la de sesiones durante una prueba. Si un puerto está ocupado, establece `WEB_PORT=8085` en `.env` y usa ese puerto en el navegador.
