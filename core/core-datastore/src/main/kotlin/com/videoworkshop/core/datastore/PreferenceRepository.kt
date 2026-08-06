package com.videoworkshop.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 偏好配置读写仓库。
 *
 * 对外暴露各 API Key 的响应式 [Flow] 读取与 [suspend] 写入方法，
 * 内部基于 DataStore 保证线程安全与异步友好。
 *
 * @param dataStore 应用级 DataStore 实例。
 */
class PreferenceRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * 便捷构造：基于 [Context] 自动获取应用级 DataStore。
     */
    constructor(context: Context) : this(AppPreferences.dataStore(context.applicationContext))

    // ===== 读取：AI 模型 Keys =====
    val deepseekKey: Flow<String> = observe(AppPreferences.DEEPSEEK_KEY)
    val azureKey: Flow<String> = observe(AppPreferences.AZURE_KEY)
    val groqKey: Flow<String> = observe(AppPreferences.GROQ_KEY)

    // ===== 读取：电商联盟 Keys =====
    val taobaoAppKey: Flow<String> = observe(AppPreferences.TAOBAO_APPKEY)
    val taobaoSecret: Flow<String> = observe(AppPreferences.TAOBAO_SECRET)
    val jdAppKey: Flow<String> = observe(AppPreferences.JD_APPKEY)
    val jdSecret: Flow<String> = observe(AppPreferences.JD_SECRET)
    val pddClientId: Flow<String> = observe(AppPreferences.PDD_CLIENT_ID)
    val pddClientSecret: Flow<String> = observe(AppPreferences.PDD_CLIENT_SECRET)

    // ===== 读取：外观偏好 =====
    /** 主题模式：system（跟随系统）/ light（浅色）/ dark（深色）。 */
    val themeMode: Flow<String> = observe(AppPreferences.THEME_MODE)

    // ===== 写入：AI 模型 Keys =====
    suspend fun setDeepseekKey(value: String) = write(AppPreferences.DEEPSEEK_KEY, value)
    suspend fun setAzureKey(value: String) = write(AppPreferences.AZURE_KEY, value)
    suspend fun setGroqKey(value: String) = write(AppPreferences.GROQ_KEY, value)

    // ===== 写入：电商联盟 Keys =====
    suspend fun setTaobaoAppKey(value: String) = write(AppPreferences.TAOBAO_APPKEY, value)
    suspend fun setTaobaoSecret(value: String) = write(AppPreferences.TAOBAO_SECRET, value)
    suspend fun setJdAppKey(value: String) = write(AppPreferences.JD_APPKEY, value)
    suspend fun setJdSecret(value: String) = write(AppPreferences.JD_SECRET, value)
    suspend fun setPddClientId(value: String) = write(AppPreferences.PDD_CLIENT_ID, value)
    suspend fun setPddClientSecret(value: String) = write(AppPreferences.PDD_CLIENT_SECRET, value)

    // ===== 写入：外观偏好 =====
    suspend fun setThemeMode(value: String) = write(AppPreferences.THEME_MODE, value)

    /**
     * 清空全部配置（用于退出登录/重置场景）。
     */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // ===== 内部工具 =====

    private fun observe(key: Preferences.Key<String>): Flow<String> =
        dataStore.data.map { preferences -> preferences[key].orEmpty() }

    private suspend fun write(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences -> preferences[key] = value }
    }
}
