# Reporte Completo de Flujos de Operación — Movilmanupuladora (PAE / SIRAE)

Este informe detalla el recorrido operativo de principio a fin para los dos perfiles principales de la aplicación institucional: **Manipuladora de Alimentos (Operativa)** y **Manipuladora Jefa (Coordinación y Gestión)**.

---

## 👩‍🍳 Perfil 1: Manipuladora de Alimentos (Operativa / Cocina)

### 1. Autenticación y Acceso (`LoginActivity`)
- La manipuladora ingresa su correo y contraseña institucionales.
- La aplicación valida las credenciales contra el backend SIRAE mediante Retrofit (`auth/login/`).
- El sistema detecta su rol operativo y la dirige automáticamente al módulo de selección de turno.

### 2. Selección de Turno (`TurnoActivity`)
- Selecciona su jornada laboral activa para el día operativo (**Mañana** o **Tarde**).
- Al confirmar el turno, la app carga el panel principal operativo (`MainActivity`).

### 3. Panel Principal Operativo (`MainActivity`)
- **Encabezado Institucional:** Muestra el saludo personalizado, su rol, la **institución y sede** asignada en tiempo real por el superadmin, y la fecha actual.
- **Selector de Jornada y Carrusel:** Permite alternar visualmente entre turnos y consultar los platos y secciones asignadas para el día.

### 4. Consulta de Menú e Ingredientes (`MenuDiaActivity` & `IngredientesActivity`)
- Visualiza el menú segmentado por tiempo de comida (*Desayuno, Almuerzo y Merienda*).
- Consulta el listado detallado de ingredientes, marcas institucionales y pautas de sazón requeridas para la preparación.

### 5. Proceso de Cocina y Preparación (`PreparacionActivity`)
- Sigue la guía paso a paso de preparación para el plato asignado.
- Actualiza el estado de su componente en tiempo real:
  - `"Por hacer"` ➔ `"En proceso"` ➔ `"Finalizada"`.
- Al marcar el plato como **Finalizado**, la app abre la pantalla de confirmación exitosa (`ConfirmacionActivity`), lo que procesa automáticamente el descuento de insumos del inventario general de la sede.

### 6. Inventario y Avisos (`InventarioActivity` & `AsignadasActivity`)
- Consulta el inventario de alimentos en modo estricto de solo lectura.
- Revisa las tareas asignadas por la Jefa de Cocina y lee avisos operativos.

### 7. Perfil y Salida (`PerfilActivity`)
- Permite consultar los datos del perfil, actualizar el teléfono de contacto y cerrar sesión de forma segura (`sessionManager.clearSession()`), limpiando credenciales y dejando el dispositivo listo para otro usuario.

---

## 👩‍💼 Perfil 2: Manipuladora Jefa / Coordinadora (Gestión y Planificación)

### 1. Autenticación y Acceso (`LoginActivity`)
- La Jefa ingresa sus credenciales institucionales.
- El sistema valida el rol de jefatura y la enruta directamente al panel gerencial (`InicioJefaActivity`).

### 2. Dashboard de Coordinación (`InicioJefaActivity`)
- **Encabezado:** Muestra el saludo, el cargo (*"Coordinación y Gestión PAE"*), **la institución y sede asignada en tiempo real**, notificaciones y acceso directo al perfil.
- **Mi semana:** Tira horizontal de 7 días navegables (Lunes a Domingo) para consultar la programación.
- **Resumen del Día:** Métricas en tiempo real sobre raciones totales, menús activos y personal de cocina a cargo.

### 3. Registro y Control de Asistencia (`AsistenciaJefaActivity`)
- Ingresa y corrige la cantidad de estudiantes presentes organizados por grados (de Jardín a Undécimo) y turnos (Mañana / Tarde).
- El sistema procesa la asistencia y calcula automáticamente los gramajes y porciones requeridas según el ciclo escolar.

### 4. Planificación Semanal de Menús (`PlanificacionSemanalJefaActivity` & `CalendarioMenuActivity`)
- Selecciona semanas y días operativos.
- Consulta sugerencias generadas por el motor de inteligencia artificial (`AiRecetaActivity`) o elige platos del catálogo oficial de los 20 menús del ciclo PAE.
- Asigna los platos a Desayuno, Almuerzo y Merienda aplicando validaciones normativas (como la rotación para no repetir menús consecutivos) y guarda la programación en la base de datos.

### 5. Asignación de Tareas al Personal (`ManipuladorasAsignadasActivity` & `AsignarComponentesActivity`)
- Visualiza el listado de manipuladoras activas de la sede.
- Asigna a cada cocinera el componente específico que preparará (proteína, carbohidrato, etc.), con opción de autoasignación para la Jefa y validación estricta para evitar tareas duplicadas.

### 6. Inventario y Reportes (`InventarioJefaActivity` & `RedactarAvisoJefaActivity`)
- Consulta el inventario general de la sede en modo solo lectura para verificar la disponibilidad de insumos.
- Redacta y envía avisos, alertas operativas o informes directamente al administrador del sistema.

### 7. Perfil de Coordinación (`PerfilJefaActivity`)
- Administra su información institucional y cierra sesión de manera segura.
