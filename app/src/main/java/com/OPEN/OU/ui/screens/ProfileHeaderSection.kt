package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.model.User
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.GradientText
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.ui.theme.OpouBrandGradient
import com.OPEN.OU.util.ImageCodec

/**
 * قسم رأس صفحة الحساب: البانر، الصورة الرمزية المتراكبة (مع زر التغيير لصاحب
 * الغرفة)، الاسم مع شارة التوثيق، شارة اسم المجتمع، تصنيفات الغرفة، ثم بطاقة
 * الإحصائيات (فقرات/متابعون/متابَعون).
 */
@Composable
internal fun ProfileHeaderSection(
    user: User,
    isOwnProfile: Boolean,
    avatarError: String?,
    onAvatarErrorChange: (String?) -> Unit,
    onAvatarClick: () -> Unit,
    onBannerClick: () -> Unit,
    onAvatarPicked: (String) -> Unit
) {
    // ── البانر ─────────────────────────────────────────────────────
    val hasBannerImage = user.bannerBase64.isNotBlank() || user.bannerUrl.isNotBlank()
    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .then(if (hasBannerImage) Modifier.clickable(onClick = onBannerClick) else Modifier)
    ) {
        when {
            user.bannerBase64.isNotBlank() -> Base64Image(
                base64 = user.bannerBase64,
                modifier = Modifier.fillMaxSize(),
                cornerRadiusDp = 0
            )
            user.bannerUrl.isNotBlank() -> AsyncImage(
                model = user.bannerUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            else -> Box(Modifier.fillMaxSize().background(OpouBrandGradient))
        }
        // تظليل خفيف أسفل البانر حتى تبرز الصورة الرمزية والاسم فوقه بوضوح
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(56.dp)
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)))
                )
        )
    }

    // ── الصورة الرمزية + الاسم + التصنيف ─────────────────────────────
    Column(Modifier.padding(horizontal = 16.dp)) {
        Box(Modifier.offset(y = (-32).dp)) {
            val hasAvatarImage = user.avatarBase64.isNotBlank() || user.avatarUrl.isNotBlank()
            val avatarModifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .then(if (hasAvatarImage) Modifier.clickable(onClick = onAvatarClick) else Modifier)

            if (user.avatarBase64.isNotBlank()) {
                Base64Image(base64 = user.avatarBase64, modifier = avatarModifier, cornerRadiusDp = 20)
            } else {
                AsyncImage(
                    model = user.avatarUrl.ifBlank { null },
                    contentDescription = null,
                    modifier = avatarModifier.background(Color(0xFF0B7A4A))
                )
            }

            if (isOwnProfile) {
                ImagePickerButton(
                    profile = ImageCodec.ImageProfile.AVATAR,
                    onImageReady = { encoded -> onAvatarPicked(encoded.base64) },
                    onError = { onAvatarErrorChange(it) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        avatarError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // إصلاح: عند خلو اسم المستخدم (مثلاً حساب لم يُكمل إعداد اسم المستخدم بعد)
            // كان النص يظهر فارغًا تمامًا بلا أي أثر. الآن نعرض بديلاً واضحًا دائمًا،
            // ولصاحب الحساب نفسه نجعله قابلاً للنقر لإرشاده لإكمال إعداد الاسم.
            if (user.username.isNotBlank()) {
                GradientText(text = user.username, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    if (isOwnProfile) "أضف اسم مستخدم" else "بدون اسم مستخدم",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.verified) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "موثّق",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        AssistChip(
            onClick = {},
            leadingIcon = { Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text(user.communityName.ifBlank { stringResource(R.string.official_member_title) }) }
        )

        if (user.categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.categories.take(4).forEach { category ->
                    AssistChip(onClick = {}, label = { Text(category, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        ProfileStatsCard(user)

        Spacer(Modifier.height(16.dp))
    }
}

/** بطاقة الإحصائيات الثلاث: عدد الفقرات، المتابعون (تيكرز)، والمتابَعون (تيكينغ). */
@Composable
private fun ProfileStatsCard(user: User) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(user.paragraphsCount, stringResource(R.string.profile_paragraphs))
            VerticalDivider()
            StatColumn(user.tekersCount, stringResource(R.string.profile_followers))
            VerticalDivider()
            StatColumn(user.tekingCount, stringResource(R.string.profile_following))
        }
    }
}
