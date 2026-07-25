package com.videoworkshop.data.publish

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.PublishTarget
import com.videoworkshop.domain.repository.PublishRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [PublishRepository] 实现：通过系统分享机制将内容发布到目标平台。
 *
 * 发布流程：
 * 1. 复制商品推广链接到剪贴板（便于用户在目标平台粘贴）
 * 2. 通过 FileProvider 生成 content:// URI
 * 3. 构造定向 Share Intent（ACTION_SEND + setPackage）
 * 4. 拉起目标平台 App（抖音 / 快手 / 小红书）
 *
 * 若目标平台未安装（ActivityNotFoundException），回退到 [PublishFallback.saveToGallery]
 * 将文件保存到系统相册。
 *
 * @param context           应用上下文
 * @param clipboardHelper   剪贴板工具
 * @param fileProviderHelper FileProvider URI 生成工具
 * @param dispatchers       协程调度器
 */
class ShareIntentPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clipboardHelper: ClipboardHelper,
    private val fileProviderHelper: FileProviderHelper,
    private val dispatchers: DispatcherProvider
) : PublishRepository {

    override suspend fun publish(
        filePath: String,
        type: ContentType,
        target: PublishTarget,
        title: String,
        goodsLink: String?
    ): Boolean = withContext(dispatchers.io) {
        // 1. 复制商品链接到剪贴板
        if (!goodsLink.isNullOrBlank()) {
            clipboardHelper.copyToClipboard(goodsLink)
        }

        // 2. 通过 FileProvider 生成可分享 URI
        val uri = try {
            fileProviderHelper.getShareUri(filePath)
        } catch (e: PublishException) {
            return@withContext false
        }

        // 3. 映射目标平台包名
        val targetPackage = PublishTargetMapper.map(target)

        // 4. 构造 Share Intent
        val shareIntent = ShareIntentBuilder.buildShareIntent(
            uri = uri,
            targetPackage = targetPackage,
            title = title,
            type = type
        )
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // 5. 拉起目标平台
        return@withContext try {
            context.startActivity(shareIntent)
            true
        } catch (e: ActivityNotFoundException) {
            // 目标平台未安装，回退到保存相册
            PublishFallback.saveToGallery(context, filePath, type)
        } catch (e: SecurityException) {
            false
        }
    }
}
