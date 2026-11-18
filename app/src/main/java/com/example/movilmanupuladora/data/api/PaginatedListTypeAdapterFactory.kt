package com.example.movilmanupuladora.data.api

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * TypeAdapterFactory para Gson que maneja automáticamente tanto respuestas en listas directas:
 *   [ {...}, {...} ]
 * como respuestas paginadas típicas de Django REST Framework:
 *   { "count": 10, "next": null, "previous": null, "results": [ {...}, {...} ] }
 *   { "data": [ {...}, {...} ] }
 */
class PaginatedListTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType = typeToken.rawType
        if (!Collection::class.java.isAssignableFrom(rawType)) {
            return null
        }

        val type = typeToken.type
        val elementType: Type = if (type is ParameterizedType) {
            type.actualTypeArguments.firstOrNull() ?: Any::class.java
        } else {
            Any::class.java
        }

        val elementAdapter = gson.getAdapter(TypeToken.get(elementType))
        val delegateAdapter = gson.getDelegateAdapter(this, typeToken)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                delegateAdapter.write(out, value)
            }

            @Suppress("UNCHECKED_CAST")
            override fun read(reader: JsonReader): T {
                val peek = reader.peek()
                if (peek == JsonToken.NULL) {
                    reader.nextNull()
                    return emptyList<Any?>() as T
                }

                if (peek == JsonToken.BEGIN_ARRAY) {
                    val list = mutableListOf<Any?>()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        list.add(elementAdapter.read(reader))
                    }
                    reader.endArray()
                    return list as T
                }

                if (peek == JsonToken.BEGIN_OBJECT) {
                    val list = mutableListOf<Any?>()
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if ((name == "results" || name == "data") && reader.peek() == JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                list.add(elementAdapter.read(reader))
                            }
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                    return list as T
                }

                reader.skipValue()
                return emptyList<Any?>() as T
            }
        }
    }
}
