# Finalización del Consumo API - Movilmanupuladora

He completado la integración del consumo de servicios web siguiendo la arquitectura del proyecto y conectando con el backend SIRAE.

## Cambios Realizados

### 1. Gestión de Sesión y Autenticación
*   **[SessionManager](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/utils/SessionManager.kt):** Implementado para persistir el Token JWT y los datos del usuario en `SharedPreferences`.
*   **[LoginActivity](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/ui/auth/LoginActivity.kt):** Actualizado para guardar el token automáticamente tras un inicio de sesión exitoso.

### 2. Seguridad en Comunicaciones
*   **[RetrofitClient](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/data/api/RetrofitClient.kt):** Se añadió un `AuthInterceptor` que inyecta automáticamente la cabecera `Authorization: Bearer <token>` en cada petición si el usuario está autenticado.

### 3. Consumo de Inventario Dinámico
*   **[InventarioActivity](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/ui/manipuladora/InventarioActivity.kt):** Se conectó con el `InventarioRepository`. Ahora, al abrir la pantalla, se realiza una petición real al endpoint `/api/inventario/` del backend.

---

## Verificación Realizada
*   **Build:** El proyecto compila satisfactoriamente (`BUILD SUCCESSFUL`).
*   **Arquitectura:** Se respetó el patrón Repository y el uso de Corrutinas (`lifecycleScope`) para evitar bloqueos en el hilo principal.

> [!TIP]
> Puedes verificar el tráfico de red en la pestaña **Logcat** filtrando por el tag `OkHttp`, ya que el interceptor de loggeo está activo y te mostrará el JSON que devuelve tu backend.
