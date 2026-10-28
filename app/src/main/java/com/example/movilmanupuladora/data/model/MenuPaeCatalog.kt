package com.example.movilmanupuladora.data.model

import com.example.movilmanupuladora.R
import java.util.Calendar

data class MenuPaeItem(
    val id: Int,
    val numeroMenu: Int,
    val nombre: String,
    val componente: String,
    val imagenRes: Int,
    val nombreDesayuno: String,
    val compDesayuno: String,
    val imgDesayuno: Int,
    val porcionesDesayuno: String,
    val nombreAlmuerzo: String,
    val compAlmuerzo: String,
    val imgAlmuerzo: Int,
    val porcionesAlmuerzo: String,
    val nombreMerienda: String,
    val compMerienda: String,
    val imgMerienda: Int,
    val porcionesMerienda: String
)

data class MenuDiaPae(
    val desayuno: MenuPaeItem,
    val almuerzo: MenuPaeItem,
    val merienda: MenuPaeItem
)

object MenuPaeCatalog {

    val menus = listOf(
        // ==========================================
        // SEMANA 1 (MENÚS 1 AL 5)
        // ==========================================
        MenuPaeItem(
            id = 1,
            numeroMenu = 1,
            nombre = "Huevos pericos con arroz blanco",
            componente = "Refresco de avena, huevos pericos, arroz blanco",
            imagenRes = R.drawable.plato1,
            nombreDesayuno = "Huevos pericos y avena",
            compDesayuno = "Huevos pericos con arroz blanco y refresco de avena",
            imgDesayuno = R.drawable.plato1,
            porcionesDesayuno = "42",
            nombreAlmuerzo = "Arroz con huevos pericos",
            compAlmuerzo = "Arroz blanco con huevos pericos",
            imgAlmuerzo = R.drawable.plato1,
            porcionesAlmuerzo = "56",
            nombreMerienda = "Refresco de avena",
            compMerienda = "Bebida láctea de avena fría",
            imgMerienda = R.drawable.plato1,
            porcionesMerienda = "26"
        ),
        MenuPaeItem(
            id = 2,
            numeroMenu = 2,
            nombre = "Pollo en salsa blanca con pasta",
            componente = "Jugo de fruta en leche, pollo en salsa blanca, pasta",
            imagenRes = R.drawable.plato2,
            nombreDesayuno = "Pasta corta con jugo en leche",
            compDesayuno = "Pasta corta con jugo de fruta en leche",
            imgDesayuno = R.drawable.plato2,
            porcionesDesayuno = "45",
            nombreAlmuerzo = "Pollo en salsa blanca y pasta",
            compAlmuerzo = "Pollo en trozos con salsa blanca y pasta",
            imgAlmuerzo = R.drawable.plato2,
            porcionesAlmuerzo = "60",
            nombreMerienda = "Jugo de fruta en leche",
            compMerienda = "Jugo natural preparado en leche",
            imgMerienda = R.drawable.plato2,
            porcionesMerienda = "30"
        ),
        MenuPaeItem(
            id = 3,
            numeroMenu = 3,
            nombre = "Chocolate en leche con arepa",
            componente = "Chocolate en leche, arepa de maiz, banano",
            imagenRes = R.drawable.plato3,
            nombreDesayuno = "Chocolate con arepa de maíz",
            compDesayuno = "Chocolate en leche caliente con arepa de maíz",
            imgDesayuno = R.drawable.plato3,
            porcionesDesayuno = "40",
            nombreAlmuerzo = "Arepa con proteína y banano",
            compAlmuerzo = "Arepa de maíz con queso y banano",
            imgAlmuerzo = R.drawable.plato3,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Banano fresco con chocolate",
            compMerienda = "Porción de banano maduro con chocolate",
            imgMerienda = R.drawable.plato3,
            porcionesMerienda = "28"
        ),
        MenuPaeItem(
            id = 4,
            numeroMenu = 4,
            nombre = "Lentejas con papa guisada y arroz",
            componente = "Jugo de fruta en leche, lentejas con papa guisada, arroz",
            imagenRes = R.drawable.plato4,
            nombreDesayuno = "Arroz con jugo en leche",
            compDesayuno = "Arroz blanco con jugo de fruta en leche",
            imgDesayuno = R.drawable.plato4,
            porcionesDesayuno = "44",
            nombreAlmuerzo = "Lentejas con papa guisada",
            compAlmuerzo = "Lentejas con papa guisada y arroz blanco",
            imgAlmuerzo = R.drawable.plato4,
            porcionesAlmuerzo = "62",
            nombreMerienda = "Jugo de fruta en leche",
            compMerienda = "Vaso de jugo de fruta en leche",
            imgMerienda = R.drawable.plato4,
            porcionesMerienda = "32"
        ),
        MenuPaeItem(
            id = 5,
            numeroMenu = 5,
            nombre = "Torta de huevo con arroz blanco",
            componente = "Agua de panela en leche, torta de huevo, arroz blanco",
            imagenRes = R.drawable.plato5,
            nombreDesayuno = "Agua de panela y torta de huevo",
            compDesayuno = "Agua de panela en leche y torta de huevo",
            imgDesayuno = R.drawable.plato5,
            porcionesDesayuno = "48",
            nombreAlmuerzo = "Torta de huevo con arroz",
            compAlmuerzo = "Torta de huevo horneada con arroz blanco",
            imgAlmuerzo = R.drawable.plato5,
            porcionesAlmuerzo = "65",
            nombreMerienda = "Agua de panela en leche",
            compMerienda = "Agua de panela caliente en leche",
            imgMerienda = R.drawable.plato5,
            porcionesMerienda = "35"
        ),

        // ==========================================
        // SEMANA 2 (MENÚS 6 AL 10)
        // ==========================================
        MenuPaeItem(
            id = 6,
            numeroMenu = 6,
            nombre = "Carne de res guisada con arroz",
            componente = "Refresco de avena, carne de res guisada, arroz blanco",
            imagenRes = R.drawable.plato6,
            nombreDesayuno = "Arroz con refresco de avena",
            compDesayuno = "Arroz blanco con refresco de avena",
            imgDesayuno = R.drawable.plato6,
            porcionesDesayuno = "42",
            nombreAlmuerzo = "Carne de res guisada con arroz",
            compAlmuerzo = "Carne de res guisada en trozos con arroz blanco",
            imgAlmuerzo = R.drawable.plato6,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Refresco de avena",
            compMerienda = "Vaso de refresco de avena fría",
            imgMerienda = R.drawable.plato6,
            porcionesMerienda = "26"
        ),
        MenuPaeItem(
            id = 7,
            numeroMenu = 7,
            nombre = "Chocolate en leche con hojaldras",
            componente = "Chocolate en leche, hojaldras, papaya, agua potable",
            imagenRes = R.drawable.plato7,
            nombreDesayuno = "Chocolate con hojaldras",
            compDesayuno = "Chocolate en leche caliente con hojaldras",
            imgDesayuno = R.drawable.plato7,
            porcionesDesayuno = "45",
            nombreAlmuerzo = "Menú balanceado con papaya",
            compAlmuerzo = "Menú escolar PAE con porción de papaya",
            imgAlmuerzo = R.drawable.plato7,
            porcionesAlmuerzo = "60",
            nombreMerienda = "Papaya picada con agua potable",
            compMerienda = "Papaya fresca picada en cubos",
            imgMerienda = R.drawable.plato7,
            porcionesMerienda = "30"
        ),
        MenuPaeItem(
            id = 8,
            numeroMenu = 8,
            nombre = "Frijoles guisados con papa y fideos",
            componente = "Agua de panela en leche, frijoles guisados con papa, arroz con fideos",
            imagenRes = R.drawable.plato8,
            nombreDesayuno = "Agua de panela en leche",
            compDesayuno = "Agua de panela caliente con leche",
            imgDesayuno = R.drawable.plato8,
            porcionesDesayuno = "40",
            nombreAlmuerzo = "Frijoles guisados con papa",
            compAlmuerzo = "Frijoles guisados con papa y arroz con fideos",
            imgAlmuerzo = R.drawable.plato8,
            porcionesAlmuerzo = "62",
            nombreMerienda = "Agua de panela en leche",
            compMerienda = "Vaso de agua de panela en leche",
            imgMerienda = R.drawable.plato8,
            porcionesMerienda = "28"
        ),
        MenuPaeItem(
            id = 9,
            numeroMenu = 9,
            nombre = "Leche con galleta cuca y mango",
            componente = "Leche, galleta / cuca, maní Salado, mango",
            imagenRes = R.drawable.plato9,
            nombreDesayuno = "Leche con galleta cuca",
            compDesayuno = "Vaso de leche entera con galleta cuca",
            imgDesayuno = R.drawable.plato9,
            porcionesDesayuno = "44",
            nombreAlmuerzo = "Menú escolar PAE",
            compAlmuerzo = "Plato fuerte PAE con acompañamiento",
            imgAlmuerzo = R.drawable.plato9,
            porcionesAlmuerzo = "56",
            nombreMerienda = "Mango dulce con maní salado",
            compMerienda = "Mango fresco tajado con maní salado",
            imgMerienda = R.drawable.plato9,
            porcionesMerienda = "32"
        ),
        MenuPaeItem(
            id = 10,
            numeroMenu = 10,
            nombre = "Huevos revueltos con arroz blanco",
            componente = "Jugo de frutas leche, huevos revueltos, Arroz blanco",
            imagenRes = R.drawable.plato10,
            nombreDesayuno = "Huevos revueltos y arroz",
            compDesayuno = "Huevos revueltos suaves con arroz blanco",
            imgDesayuno = R.drawable.plato10,
            porcionesDesayuno = "48",
            nombreAlmuerzo = "Arroz con huevos revueltos",
            compAlmuerzo = "Arroz blanco con huevos revueltos",
            imgAlmuerzo = R.drawable.plato10,
            porcionesAlmuerzo = "65",
            nombreMerienda = "Jugo de frutas en leche",
            compMerienda = "Vaso de jugo de frutas en leche",
            imgMerienda = R.drawable.plato10,
            porcionesMerienda = "35"
        ),

        // ==========================================
        // SEMANA 3 (MENÚS 11 AL 15)
        // ==========================================
        MenuPaeItem(
            id = 11,
            numeroMenu = 11,
            nombre = "Pollo guisado con arroz blanco",
            componente = "Refresco de avena, pollo guisado, arroz blanco",
            imagenRes = R.drawable.plato11,
            nombreDesayuno = "Arroz blanco con avena",
            compDesayuno = "Arroz blanco con refresco de avena",
            imgDesayuno = R.drawable.plato11,
            porcionesDesayuno = "42",
            nombreAlmuerzo = "Pollo guisado con arroz blanco",
            compAlmuerzo = "Pollo guisado en salsa con arroz blanco",
            imgAlmuerzo = R.drawable.plato11,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Refresco de avena",
            compMerienda = "Vaso de refresco de avena fría",
            imgMerienda = R.drawable.plato11,
            porcionesMerienda = "26"
        ),
        MenuPaeItem(
            id = 12,
            numeroMenu = 12,
            nombre = "Lentejas guisadas con papa y mango",
            componente = "Agua de panela en leche, lentejas guisadas con papa, arroz blanco, mango",
            imagenRes = R.drawable.plato4,
            nombreDesayuno = "Agua de panela con leche",
            compDesayuno = "Bebida caliente de agua de panela con leche",
            imgDesayuno = R.drawable.plato4,
            porcionesDesayuno = "40",
            nombreAlmuerzo = "Lentejas con papa y arroz",
            compAlmuerzo = "Lentejas guisadas con papa y arroz blanco",
            imgAlmuerzo = R.drawable.plato4,
            porcionesAlmuerzo = "60",
            nombreMerienda = "Mango dulce porcionado",
            compMerienda = "Porción de mango fresco picado",
            imgMerienda = R.drawable.plato4,
            porcionesMerienda = "30"
        ),
        MenuPaeItem(
            id = 13,
            numeroMenu = 13,
            nombre = "Torta de huevo con colada de quinua",
            componente = "Colada de quinua, torta de huevo, arroz blanco",
            imagenRes = R.drawable.plato5,
            nombreDesayuno = "Colada de quinua y torta",
            compDesayuno = "Colada de quinua caliente con torta de huevo",
            imgDesayuno = R.drawable.plato5,
            porcionesDesayuno = "44",
            nombreAlmuerzo = "Torta de huevo con arroz",
            compAlmuerzo = "Torta de huevo sazonada con arroz blanco",
            imgAlmuerzo = R.drawable.plato5,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Colada de quinua",
            compMerienda = "Vaso de colada de quinua nutritiva",
            imgMerienda = R.drawable.plato5,
            porcionesMerienda = "28"
        ),
        MenuPaeItem(
            id = 14,
            numeroMenu = 14,
            nombre = "Yogurt con torta pastel y manzana",
            componente = "Yogurt, Torta pastel, manzana",
            imagenRes = R.drawable.plato14,
            nombreDesayuno = "Torta pastel con yogurt",
            compDesayuno = "Torta pastel esponjosa con yogurt natural",
            imgDesayuno = R.drawable.plato14,
            porcionesDesayuno = "46",
            nombreAlmuerzo = "Menú balanceado escolar",
            compAlmuerzo = "Plato balanceado escolar PAE",
            imgAlmuerzo = R.drawable.plato14,
            porcionesAlmuerzo = "62",
            nombreMerienda = "Yogurt con manzana picada",
            compMerienda = "Yogurt con porción de manzana fresca",
            imgMerienda = R.drawable.plato14,
            porcionesMerienda = "32"
        ),
        MenuPaeItem(
            id = 15,
            numeroMenu = 15,
            nombre = "Arroz con carne de cerdo y verduras",
            componente = "Jugo de fruta en leche, arroz con carne de cerdo y verduras",
            imagenRes = R.drawable.plato15,
            nombreDesayuno = "Jugo de fruta en leche",
            compDesayuno = "Jugo de fruta natural preparado en leche",
            imgDesayuno = R.drawable.plato15,
            porcionesDesayuno = "48",
            nombreAlmuerzo = "Arroz con cerdo y verduras",
            compAlmuerzo = "Arroz salteado con carne de cerdo y verduras frescas",
            imgAlmuerzo = R.drawable.plato15,
            porcionesAlmuerzo = "65",
            nombreMerienda = "Jugo de fruta en leche",
            compMerienda = "Vaso de jugo de fruta en leche",
            imgMerienda = R.drawable.plato15,
            porcionesMerienda = "35"
        ),

        // ==========================================
        // SEMANA 4 (MENÚS 16 AL 20)
        // ==========================================
        MenuPaeItem(
            id = 16,
            numeroMenu = 16,
            nombre = "Lentejas guisadas con colada de quinua",
            componente = "Colada de quinua, lentejas guisadas con papa, arroz blanco",
            imagenRes = R.drawable.plato4,
            nombreDesayuno = "Colada de quinua caliente",
            compDesayuno = "Colada de quinua con leche",
            imgDesayuno = R.drawable.plato4,
            porcionesDesayuno = "42",
            nombreAlmuerzo = "Lentejas con papa guisada y arroz",
            compAlmuerzo = "Lentejas guisadas con papa y arroz blanco",
            imgAlmuerzo = R.drawable.plato4,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Colada de quinua",
            compMerienda = "Bebida de quinua caliente",
            imgMerienda = R.drawable.plato4,
            porcionesMerienda = "26"
        ),
        MenuPaeItem(
            id = 17,
            numeroMenu = 17,
            nombre = "Huevos pericos con chocolate en leche",
            componente = "Chocolate en leche, Huevos pericos, arroz blanco",
            imagenRes = R.drawable.plato1,
            nombreDesayuno = "Chocolate con huevos pericos",
            compDesayuno = "Chocolate en leche con huevos pericos y arroz",
            imgDesayuno = R.drawable.plato1,
            porcionesDesayuno = "45",
            nombreAlmuerzo = "Arroz blanco con huevos pericos",
            compAlmuerzo = "Arroz blanco con porción de huevos pericos",
            imgAlmuerzo = R.drawable.plato1,
            porcionesAlmuerzo = "60",
            nombreMerienda = "Chocolate en leche",
            compMerienda = "Vaso de chocolate caliente en leche",
            imgMerienda = R.drawable.plato1,
            porcionesMerienda = "30"
        ),
        MenuPaeItem(
            id = 18,
            numeroMenu = 18,
            nombre = "Leche con galleta cuca y mango",
            componente = "Leche, galleta cuca, maní salado, mango",
            imagenRes = R.drawable.plato9,
            nombreDesayuno = "Leche con galleta cuca",
            compDesayuno = "Vaso de leche entera con galleta cuca",
            imgDesayuno = R.drawable.plato9,
            porcionesDesayuno = "40",
            nombreAlmuerzo = "Menú escolar PAE",
            compAlmuerzo = "Plato escolar PAE con galleta cuca y mango",
            imgAlmuerzo = R.drawable.plato9,
            porcionesAlmuerzo = "58",
            nombreMerienda = "Galleta cuca con maní y mango",
            compMerienda = "Leche, galleta cuca, maní salado y mango",
            imgMerienda = R.drawable.plato9,
            porcionesMerienda = "28"
        ),
        MenuPaeItem(
            id = 19,
            numeroMenu = 19,
            nombre = "Pollo guisado con arroz blanco",
            componente = "Refresco de avena, pollo guisado, arroz blanco",
            imagenRes = R.drawable.plato11,
            nombreDesayuno = "Arroz blanco con avena",
            compDesayuno = "Arroz blanco con refresco de avena",
            imgDesayuno = R.drawable.plato11,
            porcionesDesayuno = "44",
            nombreAlmuerzo = "Pollo guisado con arroz blanco",
            compAlmuerzo = "Pollo guisado en salsa criolla con arroz blanco",
            imgAlmuerzo = R.drawable.plato11,
            porcionesAlmuerzo = "62",
            nombreMerienda = "Refresco de avena",
            compMerienda = "Vaso de refresco de avena fría",
            imgMerienda = R.drawable.plato11,
            porcionesMerienda = "32"
        ),
        MenuPaeItem(
            id = 20,
            numeroMenu = 20,
            nombre = "Frijoles guisados con arroz con fideos",
            componente = "Agua de panela en leche, frijoles guisados con papa, arroz con fideos",
            imagenRes = R.drawable.plato8,
            nombreDesayuno = "Agua de panela en leche",
            compDesayuno = "Agua de panela caliente con leche",
            imgDesayuno = R.drawable.plato8,
            porcionesDesayuno = "48",
            nombreAlmuerzo = "Frijoles con arroz y fideos",
            compAlmuerzo = "Frijoles guisados con papa y arroz con fideos",
            imgAlmuerzo = R.drawable.plato8,
            porcionesAlmuerzo = "65",
            nombreMerienda = "Agua de panela en leche",
            compMerienda = "Vaso de agua de panela en leche",
            imgMerienda = R.drawable.plato8,
            porcionesMerienda = "35"
        )
    )

    /**
     * Calcula dinámicamente el menú PAE correspondiente para cualquier fecha.
     * Ciclo continuo de 4 semanas del PAE:
     * Semana 1 -> Menús 1 a 5 (Lunes a Viernes)
     * Semana 2 -> Menús 6 a 10
     * Semana 3 -> Menús 11 a 15
     * Semana 4 -> Menús 16 a 20
     */
    fun obtenerMenuPorFecha(cal: Calendar): MenuPaeItem {
        val calAjustado = cal.clone() as Calendar
        calAjustado.firstDayOfWeek = Calendar.MONDAY

        // Usamos la semana del año para garantizar un ciclo secuencial de 4 semanas
        val semanaDelAnio = calAjustado.get(Calendar.WEEK_OF_YEAR)
        val indiceSemana = Math.abs(semanaDelAnio - 1) % 4

        val diaSemana = calAjustado.get(Calendar.DAY_OF_WEEK)
        val diaIndice = when (diaSemana) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> 0
        }

        val menuIndex = (indiceSemana * 5 + diaIndice) % menus.size
        return menus[menuIndex]
    }

    fun obtenerMenusDeSemana(cal: Calendar): List<MenuPaeItem> {
        val calAjustado = cal.clone() as Calendar
        calAjustado.firstDayOfWeek = Calendar.MONDAY

        val semanaDelAnio = calAjustado.get(Calendar.WEEK_OF_YEAR)
        val indiceSemana = Math.abs(semanaDelAnio - 1) % 4

        val startIndex = indiceSemana * 5
        return menus.subList(startIndex, (startIndex + 5).coerceAtMost(menus.size))
    }

    fun obtenerMenuPorId(id: Int?): MenuPaeItem {
        return menus.firstOrNull { it.id == id || it.numeroMenu == id } ?: menus[0]
    }

    fun obtenerImagenPorNombre(nombre: String?): Int {
        if (nombre.isNullOrBlank()) return R.drawable.plato1
        val n = nombre.lowercase().trim()

        // 1. Coincidencia directa con algún menú del catálogo PAE
        val menuMatch = menus.firstOrNull {
            it.nombre.lowercase().contains(n) ||
            n.contains(it.nombre.lowercase()) ||
            it.componente.lowercase().contains(n) ||
            n.contains(it.componente.lowercase())
        }
        if (menuMatch != null) {
            return menuMatch.imagenRes
        }

        // 2. Coincidencia por componentes clave de las recetas PAE
        return when {
            n.contains("pericos") || n.contains("huevos pericos") -> R.drawable.plato1
            n.contains("salsa blanca") || n.contains("pasta") -> R.drawable.plato2
            n.contains("arepa") || n.contains("banano") -> R.drawable.plato3
            n.contains("lentejas") || n.contains("lenteja") -> R.drawable.plato4
            n.contains("torta de huevo") -> R.drawable.plato5
            n.contains("carne de res") || n.contains("res guisada") -> R.drawable.plato6
            n.contains("hojaldras") || n.contains("papaya") -> R.drawable.plato7
            n.contains("frijol") || n.contains("frijoles") || n.contains("fideos") -> R.drawable.plato8
            n.contains("cuca") || n.contains("galleta") || n.contains("maní") || n.contains("mango") || n.contains("arroz con leche") -> R.drawable.plato9
            n.contains("huevos revueltos") || n.contains("revueltos") -> R.drawable.plato10
            n.contains("pollo guisado") || n.contains("julianas") -> R.drawable.plato11
            n.contains("torta pastel") || n.contains("yogurt") || n.contains("manzana") -> R.drawable.plato14
            n.contains("cerdo") -> R.drawable.plato15
            n.contains("pollo") -> R.drawable.plato11
            n.contains("chocolate") -> R.drawable.plato3
            n.contains("panela") -> R.drawable.plato8
            n.contains("avena") -> R.drawable.plato1
            else -> R.drawable.plato1
        }
    }

    fun obtenerComponentesPorNombre(nombre: String?): List<String> {
        if (nombre.isNullOrBlank()) {
            return listOf("Refresco de avena", "Huevos pericos", "Arroz blanco")
        }
        val n = nombre.lowercase().trim()
        val match = menus.firstOrNull {
            it.nombre.lowercase().contains(n) ||
            n.contains(it.nombre.lowercase()) ||
            it.componente.lowercase().contains(n) ||
            it.nombreDesayuno.lowercase().contains(n) ||
            it.nombreAlmuerzo.lowercase().contains(n) ||
            it.nombreMerienda.lowercase().contains(n)
        }

        if (match != null) {
            return match.componente.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

        if (nombre.contains(",")) {
            return nombre.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

        return listOf(nombre, "Acompañamiento PAE", "Bebida")
    }

    /**
     * Entrega el menú completo del día (Desayuno, Almuerzo y Merienda)
     * asegurando que cada servicio tenga su plato real correspondiente del PAE,
     * con su nombre exacto, imagen real y componentes correctos.
     */
    fun obtenerMenuDiaCompleto(cal: Calendar): MenuDiaPae {
        val calAjustado = cal.clone() as Calendar
        calAjustado.firstDayOfWeek = Calendar.MONDAY

        val semanaDelAnio = calAjustado.get(Calendar.WEEK_OF_YEAR)
        val indiceSemana = Math.abs(semanaDelAnio - 1) % 4

        val diaSemana = calAjustado.get(Calendar.DAY_OF_WEEK)
        val diaIndice = when (diaSemana) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> 0
        }

        return when (indiceSemana) {
            0 -> when (diaIndice) {
                0 -> MenuDiaPae(menus[0], menus[1], menus[2]) // Lun: Pericos (1), Pollo salsa blanca (2), Chocolate arepa (3)
                1 -> MenuDiaPae(menus[2], menus[3], menus[0]) // Mar: Chocolate arepa (3), Lentejas papa (4), Pericos (1)
                2 -> MenuDiaPae(menus[0], menus[4], menus[2]) // Mié: Pericos (1), Torta huevo (5), Chocolate arepa (3)
                3 -> MenuDiaPae(menus[2], menus[1], menus[0]) // Jue: Chocolate arepa (3), Pollo salsa blanca (2), Pericos (1)
                else -> MenuDiaPae(menus[0], menus[3], menus[4]) // Vie: Pericos (1), Lentejas papa (4), Torta huevo (5)
            }
            1 -> when (diaIndice) {
                0 -> MenuDiaPae(menus[6], menus[5], menus[8]) // Lun: Chocolate hojaldras (7), Carne res (6), Cuca mango (9)
                1 -> MenuDiaPae(menus[9], menus[7], menus[8]) // Mar: Huevos revueltos (10), Frijoles papa (8), Cuca mango (9)
                2 -> MenuDiaPae(menus[6], menus[5], menus[9]) // Mié: Chocolate hojaldras (7), Carne res (6), Huevos revueltos (10)
                3 -> MenuDiaPae(menus[9], menus[7], menus[6]) // Jue: Huevos revueltos (10), Frijoles papa (8), Chocolate hojaldras (7)
                else -> MenuDiaPae(menus[6], menus[5], menus[8]) // Vie: Chocolate hojaldras (7), Carne res (6), Cuca mango (9)
            }
            2 -> when (diaIndice) {
                0 -> MenuDiaPae(menus[12], menus[10], menus[13]) // Lun: Torta quinua (13), Pollo guisado (11), Yogurt manzana (14)
                1 -> MenuDiaPae(menus[12], menus[11], menus[13]) // Mar: Torta quinua (13), Lentejas mango (12), Yogurt manzana (14)
                2 -> MenuDiaPae(menus[12], menus[14], menus[13]) // Mié: Torta quinua (13), Cerdo verduras (15), Yogurt manzana (14)
                3 -> MenuDiaPae(menus[12], menus[10], menus[13]) // Jue: Torta quinua (13), Pollo guisado (11), Yogurt manzana (14)
                else -> MenuDiaPae(menus[12], menus[11], menus[14]) // Vie: Torta quinua (13), Lentejas mango (12), Cerdo verduras (15)
            }
            else -> when (diaIndice) {
                0 -> MenuDiaPae(menus[16], menus[15], menus[17]) // Lun: Pericos chocolate (17), Lentejas quinua (16), Cuca y mango (18)
                1 -> MenuDiaPae(menus[16], menus[18], menus[17]) // Mar: Pericos chocolate (17), Pollo guisado (19), Cuca y mango (18)
                2 -> MenuDiaPae(menus[16], menus[19], menus[17]) // Mié: Pericos chocolate (17), Frijoles fideos (20), Cuca y mango (18)
                3 -> MenuDiaPae(menus[16], menus[18], menus[17]) // Jue: Pericos chocolate (17), Pollo guisado (19), Cuca y mango (18)
                else -> MenuDiaPae(menus[16], menus[19], menus[15]) // Vie: Pericos chocolate (17), Frijoles fideos (20), Lentejas quinua (16)
            }
        }
    }
}
