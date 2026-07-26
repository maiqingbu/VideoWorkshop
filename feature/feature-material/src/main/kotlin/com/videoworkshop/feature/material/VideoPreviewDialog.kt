package com.videoworkshop.feature.material

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 视频预览对话框。
 *
 * 基于 ExoPlayer + PlayerView 全屏播放，内置控制器支持播放/暂停/进度拖动。
 * 关闭时通过 [DisposableEffect] 自动释放 [ExoPlayer]，避免内存泄漏。
 *
 * @param videoPath 视频本地路径或 content:// URI 字符串
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewDialog(
    videoPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoPath)))
            prepare()
            playWhenReady = true
        }
    }

    // 退出时释放播放器，避免内存泄漏
    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 播放器视图（自带播放/暂停/进度拖动控制器）
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .aspectRatio(1.778f),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        // 锁定控制器可见
                        controllerAutoShow = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                }
            )

            // 顶部关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }

            // 视频路径标题
            Text(
                text = videoPath.substringAfterLast('/').ifBlank { "视频预览" },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
