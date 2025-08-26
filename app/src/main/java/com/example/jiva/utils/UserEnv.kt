package com.example.jiva.utils

import android.content.Context

/**
 * Lightweight environment-like store for app-wide values.
 * Uses SharedPreferences under the hood.
 */
object UserEnv {
    private const val PREF_NAME = "jiva_env_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_COMPANY_NAME = "company_name"
    private const val KEY_FINANCIAL_YEAR = "financial_year"

    fun setUserId(context: Context, userId: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)
    }

    fun clearUserId(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_ID)
            .apply()
    }

    fun setCompanyName(context: Context, companyName: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMPANY_NAME, companyName)
            .apply()
    }

    fun getCompanyName(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COMPANY_NAME, null)
    }

    fun clearCompanyName(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_COMPANY_NAME)
            .apply()
    }

    fun setFinancialYear(context: Context, year: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FINANCIAL_YEAR, year)
            .apply()
    }

    fun getFinancialYear(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FINANCIAL_YEAR, null)
    }

    fun clearFinancialYear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FINANCIAL_YEAR)
            .apply()
    }
}