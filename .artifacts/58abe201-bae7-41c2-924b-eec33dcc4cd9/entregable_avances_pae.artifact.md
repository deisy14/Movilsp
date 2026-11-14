# 📱 Reporte Oficial de Avances — Aplicación Móvil Movilmanupuladora (PAE - SIRAE)

**Fecha de Entrega:** Noviembre 2026
**Proyecto:** Sistema de Información y Registro de Alimentación Escolar (SIRAE)
**Módulo Móvil:** Movilmanupuladora (Android Native - Kotlin)

---

## 📌 1. Resumen Ejecutivo
Se presenta el reporte de avances del desarrollo de la aplicación móvil **Movilmanupuladora**, diseñada para coordinar y supervisar la ejecución diaria del **Programa de Alimentación Escolar (PAE)**. La solución está completamente integrada mediante arquitectura REST API (Retrofit / JSON) con el backend de SIRAE en Render, separando de forma estricta los flujos operativos de la **Manipuladora de Alimentos** y la **Jefa de Cocina / Coordinadora**.

---

## 🚀 2. Principales Módulos y Funcionalidades Desarrolladas

### A. Autenticación y Gestión de Sedes Institucionales
- **Autenticación JWT:** Inicio de sesión seguro desde el dispositivo móvil contra el servidor backend.
- **Detección Automática de Sede:** Carga e identificación en tiempo real del nombre de la **Institución / Sede escolar** asignada a la usuaria por el Superadministrador.
- **Enrutamiento por Rol:** Separación de interfaces para evitar cruce de responsabilidades entre el personal operativo y la jefatura.

---

### B. Módulo de Coordinación (Jefa de Cocina)
1. **Planificación Semanal de Menús:**
   - Selección de semanas y días operativos de Lunes a Domingo.
   - Asignación de platos oficiales del ciclo PAE (20 menús normativos) para **Desayuno, Almuerzo y Merienda**.
   - Validación automática de reglas de rotación (evita repetir menús en días consecutivos).
2. **Asignación de Componentes y Tareas:**
   - Carga dinámica en tiempo real de las manipuladoras activas desde la base de datos.
   - Asignación individualizada de componentes (*proteína, carbohidrato, bebida/acompañamiento*) por jornada.
   - Control estricto de exclusividad para evitar asignaciones duplicadas.
3. **Control de Asistencia Escolar y Gramajes:**
   - Registro y edición de asistencia por grados (Jardín a Undécimo) para turnos de Mañana y Tarde.
   - Cálculo automático de porciones e insumos según los gramajes oficiales del PAE (150g preescolar, 250g primaria, 380g secundaria).
4. **Control de Inventario e Informes:**
   - Consulta de stock de insumos por categorías (Granos, Lácteos, Proteínas).
   - Descuento automático de existencias al marcar preparaciones como *"Finalizadas"*.
   - Canal para redacción y envío de avisos e informes directos al Administrador.

---

### C. Módulo Operativo (Manipuladora de Alimentos)
1. **Control de Turno y Menú Diario:**
   - Registro de jornada de trabajo (*Mañana / Tarde*).
   - Visualización del menú del día segmentado por tiempo de comida y consulta de su componente asignado.
2. **Guía de Recetas e Ingredientes:**
   - Consulta de insumos, marcas registradas y pautas de sazón.
   - Guía de preparación paso a paso.
3. **Seguimiento de Estado de Cocina:**
   - Actualización del estado del plato en tiempo real (*"Por hacer" ➔ "En proceso" ➔ "Finalizada"*).
   - Pantalla de confirmación con registro de la hora exacta de finalización de la comida.

---

## 🎨 3. Calidad Visual y Optimización de Código
- **Diseño UI/UX:** Implementación de componentes Material Design 3 con la paleta de colores institucional del PAE (Crema `#FAF9F5`, Amarillo `#F4B41F`, Verde SIRAE `#1B3317`).
- **Logotipo Institucional:** Integración del logotipo oficial SIRAE en alta resolución con fondo transparente.
- **Calidad del Proyecto:**
  - Código estructurado y limpio en Kotlin.
  - Eliminación de archivos huérfanos e inactivos.
  - Compilación exitosa (`assembleDebug`) con cero errores en Gradle.

---

## 📋 4. Entregables Disponibles para Demostración
1. **APK ejecutable** compilado y listo para pruebas en dispositivos de campo.
2. **Manual de Flujos Operativos** (paso a paso de manipuladoras y jefas).
3. **Puntos de Integración API** conectados al entorno en la nube.
