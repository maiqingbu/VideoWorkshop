package com.videoworkshop.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.videoworkshop.core.common.AppConstants

/**
 * 应用级 DataStore 实例（单例，基于 applicationContext）。
 *
 * 使用顶层 [preferencesDataStore] 委托保证全局唯一。
 */
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.PREFERENCES_NAME
)

/**
 * 应用偏好配置中心。
 *
 * 集中定义所有 API Key 的 [Preferences.Key]，涵盖：
 * - AI 模型：DeepSeek、Azure、Groq
 * - 电商联盟：淘宝、京东、拼多多
 */
object AppPreferences {

    // ===== AI 模型 API Keys =====
    val DEEPSEEK_KEY = stringPreferencesKey("deepseek_key")
    val AZURE_KEY = stringPreferencesKey("azure_key")
    val GROQ_KEY = stringPreferencesKey("groq_key")

    // ===== 电商联盟 API Keys =====
    val TAOBAO_APPKEY = stringPreferencesKey("taobao_appkey")
    val TAOBAO_SECRET = stringPreferencesKey("taobao_secret")
    val JD_APPKEY = stringPreferencesKey("jd_appkey")
    val JD_SECRET = stringPreferencesKey("jd_secret")
    val PDD_CLIENT_ID = stringPreferencesKey("pdd_client_id")
    val PDD_CLIENT_SECRET = stringPreferencesKey("pdd_client_secret")

    /**
     * 获取应用级 [DataStore] 实例。建议传入 [Context.getApplicationContext]。
     */
    fun dataStore(context: Context): DataStore<Preferences> = context.appDataStore
}
