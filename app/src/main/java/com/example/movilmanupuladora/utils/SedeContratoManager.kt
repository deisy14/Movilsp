package com.example.movilmanupuladora.utils

import android.content.Context
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SedeContratoManager {

    private const val PREFS_NAME = "SEDE_CONTRATO_CONFIG"
    private const val KEY_SECCIONES = "secciones_activas"
    private const val KEY_JORNADAS = "jornadas_activas"
    private const val KEY_CUPOS = "cupos_sede"
    private const val KEY_CONTRATO_NUMERO = "contrato_numero"

    fun resolverIdSedeEfectivo(sessionManager: SessionManager, idSedeParam: Int = 0): Int {
        val idGuardado = sessionManager.getSedeId()
        if (idGuardado > 0) return idGuardado
        if (idSedeParam > 0) return idSedeParam
        return 0
    }

    private fun leerStringSeguro(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                try {
                    val el = obj.get(k)
                    if (el.isJsonPrimitive) return el.asString.trim()
                } catch (_: Exception) { }
            }
        }
        return ""
    }

    private fun leerIdRelacionSeguro(obj: JsonObject, vararg keys: String): Int {
        for (k in keys) {
            if (!obj.has(k) || obj.get(k).isJsonNull) continue
            try {
                val el = obj.get(k)
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) {
                    val v = el.asInt
                    if (v > 0) return v
                } else if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                    val v = el.asString.trim().toIntOrNull()
                    if (v != null && v > 0) return v
                } else if (el.isJsonObject) {
                    val v = leerIntSeguro(el.asJsonObject, "id_contrato", "id_seccion", "id_sede", "id_institucion", "id")
                    if (v > 0) return v
                }
            } catch (_: Exception) { }
        }
        return 0
    }

    private fun normalizarTexto(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val nfd = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun limpiarNombreSede(nombre: String?): String {
        if (nombre.isNullOrBlank()) return ""
        return normalizarTexto(nombre)
            .replace("institucion educativa", " ")
            .replace("i.e.", " ")
            .replace("i.e", " ")
            .replace("sede", " ")
            .replace("·", " ")
            .replace("-", " ")
            .replace(",", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun normalizarNombreSeccion(idSec: Int?, nomSec: String?): String? {
        val nom = nomSec?.trim().orEmpty().lowercase()
        return when {
            idSec == 1 || nom.contains("desayun") -> "Desayuno"
            idSec == 2 || nom.contains("almuerz") -> "Almuerzo"
            idSec == 3 || nom.contains("meriend") || nom.contains("refrigerio") -> "Merienda"
            else -> null
        }
    }

    private fun leerBooleanSeguro(obj: JsonObject, key: String): Boolean {
        if (!obj.has(key) || obj.get(key).isJsonNull) return false
        val el = obj.get(key)
        return try {
            when {
                el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
                el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt != 0
                el.isJsonPrimitive && el.asJsonPrimitive.isString -> {
                    val s = el.asString.trim().lowercase()
                    s == "true" || s == "1" || s == "si" || s == "sí"
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun leerIntSeguro(obj: JsonObject, vararg keys: String): Int {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                try {
                    val el = obj.get(k)
                    if (el.isJsonPrimitive) return el.asInt
                } catch (_: Exception) { }
            }
        }
        return 0
    }

    /**
     * Sincroniza en tiempo real las secciones de menú y cupos asignados a la sede desde el backend.
     * Cero datos quemados: consulta configuración de sede, contratos, distribución por sede y contratos-secciones-menu.
     */
    suspend fun sincronizarSeccionesContrato(context: Context, idSede: Int): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val sessionManager = SessionManager(context)
            sessionManager.fetchAuthToken()

            var idEfectivo = resolverIdSedeEfectivo(sessionManager, idSede)
            val nombreSedeUser = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
            val idInstitucionUser = sessionManager.getInstitucionId()
            val nombreInstUser = sessionManager.getInstitucionNombre()?.trim().orEmpty().lowercase()

            // 0. Si no tenemos el id_sede numérico, intentar resolverlo dinámicamente desde /api/sedes/
            if (idEfectivo <= 0 && nombreSedeUser.isNotEmpty()) {
                val userNomLimpio = limpiarNombreSede(nombreSedeUser)
                try {
                    val sedesRes = RetrofitClient.apiService.obtenerSedes()
                    if (sedesRes.isSuccessful && sedesRes.body() != null) {
                        val root = sedesRes.body()!!
                        val arr = when {
                            root.isJsonArray -> root.asJsonArray
                            root.isJsonObject && root.asJsonObject.has("results") -> root.asJsonObject.getAsJsonArray("results")
                            root.isJsonObject && root.asJsonObject.has("data") -> root.asJsonObject.getAsJsonArray("data")
                            else -> null
                        }
                        if (arr != null) {
                            var fallbackId = 0
                            for (el in arr) {
                                if (!el.isJsonObject) continue
                                val so = el.asJsonObject
                                val sNom = leerStringSeguro(so, "nombre_sede", "nombre")
                                val sNomLimpio = limpiarNombreSede(sNom)
                                val foundId = leerIntSeguro(so, "id_sede", "id")
                                if (foundId > 0 && userNomLimpio.isNotEmpty() && sNomLimpio.isNotEmpty()) {
                                    if (sNomLimpio == userNomLimpio) {
                                        idEfectivo = foundId
                                        break
                                    } else if (fallbackId == 0 && (sNomLimpio.contains(userNomLimpio) || userNomLimpio.contains(sNomLimpio))) {
                                        fallbackId = foundId
                                    }
                                }
                            }
                            if (idEfectivo <= 0 && fallbackId > 0) {
                                idEfectivo = fallbackId
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            val claveSede = "${KEY_SECCIONES}_${if (idEfectivo > 0) idEfectivo else nombreSedeUser}"
            val claveJornadasSede = "${KEY_JORNADAS}_${if (idEfectivo > 0) idEfectivo else nombreSedeUser}"
            val claveCuposSede = "${KEY_CUPOS}_${if (idEfectivo > 0) idEfectivo else nombreSedeUser}"
            val claveContratoSede = "${KEY_CONTRATO_NUMERO}_${if (idEfectivo > 0) idEfectivo else nombreSedeUser}"
            val setNombres = mutableSetOf<String>()
            val jornadasEncontradas = mutableSetOf<String>()

            // 1. Consultar contratos ordenados del ÚLTIMO activo al más antiguo (usando JsonElement para tolerar listas o paginación)
            val contratosRawRes = RetrofitClient.apiService.obtenerContratosRaw()
            val seccionesUltimoContrato = mutableSetOf<String>()
            var encontradoEnUltimoContrato = false

            if (contratosRawRes.isSuccessful && contratosRawRes.body() != null) {
                val rootC = contratosRawRes.body()!!
                val arrC = when {
                    rootC.isJsonArray -> rootC.asJsonArray
                    rootC.isJsonObject && rootC.asJsonObject.has("results") -> rootC.asJsonObject.getAsJsonArray("results")
                    rootC.isJsonObject && rootC.asJsonObject.has("data") -> rootC.asJsonObject.getAsJsonArray("data")
                    else -> null
                }

                val contratos = mutableListOf<JsonObject>()
                if (arrC != null) {
                    for (el in arrC) {
                        if (el.isJsonObject) contratos.add(el.asJsonObject)
                    }
                }

                // Ordenar por estado Activo primero y luego por último contrato creado (id_contrato descendente)
                val contratosOrdenados = contratos.sortedWith(
                    compareByDescending<JsonObject> { cObj ->
                        val est = leerStringSeguro(cObj, "estado")
                        if (est.equals("Activo", ignoreCase = true)) 1 else 0
                    }.thenByDescending { cObj ->
                        val cIdSede = leerIdRelacionSeguro(cObj, "id_sede", "sede_id", "sede")
                        if (idEfectivo > 0 && cIdSede == idEfectivo) 1 else 0
                    }.thenByDescending { cObj ->
                        val cIdInst = leerIdRelacionSeguro(cObj, "id_institucion", "institucion_id", "institucion")
                        if (idInstitucionUser > 0 && cIdInst == idInstitucionUser) 1 else 0
                    }.thenByDescending { cObj ->
                        leerIntSeguro(cObj, "id_contrato", "id")
                    }
                )

                // Cargar todas las relaciones de contratos-secciones-menu (soportando array o { results: [...] })
                val todasRelacionesSecciones = mutableListOf<Pair<Int, String>>()
                try {
                    val sRes = RetrofitClient.apiService.obtenerContratosSeccionesMenuRaw()
                    if (sRes.isSuccessful && sRes.body() != null) {
                        val rootS = sRes.body()!!
                        val arrS = when {
                            rootS.isJsonArray -> rootS.asJsonArray
                            rootS.isJsonObject && rootS.asJsonObject.has("results") -> rootS.asJsonObject.getAsJsonArray("results")
                            rootS.isJsonObject && rootS.asJsonObject.has("data") -> rootS.asJsonObject.getAsJsonArray("data")
                            else -> null
                        }
                        if (arrS != null) {
                            for (el in arrS) {
                                if (!el.isJsonObject) continue
                                val obj = el.asJsonObject
                                val cIdRel = leerIdRelacionSeguro(obj, "id_contrato", "contrato_id", "contrato")
                                val sIdRel = leerIdRelacionSeguro(obj, "id_seccion", "seccion_id", "seccion")
                                val sNomRel = when {
                                    obj.has("nombre_seccion") && obj.get("nombre_seccion").isJsonPrimitive -> obj.get("nombre_seccion").asString
                                    obj.has("seccion_nombre") && obj.get("seccion_nombre").isJsonPrimitive -> obj.get("seccion_nombre").asString
                                    obj.has("id_seccion") && obj.get("id_seccion").isJsonObject -> leerStringSeguro(obj.getAsJsonObject("id_seccion"), "nombre_seccion", "nombre")
                                    obj.has("seccion") && obj.get("seccion").isJsonObject -> leerStringSeguro(obj.getAsJsonObject("seccion"), "nombre_seccion", "nombre")
                                    else -> leerStringSeguro(obj, "nombre")
                                }
                                val norm = normalizarNombreSeccion(sIdRel, sNomRel)
                                if (cIdRel > 0 && norm != null) {
                                    todasRelacionesSecciones.add(Pair(cIdRel, norm))
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }

                // Evaluar los contratos del más reciente al más antiguo hasta encontrar el último contrato que aplica a esta sede
                for (cObj in contratosOrdenados) {
                    val idContrato = leerIntSeguro(cObj, "id_contrato", "id")
                    if (idContrato <= 0) continue
                    val seccionesEsteContrato = mutableSetOf<String>()
                    var aplicaASede = false

                    // A) Revisar si esta sede está en la distribución de este contrato
                    try {
                        val distRes = RetrofitClient.apiService.obtenerDistribucionContrato(idContrato)
                        if (distRes.isSuccessful && distRes.body() != null && distRes.body()!!.isJsonObject) {
                            val distObj = distRes.body()!!.asJsonObject
                            if (distObj.has("sedes") && distObj.get("sedes").isJsonArray) {
                                val sedesArr = distObj.getAsJsonArray("sedes")
                                val userNomLimpio = limpiarNombreSede(nombreSedeUser)
                                var sedeMatch: JsonObject? = null
                                for (sElem in sedesArr) {
                                    if (!sElem.isJsonObject) continue
                                    val sObj = sElem.asJsonObject
                                    val sId = leerIntSeguro(sObj, "id_sede", "id")
                                    val sNom = leerStringSeguro(sObj, "nombre_sede", "nombre")
                                    val sNomLimpio = limpiarNombreSede(sNom)

                                    if (userNomLimpio.isNotEmpty() && sNomLimpio.isNotEmpty() &&
                                        (sNomLimpio == userNomLimpio || sNomLimpio.contains(userNomLimpio) || userNomLimpio.contains(sNomLimpio))
                                    ) {
                                        sedeMatch = sObj
                                        if (sId > 0) idEfectivo = sId
                                        break
                                    }
                                    if (sedeMatch == null && idEfectivo > 0 && sId == idEfectivo) {
                                        sedeMatch = sObj
                                    }
                                }

                                if (sedeMatch != null) {
                                    val sObj = sedeMatch
                                    aplicaASede = true

                                    val cuposSede = leerIntSeguro(sObj, "cupos_asignados", "cupos")
                                    if (cuposSede > 0) {
                                        prefs.edit().putInt(KEY_CUPOS, cuposSede).putInt(claveCuposSede, cuposSede).apply()
                                    }

                                    if (sObj.has("secciones_ids") && sObj.get("secciones_ids").isJsonArray) {
                                        for (sec in sObj.getAsJsonArray("secciones_ids")) {
                                            try {
                                                if (sec.isJsonPrimitive) {
                                                    normalizarNombreSeccion(sec.asInt, null)?.let { seccionesEsteContrato.add(it) }
                                                }
                                            } catch (_: Exception) { }
                                        }
                                    }

                                    if (sObj.has("secciones") && sObj.get("secciones").isJsonArray) {
                                        for (sec in sObj.getAsJsonArray("secciones")) {
                                            if (sec.isJsonObject) {
                                                val so = sec.asJsonObject
                                                val sid = leerIntSeguro(so, "id_seccion", "id")
                                                val snom = leerStringSeguro(so, "nombre_seccion", "nombre")
                                                normalizarNombreSeccion(sid, snom)?.let { seccionesEsteContrato.add(it) }
                                            } else if (sec.isJsonPrimitive) {
                                                val p = sec.asJsonPrimitive
                                                if (p.isNumber) normalizarNombreSeccion(p.asInt, null)?.let { seccionesEsteContrato.add(it) }
                                                else normalizarNombreSeccion(null, p.asString)?.let { seccionesEsteContrato.add(it) }
                                            }
                                        }
                                    }

                                    if (sObj.has("jornadas") && sObj.get("jornadas").isJsonArray) {
                                        for (jElem in sObj.getAsJsonArray("jornadas")) {
                                            if (jElem.isJsonObject) {
                                                val jo = jElem.asJsonObject
                                                if (jo.has("secciones") && jo.get("secciones").isJsonArray) {
                                                    for (sec in jo.getAsJsonArray("secciones")) {
                                                        if (sec.isJsonObject) {
                                                            val so = sec.asJsonObject
                                                            val sid = leerIntSeguro(so, "id_seccion", "id")
                                                            val snom = leerStringSeguro(so, "nombre_seccion", "nombre")
                                                            normalizarNombreSeccion(sid, snom)?.let { seccionesEsteContrato.add(it) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Si la distribución no devolvió secciones explícitas, consultar primero /api/sedes/{id}/configuracion/
                                    // (configuración individual de esta sede sincronizada desde el contrato), luego contratos-secciones-menu
                                    // filtrado por las jornadas habilitadas de esta sede, y por último flags de jornada.
                                    if (seccionesEsteContrato.isEmpty()) {
                                        var flagsManana = leerBooleanSeguro(sObj, "jornada_manana")
                                        var flagsTarde = leerBooleanSeguro(sObj, "jornada_tarde")
                                        if (sObj.has("jornadas") && sObj.get("jornadas").isJsonArray) {
                                            for (jElem in sObj.getAsJsonArray("jornadas")) {
                                                if (jElem.isJsonPrimitive) {
                                                    val jStr = jElem.asString.lowercase()
                                                    if (jStr.contains("mañan") || jStr.contains("manan")) flagsManana = true
                                                    if (jStr.contains("tard")) flagsTarde = true
                                                }
                                            }
                                        }

                                        if (idEfectivo > 0) {
                                            try {
                                                val confRes = RetrofitClient.apiService.obtenerConfiguracionSede(idEfectivo)
                                                if (confRes.isSuccessful && confRes.body() != null && confRes.body()!!.isJsonObject) {
                                                    val confObj = confRes.body()!!.asJsonObject
                                                    if (confObj.has("jornadas") && confObj.get("jornadas").isJsonArray) {
                                                        for (jElem in confObj.getAsJsonArray("jornadas")) {
                                                            if (!jElem.isJsonObject) continue
                                                            val jObj = jElem.asJsonObject
                                                            val hab = leerBooleanSeguro(jObj, "habilitada")
                                                            val cupJ = leerIntSeguro(jObj, "cupos")
                                                            val tieneSec = jObj.has("secciones") && jObj.get("secciones").isJsonArray && jObj.getAsJsonArray("secciones").size() > 0
                                                            if ((hab || cupJ > 0 || tieneSec) && jObj.has("secciones") && jObj.get("secciones").isJsonArray) {
                                                                for (secEl in jObj.getAsJsonArray("secciones")) {
                                                                    val secNorm = if (secEl.isJsonObject) {
                                                                        val secObj = secEl.asJsonObject
                                                                        val sid = leerIntSeguro(secObj, "id_seccion", "id")
                                                                        val snom = leerStringSeguro(secObj, "nombre_seccion", "nombre")
                                                                        normalizarNombreSeccion(sid, snom)
                                                                    } else if (secEl.isJsonPrimitive) {
                                                                        val prim = secEl.asJsonPrimitive
                                                                        if (prim.isNumber) normalizarNombreSeccion(prim.asInt, null)
                                                                        else normalizarNombreSeccion(null, prim.asString)
                                                                    } else null

                                                                    if (secNorm != null) {
                                                                        val esSecManana = secNorm == "Desayuno"
                                                                        val esSecTarde = secNorm == "Almuerzo" || secNorm == "Merienda"
                                                                        val coincideJornada = (!flagsManana && !flagsTarde) ||
                                                                                (flagsManana && esSecManana) ||
                                                                                (flagsTarde && esSecTarde)
                                                                        if (coincideJornada) {
                                                                            seccionesEsteContrato.add(secNorm)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) { }
                                        }

                                        if (seccionesEsteContrato.isEmpty()) {
                                            val rels = todasRelacionesSecciones.filter { it.first == idContrato }
                                            if (rels.isNotEmpty()) {
                                                for ((_, secNorm) in rels) {
                                                    val esSecManana = secNorm == "Desayuno"
                                                    val esSecTarde = secNorm == "Almuerzo" || secNorm == "Merienda"
                                                    val coincideJornada = (!flagsManana && !flagsTarde) ||
                                                            (flagsManana && esSecManana) ||
                                                            (flagsTarde && esSecTarde)
                                                    if (coincideJornada) {
                                                        seccionesEsteContrato.add(secNorm)
                                                    }
                                                }
                                            }
                                        }

                                        if (seccionesEsteContrato.isEmpty()) {
                                            if (flagsManana) seccionesEsteContrato.add("Desayuno")
                                            if (flagsTarde) seccionesEsteContrato.add("Almuerzo")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) { }

                    // B) Si el contrato pertenece directamente a ESTA sede (sin tomar contratos asignados a otra sede distinta)
                    if (!aplicaASede) {
                        val cIdSede = leerIdRelacionSeguro(cObj, "id_sede", "sede_id", "sede")
                        val cSedeNom = leerStringSeguro(cObj, "sede_nombre").ifEmpty {
                            if (cObj.has("sede") && cObj.get("sede").isJsonObject) leerStringSeguro(cObj.getAsJsonObject("sede"), "nombre_sede", "nombre") else ""
                        }
                        val cIdInst = leerIdRelacionSeguro(cObj, "id_institucion", "institucion_id", "institucion")
                        val cInstNom = leerStringSeguro(cObj, "institucion_nombre", "institucion").ifEmpty {
                            if (cObj.has("institucion") && cObj.get("institucion").isJsonObject) leerStringSeguro(cObj.getAsJsonObject("institucion"), "nombre_institucion", "nombre") else ""
                        }

                        val perteneceAOtraSede = (idEfectivo > 0 && cIdSede > 0 && cIdSede != idEfectivo)
                        if (!perteneceAOtraSede) {
                            val coincideSede = (idEfectivo > 0 && cIdSede == idEfectivo) ||
                                    (nombreSedeUser.isNotEmpty() && cSedeNom.isNotEmpty() && limpiarNombreSede(cSedeNom).contains(limpiarNombreSede(nombreSedeUser)))
                            val coincideInstitucionSinSede = (cIdSede <= 0) &&
                                    ((idInstitucionUser > 0 && cIdInst == idInstitucionUser) ||
                                    (nombreInstUser.isNotEmpty() && cInstNom.lowercase().contains(nombreInstUser)))
                            if (coincideSede || coincideInstitucionSinSede) {
                                aplicaASede = true
                            }
                        }
                    }

                    // C) Enriquecer/obtener secciones del contrato desde contratos-secciones-menu si aún no las trajo la distribución
                    if (aplicaASede && seccionesEsteContrato.isEmpty()) {
                        val rels = todasRelacionesSecciones.filter { it.first == idContrato }
                        for ((_, secNorm) in rels) {
                            seccionesEsteContrato.add(secNorm)
                        }
                    }

                    // Si este último contrato aplicable a la sede ya tiene sus secciones definidas, nos quedamos SOLO con las de este contrato
                    if (aplicaASede) {
                        val numContrato = leerStringSeguro(cObj, "numero_cor", "codigo", "numero_contrato")
                        if (numContrato.isNotBlank()) {
                            prefs.edit()
                                .putString(KEY_CONTRATO_NUMERO, numContrato)
                                .putString(claveContratoSede, numContrato)
                                .apply()
                        }
                    }

                    if (aplicaASede && seccionesEsteContrato.isNotEmpty()) {
                        seccionesUltimoContrato.addAll(seccionesEsteContrato)
                        encontradoEnUltimoContrato = true
                        break
                    }
                }
            }

            if (encontradoEnUltimoContrato && seccionesUltimoContrato.isNotEmpty()) {
                setNombres.clear()
                setNombres.addAll(seccionesUltimoContrato)
            } else if (setNombres.isEmpty() && idEfectivo > 0) {
                // 2. Fallback: consultar configuración oficial directa de la sede (/api/sedes/{id}/configuracion/)
                try {
                    val confRes = RetrofitClient.apiService.obtenerConfiguracionSede(idEfectivo)
                    if (confRes.isSuccessful && confRes.body() != null && confRes.body()!!.isJsonObject) {
                        val confObj = confRes.body()!!.asJsonObject
                        val cuposTotales = leerIntSeguro(confObj, "cupos_totales", "cupos")
                        if (cuposTotales > 0) {
                            prefs.edit().putInt(KEY_CUPOS, cuposTotales).putInt(claveCuposSede, cuposTotales).apply()
                        }

                        if (confObj.has("jornadas") && confObj.get("jornadas").isJsonArray) {
                            val jArr = confObj.getAsJsonArray("jornadas")
                            for (jElem in jArr) {
                                if (!jElem.isJsonObject) continue
                                val jObj = jElem.asJsonObject
                                val habilitada = leerBooleanSeguro(jObj, "habilitada")
                                val cuposJ = leerIntSeguro(jObj, "cupos")
                                val tieneSecciones = jObj.has("secciones") && jObj.get("secciones").isJsonArray && jObj.getAsJsonArray("secciones").size() > 0

                                if (habilitada || cuposJ > 0 || tieneSecciones) {
                                    val nomJ = leerStringSeguro(jObj, "nombre_jornada", "nombre").lowercase()
                                    if (nomJ.contains("mañan") || nomJ.contains("manan")) jornadasEncontradas.add("Mañana")
                                    if (nomJ.contains("tard")) jornadasEncontradas.add("Tarde")
                                    if (jObj.has("secciones") && jObj.get("secciones").isJsonArray) {
                                        for (secEl in jObj.getAsJsonArray("secciones")) {
                                            if (secEl.isJsonObject) {
                                                val secObj = secEl.asJsonObject
                                                val sId = leerIntSeguro(secObj, "id_seccion", "id")
                                                val sNom = secObj.get("nombre_seccion")?.takeIf { !it.isJsonNull }?.asString
                                                    ?: secObj.get("nombre")?.takeIf { !it.isJsonNull }?.asString
                                                normalizarNombreSeccion(sId, sNom)?.let { setNombres.add(it) }
                                            } else if (secEl.isJsonPrimitive) {
                                                val prim = secEl.asJsonPrimitive
                                                if (prim.isNumber) normalizarNombreSeccion(prim.asInt, null)?.let { setNombres.add(it) }
                                                else normalizarNombreSeccion(null, prim.asString)?.let { setNombres.add(it) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            if (jornadasEncontradas.isNotEmpty()) {
                prefs.edit().putStringSet(claveJornadasSede, jornadasEncontradas).apply()
            } else if (setNombres.isNotEmpty()) {
                val jInferidas = mutableSetOf<String>()
                if (setNombres.contains("Desayuno") || setNombres.contains("Almuerzo")) jInferidas.add("Mañana")
                if (setNombres.contains("Merienda")) jInferidas.add("Tarde")
                if (jInferidas.isNotEmpty()) {
                    prefs.edit().putStringSet(claveJornadasSede, jInferidas).apply()
                }
            }

            if (setNombres.isNotEmpty()) {
                prefs.edit().putStringSet(claveSede, setNombres).apply()
                return@withContext setNombres
            }
        } catch (_: Exception) { }

        return@withContext obtenerSeccionesActivas(context)
    }

    /**
     * Retorna las secciones de menú activas para la sede actual según el contrato sincronizado.
     */
    fun obtenerSeccionesActivas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionManager = SessionManager(context)
        val idEfectivo = resolverIdSedeEfectivo(sessionManager)
        val sedeNom = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
        val claveSede = "${KEY_SECCIONES}_${if (idEfectivo > 0) idEfectivo else sedeNom}"

        val guardadasSede = prefs.getStringSet(claveSede, null)
        if (!guardadasSede.isNullOrEmpty()) {
            return guardadasSede
        }

        // Fallback temporal mientras termina la primera sincronización de red para la sede
        return setOf("Desayuno")
    }

    /**
     * Retorna los cupos asignados a la sede en el contrato según la base de datos (0 si no configurado).
     */
    fun obtenerCuposSede(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionManager = SessionManager(context)
        val idEfectivo = resolverIdSedeEfectivo(sessionManager)
        val sedeNom = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
        val claveCuposSede = "${KEY_CUPOS}_${if (idEfectivo > 0) idEfectivo else sedeNom}"
        val cuposSede = prefs.getInt(claveCuposSede, 0)
        return if (cuposSede > 0) cuposSede else prefs.getInt(KEY_CUPOS, 0)
    }

    /**
     * Retorna el número de contrato PAE asignado a la sede actual (ej. PAE-2026-294).
     */
    fun obtenerNumeroContrato(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionManager = SessionManager(context)
        val idEfectivo = resolverIdSedeEfectivo(sessionManager)
        val sedeNom = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
        val claveContratoSede = "${KEY_CONTRATO_NUMERO}_${if (idEfectivo > 0) idEfectivo else sedeNom}"
        return prefs.getString(claveContratoSede, null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_CONTRATO_NUMERO, null)?.takeIf { it.isNotBlank() }
    }

    /**
     * Retorna las jornadas habilitadas para la sede según su contrato ("Mañana", "Tarde").
     */
    fun obtenerJornadasActivas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionManager = SessionManager(context)
        val idEfectivo = resolverIdSedeEfectivo(sessionManager)
        val sedeNom = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
        val claveJornadasSede = "${KEY_JORNADAS}_${if (idEfectivo > 0) idEfectivo else sedeNom}"
        val guardadas = prefs.getStringSet(claveJornadasSede, null)
        if (!guardadas.isNullOrEmpty()) return guardadas

        val secciones = obtenerSeccionesActivas(context)
        val inferidas = mutableSetOf<String>()
        if (secciones.contains("Desayuno") || secciones.contains("Almuerzo")) inferidas.add("Mañana")
        if (secciones.contains("Merienda")) inferidas.add("Tarde")
        return inferidas.ifEmpty { setOf("Mañana") }
    }
}
