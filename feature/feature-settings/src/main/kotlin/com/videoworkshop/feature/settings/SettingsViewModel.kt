package com.videoworkshop.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.datastore.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 单条凭证字段描述：标签 + 当前密文显示值。
 */
data class CredentialField(
    val label: String,
    val value: String
) {
    /** 是否已配置。空串视为未配置。 */
    val isConfigured: Boolean get() = value.isNotBlank()

    /** 密文展示：仅显示前 4 位 + ******；空串返回空字符串。 */
    val masked: String
        get() = when {
            value.isBlank() -> ""
            value.length <= 4 -> "****"
            else -> "${value.take(4)}${"*".repeat(8)}"
        }
}

/**
 * AI 设置分组：DeepSeek / Azure / Groq 三个 Key。
 */
data class AiSettings(
    val deepSeekKey: CredentialField = CredentialField("DeepSeek API Key", ""),
    val azureKey: CredentialField = CredentialField("Azure TTS Key", ""),
    val groqKey: CredentialField = CredentialField("Groq API Key", ""),
)

/**
 * 联盟凭证分组：淘宝 / 京东 / 拼多多。
 */
data class AllianceSettings(
    val taobaoAppKey: CredentialField = CredentialField("淘宝 AppKey", ""),
    val taobaoSecret: CredentialField = CredentialField("淘宝 AppSecret", ""),
    val jdAppKey: CredentialField = CredentialField("京东 AppKey", ""),
    val jdSecret: CredentialField = CredentialField("京东 AppSecret", ""),
    val pddClientId: CredentialField = CredentialField("拼多多 ClientId", ""),
    val pddClientSecret: CredentialField = CredentialField("拼多多 ClientSecret", ""),
) {
    /** 任一平台完整配置即视为已配置。 */
    val anyConfigured: Boolean
        get() = (taobaoAppKey.isConfigured && taobaoSecret.isConfigured) ||
            (jdAppKey.isConfigured && jdSecret.isConfigured) ||
            (pddClientId.isConfigured && pddClientSecret.isConfigured)
}

/**
 * 关于页信息：版本号、构建时间、GitHub 仓库链接。
 */
data class AboutInfo(
    val versionName: String,
    val buildTime: String,
    val githubUrl: String = "https://github.com/maiqingbu"
)

/**
 * 缓存清理相关信息。
 */
data class CacheInfo(
    val sizeBytes: Long,
    val cleaned: Boolean = false,
)

/**
 * 设置页 UI 状态聚合。
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val ai: AiSettings = AiSettings(),
    val alliance: AllianceSettings = AllianceSettings(),
    val about: AboutInfo = AboutInfo(versionName = "", buildTime = ""),
    val cache: CacheInfo = CacheInfo(sizeBytes = 0L),
    val themeMode: String = "system",
    val editingFieldKey: String? = null,
    val editingInitialValue: String = "",
    val toast: String? = null
)

/**
 * 设置页字段编辑目标：标识当前正在编辑的字段。
 */
enum class SettingsField(val key: String, val label: String) {
    DEEPSEEK_KEY("deepseek_key", "DeepSeek API Key"),
    AZURE_KEY("azure_key", "Azure TTS Key"),
    GROQ_KEY("groq_key", "Groq API Key"),
    TAOBAO_APPKEY("taobao_appkey", "淘宝 AppKey"),
    TAOBAO_SECRET("taobao_secret", "淘宝 AppSecret"),
    JD_APPKEY("jd_appkey", "京东 AppKey"),
    JD_SECRET("jd_secret", "京东 AppSecret"),
    PDD_CLIENT_ID("pdd_client_id", "拼多多 ClientId"),
    PDD_CLIENT_SECRET("pdd_client_secret", "拼多多 ClientSecret");

    companion object {
        fun from(key: String): SettingsField? = entries.firstOrNull { it.key == key }
    }
}

/**
 * 设置页 ViewModel：管理 AI Key / 联盟凭证 / 关于 / 缓存清理。
 *
 * 凭证通过 [PreferenceRepository]（core-datastore）持久化；当前 DataStore 仅做明文存储，
 * 后续如需加密可在此处包装一层 EncryptedSharedPreferences 适配器，对调用方透明。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val dispatchers: DispatcherProvider,
    private val appInfoProvider: AppInfoProvider
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _editingFieldKey = MutableStateFlow<String?>(null)
    private val _editingInitialValue = MutableStateFlow("")
    private val _cache = MutableStateFlow(CacheInfo(sizeBytes = 0L))
    private val _toast = MutableStateFlow<String?>(null)

    /** 凭证数据流：把 9 个 Preference flow 合并为一个 [CredentialsChunk2]。 */
    private val credentialsFlow = combine(
        combine(
            preferenceRepository.deepseekKey,
            preferenceRepository.azureKey,
            preferenceRepository.groqKey,
            preferenceRepository.taobaoAppKey,
            preferenceRepository.taobaoSecret
        ) { deep, azure, groq, tbApp, tbSecret ->
            // 第一层：5 个 flow 合并得到 AI 三件 + 淘宝两件
            CredentialsChunk1(deep, azure, groq, tbApp, tbSecret)
        },
        combine(
            preferenceRepository.jdAppKey,
            preferenceRepository.jdSecret,
            preferenceRepository.pddClientId,
            preferenceRepository.pddClientSecret
        ) { jdApp, jdSecret, pddId, pddSecret ->
            // 第二层：4 个 flow 合并得到京东 + 拼多多
            JdPddChunk(jdApp, jdSecret, pddId, pddSecret)
        }
    ) { c1, c2 ->
        CredentialsChunk2(
            ai = AiSettings(
                deepSeekKey = CredentialField("DeepSeek API Key", c1.deep),
                azureKey = CredentialField("Azure TTS Key", c1.azure),
                groqKey = CredentialField("Groq API Key", c1.groq),
            ),
            alliance = AllianceSettings(
                taobaoAppKey = CredentialField("淘宝 AppKey", c1.tbApp),
                taobaoSecret = CredentialField("淘宝 AppSecret", c1.tbSecret),
                jdAppKey = CredentialField("京东 AppKey", c2.jdApp),
                jdSecret = CredentialField("京东 AppSecret", c2.jdSecret),
                pddClientId = CredentialField("拼多多 ClientId", c2.pddId),
                pddClientSecret = CredentialField("拼多多 ClientSecret", c2.pddSecret),
            )
        )
    }

    /** 聚合后的 UI 状态。 */
    val uiState: StateFlow<SettingsUiState> = combine(
        credentialsFlow,
        _isLoading,
        combine(_editingFieldKey, _editingInitialValue, _cache) { k, v, c -> Triple(k, v, c) },
        _toast,
        preferenceRepository.themeMode,
        flowOf(Unit)
    ) { creds, loading, editCache, toast, themeMode, _ ->
        val (editingKey, editingInit, cache) = editCache
        SettingsUiState(
            isLoading = loading,
            ai = creds.ai,
            alliance = creds.alliance,
            about = appInfoProvider.about(),
            cache = cache,
            themeMode = themeMode.ifBlank { "system" },
            editingFieldKey = editingKey,
            editingInitialValue = editingInit,
            toast = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(isLoading = true, about = appInfoProvider.about())
    )

    init {
        refresh()
    }

    /** 首次进入加载缓存大小并标记加载完成。 */
    fun refresh() {
        viewModelScope.launch {
            updateCacheSize()
            _isLoading.value = false
        }
    }

    /** 打开字段编辑对话框。 */
    fun startEdit(field: SettingsField) {
        val current = currentValueFor(field)
        _editingFieldKey.value = field.key
        _editingInitialValue.value = current
    }

    /** 取消编辑。 */
    fun cancelEdit() {
        _editingFieldKey.value = null
        _editingInitialValue.value = ""
    }

    /** 保存编辑后的值。 */
    fun saveEdit(value: String) {
        val key = _editingFieldKey.value ?: return
        val field = SettingsField.from(key) ?: return
        viewModelScope.launch {
            withContext(dispatchers.io) {
                when (field) {
                    SettingsField.DEEPSEEK_KEY -> preferenceRepository.setDeepseekKey(value.trim())
                    SettingsField.AZURE_KEY -> preferenceRepository.setAzureKey(value.trim())
                    SettingsField.GROQ_KEY -> preferenceRepository.setGroqKey(value.trim())
                    SettingsField.TAOBAO_APPKEY -> preferenceRepository.setTaobaoAppKey(value.trim())
                    SettingsField.TAOBAO_SECRET -> preferenceRepository.setTaobaoSecret(value.trim())
                    SettingsField.JD_APPKEY -> preferenceRepository.setJdAppKey(value.trim())
                    SettingsField.JD_SECRET -> preferenceRepository.setJdSecret(value.trim())
                    SettingsField.PDD_CLIENT_ID -> preferenceRepository.setPddClientId(value.trim())
                    SettingsField.PDD_CLIENT_SECRET -> preferenceRepository.setPddClientSecret(value.trim())
                }
            }
            _toast.value = "已保存"
            _editingFieldKey.value = null
            _editingInitialValue.value = ""
        }
    }

    /** 清理缓存：cacheDir + filesDir/cache 临时产物；不影响 filesDir/materials。 */
    fun clearCache() {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                val before = currentCacheSize()
                cleanCacheDir()
                cleanFilesCacheDir()
                val after = currentCacheSize()
                _cache.value = CacheInfo(sizeBytes = after, cleaned = true)
                _toast.value = "已清理 ${formatSize(before - after).ifBlank { "0B" }}"
            }
        }
    }

    /** 清除一次性 toast。 */
    fun consumeToast() {
        _toast.value = null
    }

    /** 切换主题模式：system（跟随系统）/ light（浅色）/ dark（深色）。 */
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                preferenceRepository.setThemeMode(mode)
            }
        }
    }

    // ===== 内部工具 =====

    private fun currentValueFor(field: SettingsField): String {
        val s = uiState.value
        return when (field) {
            SettingsField.DEEPSEEK_KEY -> s.ai.deepSeekKey.value
            SettingsField.AZURE_KEY -> s.ai.azureKey.value
            SettingsField.GROQ_KEY -> s.ai.groqKey.value
            SettingsField.TAOBAO_APPKEY -> s.alliance.taobaoAppKey.value
            SettingsField.TAOBAO_SECRET -> s.alliance.taobaoSecret.value
            SettingsField.JD_APPKEY -> s.alliance.jdAppKey.value
            SettingsField.JD_SECRET -> s.alliance.jdSecret.value
            SettingsField.PDD_CLIENT_ID -> s.alliance.pddClientId.value
            SettingsField.PDD_CLIENT_SECRET -> s.alliance.pddClientSecret.value
        }
    }

    private suspend fun updateCacheSize() {
        val size = withContext(dispatchers.io) { currentCacheSize() }
        _cache.value = _cache.value.copy(sizeBytes = size, cleaned = false)
    }

    private fun currentCacheSize(): Long {
        var total = 0L
        appInfoProvider.cacheDir.listFiles()?.forEach { total += dirSize(it) }
        appInfoProvider.filesCacheDir.listFiles()?.forEach { total += dirSize(it) }
        return total
    }

    private fun cleanCacheDir() {
        appInfoProvider.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun cleanFilesCacheDir() {
        appInfoProvider.filesCacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun dirSize(file: File): Long = when {
        file.isDirectory -> file.listFiles()?.sumOf { dirSize(it) } ?: 0L
        else -> file.length()
    }

    /** 字节数转人类可读字符串。 */
    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024.0 && unitIndex < units.lastIndex) {
            size /= 1024.0
            unitIndex++
        }
        return "%.1f%s".format(size, units[unitIndex])
    }

    /** 内部数据块：第一层 combine 的中间结果。 */
    private data class CredentialsChunk1(
        val deep: String,
        val azure: String,
        val groq: String,
        val tbApp: String,
        val tbSecret: String
    )

    /** 内部数据块：第二层 combine 的中间结果，仅含京东 + 拼多多凭证。 */
    private data class JdPddChunk(
        val jdApp: String,
        val jdSecret: String,
        val pddId: String,
        val pddSecret: String
    )

    /** 内部数据块：第二层 combine 的结果，包含完整的 AI/Alliance 设置。 */
    private data class CredentialsChunk2(
        val ai: AiSettings,
        val alliance: AllianceSettings
    )

    companion object {
        /** 便捷格式化缓存大小（供 UI 显示）。 */
        fun formatCacheSize(bytes: Long): String {
            if (bytes <= 0) return "0B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            var unitIndex = 0
            while (size >= 1024.0 && unitIndex < units.lastIndex) {
                size /= 1024.0
                unitIndex++
            }
            return "%.1f%s".format(size, units[unitIndex])
        }
    }
}
