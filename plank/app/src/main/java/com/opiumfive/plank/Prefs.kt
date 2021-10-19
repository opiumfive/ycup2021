package com.opiumfive.plank

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Prefs constructor(preferences: SharedPreferences) {
    var stickers by preferences.stringPreference(DATA, "[]")
}


private const val DATA = "data"


/***
Delegates
 */

private class BooleanPreferenceDelegate(
    private val name: String,
    private val defaultValue: Boolean,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
        preferences.getBoolean(name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit().putBoolean(name, value).apply()
    }

}

fun SharedPreferences.booleanPreference(
    name: String,
    defaultValue: Boolean
): ReadWriteProperty<Any, Boolean> = BooleanPreferenceDelegate(name, defaultValue, this)

private class StringPreferenceDelegate(
    private val name: String,
    private val defaultValue: String,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        preferences.getString(name, defaultValue) ?: ""

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        preferences.edit().putString(name, value).apply()
    }
}

fun SharedPreferences.stringPreference(
    name: String,
    defaultValue: String
): ReadWriteProperty<Any, String> = StringPreferenceDelegate(name, defaultValue, this)

private class IntPreferenceDelegate(
    private val name: String,
    private val defaultValue: Int,
    private val preferences: SharedPreferences
) : ReadWriteProperty<Any, Int> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Int =
        preferences.getInt(name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.edit().putInt(name, value).apply()
    }

}