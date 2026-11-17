package com.example.movilmanupuladora

import com.example.movilmanupuladora.data.model.DescontarInventarioRequest
import com.example.movilmanupuladora.data.model.SugerenciasRecetasResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SugerenciasRecetasParsingTest {

    @Test
    fun parsesMixedObjectAndStringIngredientsWithoutError() {
        val sampleBackendJson = """
            {
              "status": "success",
              "seccion": "Desayuno",
              "id_seccion": 1,
              "ninos_presentes": 100,
              "id_sede": 1,
              "sugerencias_ia_estructuradas": [
                {
                  "id_plato": null,
                  "nombre_plato": "Sugerencia IA: Huevos campesinos",
                  "id_seccion": 1,
                  "seccion_nombre": "Desayuno",
                  "componente": "Proteico y cereal",
                  "ninos_presentes": 100,
                  "ingredientes": [
                    {
                      "id_ingrediente": 8,
                      "id_inventario": null,
                      "nombre_ingrediente": "Huevos",
                      "cantidad_por_nino": "1.0 und",
                      "cantidad_por_nino_valor": "1.0",
                      "cantidad_total": "100.00 und",
                      "cantidad_total_valor": "100.0",
                      "unidad": "u",
                      "stock_actual": "100.0",
                      "stock_restante_proyectado": "0.00",
                      "alcanza": true
                    }
                  ],
                  "pasos": ["Batir los huevos"],
                  "origen": "SUGERENCIA_IA",
                  "viable_con_inventario": false
                }
              ],
              "platos_estipulados_pae": [
                {
                  "id_plato": null,
                  "nombre_plato": "Pericos con arroz",
                  "id_seccion": 1,
                  "seccion_nombre": "Desayuno",
                  "componente": "Refresco de avena, Huevos pericos",
                  "ingredientes": ["Avena", "Leche líquida", "Huevos"],
                  "pasos": ["Preparar refresco"],
                  "origen": "PAE_ESTIPULADO"
                }
              ]
            }
        """.trimIndent()

        val gson = Gson()
        val parsed = gson.fromJson(sampleBackendJson, SugerenciasRecetasResponse::class.java)

        assertEquals("success", parsed.status)
        assertEquals(1, parsed.sugerenciasIaEstructuradas.size)
        val iaItem = parsed.sugerenciasIaEstructuradas.first()
        assertEquals("Sugerencia IA: Huevos campesinos", iaItem.nombrePlato)
        assertFalse(iaItem.viableConInventario)
        assertEquals(1, iaItem.ingredientes.size)
        assertEquals("Huevos", iaItem.ingredientes.first().nombreIngrediente)
        assertEquals("100.00 und", iaItem.ingredientes.first().cantidadTotal)
        assertTrue(iaItem.ingredientes.first().alcanza)

        assertEquals(1, parsed.platosEstipuladosPae.size)
        val paeItem = parsed.platosEstipuladosPae.first()
        assertEquals("Pericos con arroz", paeItem.nombrePlato)
        assertEquals(3, paeItem.ingredientes.size)
        assertEquals("Avena", paeItem.ingredientes[0].nombreIngrediente)
        assertEquals("Leche líquida", paeItem.ingredientes[1].nombreIngrediente)
        assertEquals("Huevos", paeItem.ingredientes[2].nombreIngrediente)

        val descuentoJson = gson.toJson(
            DescontarInventarioRequest(
                idSede = 1,
                idPlato = iaItem.idPlato,
                nombrePlato = iaItem.nombrePlato,
                ninosPresentes = 100,
                ingredientes = iaItem.ingredientes
            )
        )
        assertTrue(descuentoJson.contains("\"nombre_ingrediente\":\"Huevos\""))
    }
}
