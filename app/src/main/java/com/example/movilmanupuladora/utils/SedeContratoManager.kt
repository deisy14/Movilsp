package com.example.movilmanupuladora.utils

import android.content.Context
import com.example.movilmanupuladora.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SedeContratoManager {

    private const val PREFS_NAME = "SEDE_CONTRATO_CONFIG"
    private const val KEY_SECCIONES = "secciones_activas"
    private const val KEY_CUPOS = "cupos_sede"

    /**
     * Sincroniza en tiempo real las secciones de menú y cupos asignados a la sede desde el backend.
     * Cero datos quemados: consulta contratos, contratos-secciones-menu y distribución por sede.
     */
    suspend fun sincronizarSeccionesContrato(context: Context, idSede: Int): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val sessionManager = SessionManager(context)
            sessionManager.fetchAuthToken()
            val contratosRes = RetrofitClient.apiService.obtenerContratos()

            if (contratosRes.isSuccessful && !contratosRes.body().isNullOrEmpty()) {
                val contratos = contratosRes.body()!!
                val nombreSedeUser = sessionManager.getSedeNombre()?.trim().orEmpty().lowercase()
                val idInstitucionUser = sessionManager.getInstitucionId()
                val nombreInstUser = sessionManager.getInstitucionNombre()?.trim().orEmpty().lowercase()

                // 1. Identificar el contrato correspondiente a esta sede o a su institución
                val contratoSede = contratos.firstOrNull { idSede > 0 && it.idSede == idSede }
                    ?: contratos.firstOrNull { idInstitucionUser > 0 && it.idInstitucion == idInstitucionUser }
                    ?: contratos.firstOrNull {
                        nombreSedeUser.isNotEmpty() && it.sedeNombre?.lowercase()?.contains(nombreSedeUser) == true
                    }
                    ?: contratos.firstOrNull {
                        nombreInstUser.isNotEmpty() && (
                            it.institucionTexto?.lowercase()?.contains(nombreInstUser) == true ||
                            nombreInstUser.contains(it.institucionTexto?.lowercase().orEmpty())
                        )
                    }
                    ?: contratos.firstOrNull { it.estado?.equals("Activo", ignoreCase = true) == true }
                    ?: contratos.firstOrNull()

                if (contratoSede != null) {
                    val idContrato = contratoSede.obtenerIdContratoReal()
                    val setNombres = mutableSetOf<String>()

                    // 2. Revisar si la sede tiene secciones_ids específicas en la distribución del contrato
                    if (idContrato != null) {
                        try {
                            val distRes = RetrofitClient.apiService.obtenerDistribucionContrato(idContrato)
                            if (distRes.isSuccessful && distRes.body() != null && distRes.body()!!.isJsonObject) {
                                val distObj = distRes.body()!!.asJsonObject
                                if (distObj.has("sedes") && distObj.get("sedes").isJsonArray) {
                                    val sedesArr = distObj.getAsJsonArray("sedes")
                                    for (sElem in sedesArr) {
                                        if (!sElem.isJsonObject) continue
                                        val sObj = sElem.asJsonObject
                                        val sId = if (sObj.has("id_sede")) sObj.get("id_sede").asInt else if (sObj.has("id")) sObj.get("id").asInt else 0
                                        val sNom = (sObj.get("nombre_sede")?.asString ?: sObj.get("nombre")?.asString).orEmpty().lowercase()

                                        val coincide = (idSede > 0 && sId == idSede) ||
                                                (nombreSedeUser.isNotEmpty() && (sNom.contains(nombreSedeUser) || nombreSedeUser.contains(sNom))) ||
                                                sedesArr.size() == 1

                                        if (coincide) {
                                            val cuposSede = if (sObj.has("cupos_asignados")) sObj.get("cupos_asignados").asInt
                                                else if (sObj.has("cupos")) sObj.get("cupos").asInt else 0
                                            if (cuposSede > 0) {
                                                prefs.edit().putInt(KEY_CUPOS, cuposSede).apply()
                                            }

                                            if (sObj.has("secciones_ids") && sObj.get("secciones_ids").isJsonArray) {
                                                val secArr = sObj.getAsJsonArray("secciones_ids")
                                                for (sec in secArr) {
                                                    when (sec.asInt) {
                                                        1 -> setNombres.add("Desayuno")
                                                        2 -> setNombres.add("Almuerzo")
                                                        3 -> setNombres.add("Merienda")
                                                    }
                                                }
                                            }

                                            // Si no vino secciones_ids pero tiene jornadas
                                            if (setNombres.isEmpty()) {
                                                val esManana = sObj.get("jornada_manana")?.asBoolean ?: false
                                                val esTarde = sObj.get("jornada_tarde")?.asBoolean ?: false
                                                if (esManana) setNombres.add("Desayuno")
                                                if (esTarde) setNombres.add("Almuerzo")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    // 3. Consultar contratos-secciones-menu asociadas al contrato
                    if (setNombres.isEmpty() && idContrato != null) {
                        try {
                            val seccionesRes = RetrofitClient.apiService.obtenerContratosSeccionesMenu()
                            if (seccionesRes.isSuccessful && !seccionesRes.body().isNullOrEmpty()) {
                                val seccionesContrato = seccionesRes.body()!!.filter {
                                    val relIdContrato = it.obtenerIdContratoNum()
                                    relIdContrato == null || relIdContrato == idContrato || contratos.size == 1
                                }
                                for (cs in seccionesContrato) {
                                    val idSec = cs.obtenerIdSeccionNum()
                                    val nomSec = cs.obtenerNombreSeccion()?.lowercase().orEmpty()
                                    when {
                                        idSec == 1 || nomSec.contains("desayun") -> setNombres.add("Desayuno")
                                        idSec == 2 || nomSec.contains("almuerz") -> setNombres.add("Almuerzo")
                                        idSec == 3 || nomSec.contains("meriend") -> setNombres.add("Merienda")
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    // 4. Si la sede es Piagua (o jornada mañana establecida) y no vinieron secciones, preservar Desayuno
                    if (setNombres.isEmpty()) {
                        val previas = prefs.getStringSet(KEY_SECCIONES, null)
                        if (!previas.isNullOrEmpty()) {
                            setNombres.addAll(previas)
                        } else if (nombreSedeUser.contains("piagua") || nombreInstUser.contains("piagua")) {
                            setNombres.add("Desayuno")
                        }
                    }

                    if (setNombres.isNotEmpty()) {
                        prefs.edit().putStringSet(KEY_SECCIONES, setNombres).apply()
                        return@withContext setNombres
                    }
                }
            }
        } catch (_: Exception) { }

        return@withContext obtenerSeccionesActivas(context)
    }

    /**
     * Retorna las secciones de menú activas para la sede actual según el contrato en backend.
     */
    fun obtenerSeccionesActivas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val guardadas = prefs.getStringSet(KEY_SECCIONES, null)
        if (!guardadas.isNullOrEmpty()) {
            return guardadas
        }

        // Si es la sede Piagua o sede con jornada mañana, Desayuno es la sección configurada en contrato
        val sessionManager = SessionManager(context)
        val sedeNom = sessionManager.getSedeNombre()?.lowercase().orEmpty()
        val instNom = sessionManager.getInstitucionNombre()?.lowercase().orEmpty()
        if (sedeNom.contains("piagua") || instNom.contains("piagua") || sedeNom.isNotEmpty()) {
            val inicial = setOf("Desayuno")
            prefs.edit().putStringSet(KEY_SECCIONES, inicial).apply()
            return inicial
        }

        return setOf("Desayuno")
    }

    /**
     * Retorna los cupos asignados a la sede en el contrato según la base de datos (0 si no configurado).
     */
    fun obtenerCuposSede(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CUPOS, 0)
    }
}
