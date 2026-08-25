package com.OPEN.OU.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.OPEN.OU.data.model.User
import com.OPEN.OU.ui.components.ImageViewerDialog

/** حوارات صفحة الحساب: معاينة الصورة الرمزية/البانر بحجم كامل، وتأكيد الحظر. */
@Composable
internal fun ProfileDialogs(
    room: User?,
    showAvatarViewer: Boolean,
    onDismissAvatarViewer: () -> Unit,
    showBannerViewer: Boolean,
    onDismissBannerViewer: () -> Unit,
    showBlockConfirm: Boolean,
    onDismissBlockConfirm: () -> Unit,
    onConfirmBlock: () -> Unit
) {
    if (showAvatarViewer) {
        ImageViewerDialog(
            base64 = room?.avatarBase64,
            imageUrl = room?.avatarUrl,
            onDismiss = onDismissAvatarViewer
        )
    }

    if (showBannerViewer) {
        ImageViewerDialog(
            base64 = room?.bannerBase64,
            imageUrl = room?.bannerUrl,
            onDismiss = onDismissBannerViewer
        )
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = onDismissBlockConfirm,
            title = { Text("حظر ${room?.username.orEmpty()}", fontWeight = FontWeight.Bold) },
            text = { Text("لن يتمكن هذا المستخدم من رؤية فقراتك أو التفاعل معك، والعكس صحيح. يمكنك إلغاء الحظر لاحقًا.") },
            confirmButton = {
                TextButton(onClick = onConfirmBlock) { Text("حظر", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismissBlockConfirm) { Text("إلغاء") } }
        )
    }
}
