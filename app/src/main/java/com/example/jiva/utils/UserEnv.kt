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
    private const val KEY_OWNER_NAME = "owner_name"
    private const val KEY_ADDRESS1 = "address1"
    private const val KEY_ADDRESS2 = "address2"
    private const val KEY_ADDRESS3 = "address3"

    // MsgTemplates storage
    private const val KEY_MSG_TEMPLATES_JSON = "msg_templates_json"

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

    // Owner name
    fun setOwnerName(context: Context, ownerName: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OWNER_NAME, ownerName)
            .apply()
    }

    fun getOwnerName(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OWNER_NAME, null)
    }

    fun clearOwnerName(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_OWNER_NAME)
            .apply()
    }

    // Addresses
    fun setAddress1(context: Context, address: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADDRESS1, address)
            .apply()
    }

    fun getAddress1(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS1, null)
    }

    fun setAddress2(context: Context, address: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADDRESS2, address)
            .apply()
    }

    fun getAddress2(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS2, null)
    }

    fun setAddress3(context: Context, address: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADDRESS3, address)
            .apply()
    }

    fun getAddress3(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS3, null)
    }

    fun clearAddresses(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ADDRESS1)
            .remove(KEY_ADDRESS2)
            .remove(KEY_ADDRESS3)
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

    // MsgTemplates helpers: store whole list JSON for simplicity
    fun setMsgTemplatesJson(context: Context, json: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MSG_TEMPLATES_JSON, json)
            .apply()
    }

    fun getMsgTemplatesJson(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MSG_TEMPLATES_JSON, null)
    }

    fun clearMsgTemplates(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MSG_TEMPLATES_JSON)
            .apply()
    }
}