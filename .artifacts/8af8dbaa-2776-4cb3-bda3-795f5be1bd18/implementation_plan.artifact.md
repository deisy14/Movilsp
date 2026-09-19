# Plan de Diagnóstico y Corrección de Navegación Post-Login

El usuario reporta que el inicio de sesión funciona (tanto en web como en la app), pero al intentar navegar entre módulos (acceder a otros endpoints), el sistema deniega el acceso por problemas de autenticación. Esto sugiere que el token no está siendo reconocido en las peticiones posteriores o que el usuario carece de los permisos necesarios.

## Análisis del Problema

1.  **Prefix del Token:** Actualmente la app envía `Authorization: Token <token>`. Muchos backends modernos (especialmente los que usan SimpleJWT) esperan `Authorization: Bearer <token>`.
2.  **Persistencia del Token:** Si la app se reinicia, `RetrofitClient.authToken` (que es una variable en memoria) se pierde, aunque el usuario siga "logeado" en las preferencias.
3.  **Problema en el Backend (Probable):** El usuario menciona que en la versión WEB también falla la navegación tras el login. Esto indica que el problema podría estar en los **Permisos de Django** (ej: `IsAuthenticated` vs `IsStaff` o roles personalizados) o en la expiración inmediata del token.

---

## Cambios Propuestos

### Componente: Red (RetrofitClient)
Estandarizar el envío del token y mejorar la persistencia.

#### [MODIFY] [RetrofitClient.kt](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/data/api/RetrofitClient.kt)
*   Cambiar el prefijo de `Token` a `Bearer`.
*   Añadir un método para inicializar el `authToken` desde el `SessionManager` al abrir la app.

### Componente: Interfaz de Usuario (Main/Login)
Asegurar que el token esté disponible siempre.

#### [MODIFY] [MainActivity.kt](file:///C:/Movilsp/app/src/main/java/com/example/movilmanupuladora/MainActivity.kt)
*   En el `onCreate`, cargar el token desde `SessionManager` y asignarlo a `RetrofitClient.authToken`.

---

## Plan de Verificación

### Pruebas Manuales
1.  **Verificar Logs:** Observar si el error al navegar es `401 Unauthorized` (Token inválido/mal formado) o `403 Forbidden` (Token válido pero sin permisos).
2.  **Prueba con Bearer:** Intentar la navegación tras el cambio a `Bearer`.

## Preguntas y Recomendaciones para el Backend
> [!IMPORTANT]
> Si el problema persiste en la WEB, es vital revisar lo siguiente en el servidor:
> 1. **Roles:** ¿El usuario tiene asignado el rol correcto en la tabla de la base de datos?
> 2. **CORS:** Si es web, verificar que el dominio esté permitido.
> 3. **Permissions:** En las Views de Django, verificar si se está exigiendo `is_staff=True` y el usuario no lo tiene.
