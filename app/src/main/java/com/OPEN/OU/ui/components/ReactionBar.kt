package com.OPEN.OU.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.theme.OpouAccentBlue
import com.OPEN.OU.ui.theme.OpouAccentGreen
import com.OPEN.OU.ui.theme.OpouBrokenHeart
import com.OPEN.OU.ui.theme.OpouStar

/** شريط تفاعلات الفقرة بأيقونات حقيقية: نجمة (إعجاب) — قلب مكسور (لم يعجبني) — تعليق — تيك (إعادة نشر) */
@Composable
fun ReactionBar(
    likesCount: Int,
    dislikesCount: Int,
    commentsCount: Int,
    teksCount: Int,
    currentReaction: ReactionType,
    onReact: (ReactionType) -> Unit,
    onComment: () -> Unit,
    onTek: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconReactionButton(
            icon = Icons.Filled.Star,
            contentDescription = stringResource(R.string.reaction_like),
            count = likesCount,
            selected = currentReaction == ReactionType.LIKE,
            activeColor = OpouStar,
            onClick = { onReact(if (currentReaction == ReactionType.LIKE) ReactionType.NONE else ReactionType.LIKE) }
        )
        IconReactionButton(
            icon = Icons.Filled.HeartBroken,
            contentDescription = stringResource(R.string.reaction_dislike),
            count = dislikesCount,
            selected = currentReaction == ReactionType.DISLIKE,
            activeColor = OpouBrokenHeart,
            onClick = { onReact(if (currentReaction == ReactionType.DISLIKE) ReactionType.NONE else ReactionType.DISLIKE) }
        )
        IconAction(
            icon = Icons.Filled.ChatBubbleOutline,
            contentDescription = stringResource(R.string.action_comment),
            count = commentsCount,
            tint = OpouAccentBlue,
            onClick = onComment
        )
        IconAction(
            icon = Icons.Filled.Repeat,
            contentDescription = stringResource(R.string.action_tek),
            count = teksCount,
            tint = OpouAccentGreen,
            onClick = onTek
        )
    }
}

@Composable
private fun IconReactionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    count: Int,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reactionScale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp).scale(scale)
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    count: Int,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
