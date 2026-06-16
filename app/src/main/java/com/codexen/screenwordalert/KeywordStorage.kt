package com.codexen.screenwordalert

import android.content.Context
import android.content.SharedPreferences

class KeywordStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("screenword_prefs", Context.MODE_PRIVATE)

    fun saveKeyword(keyword: String) {
        val list = getKeywords().toMutableList()
        if (!list.contains(keyword.trim())) {
            list.add(keyword.trim())
            prefs.edit().putString("keywords", list.joinToString(",")).apply()
        }
    }

    fun getKeywords(): List<String> {
        val raw = prefs.getString("keywords", "") ?: ""
        return if (raw.isEmpty()) emptyList()
        else raw.split(",").filter { it.isNotEmpty() }
    }

    fun deleteKeyword(keyword: String) {
        val list = getKeywords().toMutableList()
        list.remove(keyword)
        prefs.edit().putString("keywords", list.joinToString(",")).apply()
    }

    fun saveTargetPackage(pkg: String) {
        val list = getTargetPackages().toMutableList()
        if (!list.contains(pkg.trim()) && pkg.isNotEmpty()) {
            list.add(pkg.trim())
            prefs.edit().putString("target_packages", list.joinToString(",")).apply()
        }
    }

    fun getTargetPackages(): List<String> {
        val raw = prefs.getString("target_packages", "") ?: ""
        return if (raw.isEmpty()) emptyList()
        else raw.split(",").filter { it.isNotEmpty() }
    }

    fun deleteTargetPackage(pkg: String) {
        val list = getTargetPackages().toMutableList()
        list.remove(pkg)
        prefs.edit().putString("target_packages", list.joinToString(",")).apply()
    }

    // Keep single get for service compatibility
    fun getTargetPackage(): String {
        return getTargetPackages().joinToString(",")
    }
}