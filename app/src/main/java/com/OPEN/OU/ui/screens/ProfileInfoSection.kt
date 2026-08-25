package com.OPEN.OU.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.User
import com.OPEN.OU.ui.theme.OpouAccentGreen

/** قسم "لمحة" + (غرفة خاصة أو السيرة الذاتية وروابط التواصل والأزرار المخصّصة). */
@Composable
internal fun ProfileInfoSection(
    user: User,
    isOwnProfile: Boolean,
    isLocked: Boolean,
    uriHandler: UriHandler,
    onEditRoom: () -> Unit
) {
    ProfileSectionCard(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.profile_overview_title)
    ) {
        Text(
            "${stringResource(R.string.profile_joined_as)} ${
                user.communityName.ifBlank { stringResource(R.string.official_member_title) }
            }",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(12.dp))

    if (isLocked) {
        ProfileSectionCard(icon = Icons.Filled.Lock, title = "غرفة خاصة") {
            Text(
                "هذه الغرفة خاصة — تابع (تيك) صاحبها لرؤية فقراته وسيرته الذاتية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // ── قسم "السيرة الذاتية" ───────────────────────────────────
    ProfileSectionCard(
        icon = Icons.Filled.Notes,
        title = stringResource(R.string.profile_bio_title),
        onClick = if (isOwnProfile && user.bio.isBlank()) onEditRoom else null
    ) {
        if (user.bio.isNotBlank()) {
            Text(user.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                stringResource(if (isOwnProfile) R.string.profile_bio_empty_self else R.string.profile_bio_empty_other),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }

    if (user.socialLinks.values.any { it.isNotBlank() }) {
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            user.socialLinks.filterValues { it.isNotBlank() }.forEach { (platform, url) ->
                AssistChip(
                    onClick = {
                        val normalized = if (url.startsWith("http")) url else "https://$url"
                        runCatching { uriHandler.openUri(normalized) }
                    },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = OpouAccentGreen, modifier = Modifier.size(14.dp)) },
                    label = { Text(platform.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    if (user.customButtons.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            user.customButtons.filter { it.label.isNotBlank() && it.url.isNotBlank() }.forEach { button ->
                OutlinedButton(
                    onClick = {
                        val normalized = if (button.url.startsWith("http")) button.url else "https://${button.url}"
                        runCatching { uriHandler.openUri(normalized) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(button.label) }
            }
        }
    }
}

/** زر الإجراء الرئيسي أسفل معلومات الحساب: تيك/إلغاء تيك لغرفة الآخرين، أو تعديل الغرفة لصاحبها. */
@Composable
internal fun ProfileActionButton(
    isOwnProfile: Boolean,
    canShowTekButton: Boolean,
    isTeking: Boolean,
    onToggleTek: () -> Unit,
    onEditRoom: () -> Unit
) {
    if (canShowTekButton) {
        if (isTeking) {
            OutlinedButton(onClick = onToggleTek, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إلغاء التيك")
            }
        } else {
            Button(
                onClick = onToggleTek,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_tek))
            }
        }
    } else if (isOwnProfile) {
        Button(
            onClick = onEditRoom,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.profile_edit_profile))
        }
    }
}
