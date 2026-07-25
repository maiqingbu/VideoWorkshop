package com.videoworkshop.core.media

import android.content.Context
import com.videoworkshop.core.common.AppConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地文件目录与命名管理工具。
 *
 * 在应用私有目录下统一管理 videos / images / audio / output / thumbnails 子目录，
 * 并提供基于时间戳的唯一文件名生成能力。
 */
object FileManager {

    /** 应用工作根目录（[Context.getFilesDir]/[AppConstants.APP_DIR_NAME]）。 */
    fun appRootDir(context: Context): File =
        File(context.filesDir, AppConstants.APP_DIR_NAME).ensureDir()

    fun videosDir(context: Context): File = subdir(context, AppConstants.SUBDIR_VIDEOS)
    fun imagesDir(context: Context): File = subdir(context, AppConstants.SUBDIR_IMAGES)
    fun audioDir(context: Context): File = subdir(context, AppConstants.SUBDIR_AUDIO)
    fun outputDir(context: Context): File = subdir(context, AppConstants.SUBDIR_OUTPUT)
    fun thumbnailsDir(context: Context): File = subdir(context, AppConstants.SUBDIR_THUMBNAILS)

    /** 一次性创建全部子目录。 */
    fun ensureAllDirs(context: Context) {
        videosDir(context)
        imagesDir(context)
        audioDir(context)
        outputDir(context)
        thumbnailsDir(context)
    }

    /** 生成视频文件名，默认 `VID_yyyyMMdd_HHmmss_SSS.mp4`。 */
    fun generateVideoName(prefix: String = "VID", extension: String = "mp4"): String =
        buildName(prefix, extension)

    /** 生成图片文件名，默认 `IMG_yyyyMMdd_HHmmss_SSS.jpg`。 */
    fun generateImageName(prefix: String = "IMG", extension: String = "jpg"): String =
        buildName(prefix, extension)

    /** 生成音频文件名，默认 `AUD_yyyyMMdd_HHmmss_SSS.m4a`。 */
    fun generateAudioName(prefix: String = "AUD", extension: String = "m4a"): String =
        buildName(prefix, extension)

    /**
     * 在指定目录下生成保证不重名的文件。
     */
    fun generateUniqueFile(dir: File, prefix: String, extension: String): File {
        val ext = extension.removePrefix(".")
        var file = File(dir, buildName(prefix, ext))
        var counter = 1
        while (file.exists()) {
            file = File(dir, "${prefix}_${timestamp()}_$counter.$ext")
            counter++
        }
        return file
    }

    private fun subdir(context: Context, name: String): File =
        File(appRootDir(context), name).ensureDir()

    private fun buildName(prefix: String, extension: String): String =
        "${prefix}_${timestamp()}.$extension"

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    private fun File.ensureDir(): File = apply {
        if (!exists()) mkdirs()
    }
}
