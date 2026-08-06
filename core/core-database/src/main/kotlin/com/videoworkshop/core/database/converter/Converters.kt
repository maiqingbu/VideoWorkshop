package com.videoworkshop.core.database.converter

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room 类型转换器：处理 [List] 与 [Map] 等复杂类型与数据库文本列之间的转换。
 *
 * 基于 Android 内置 [org.json] 实现，无需额外依赖。
 */
class Converters {

    // ===== List<String> =====

    @TypeConverter
    fun stringListToJson(value: List<String>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val array = JSONArray(value)
        return buildList {
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }

    // ===== Set<String> =====

    @TypeConverter
    fun stringSetToJson(value: Set<String>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringSet(value: String?): Set<String> {
        if (value.isNullOrEmpty()) return emptySet()
        val array = JSONArray(value)
        return buildSet {
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }

    // ===== Map<String, String> =====

    @TypeConverter
    fun stringMapToJson(value: Map<String, String>?): String? {
        if (value == null) return null
        val obj = JSONObject()
        value.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun jsonToStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val obj = JSONObject(value)
        return buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, obj.getString(key))
            }
        }
    }
}
