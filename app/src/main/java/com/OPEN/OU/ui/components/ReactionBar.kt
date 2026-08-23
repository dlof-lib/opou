package com.OPEN.OU.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.theme.OpouBrokenHeart
import com.OPEN.OU.ui.theme.OpouStar

/** شريط تفاعلات الفقرة: ⭐ إعجاب — 💔 عدم إعجاب — تعليق — تيك (إعادة نشر) */
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
        EmojiReactionButton(
            emoji = "⭐",
            label = stringResource(R.string.reaction_like),
            count = likesCount,
            selected = currentReaction == ReactionType.LIKE,
            activeColor = OpouStar,
            onClick = { onReact(if (currentReaction == ReactionType.LIKE) ReactionType.NONE else ReactionType.LIKE) }
        )
        EmojiReactionButton(
            emoji = "💔",
            label = stringResource(R.string.reaction_dislike),
            count = dislikesCount,
            selected = currentReaction == ReactionType.DISLIKE,
            activeColor = OpouBrokenHeart,
            onClick = { onReact(if (currentReaction == ReactionType.DISLIKE) ReactionType.NONE else ReactionType.DISLIKE) }
        )
        TextIconAction(icon = "💬", count = commentsCount, onClick = onComment)
        TextIconAction(icon = "🔁", count = teksCount, onClick = onTek)
    }
}

@Composable
private fun EmojiReactionButton(
    emoji: String,
    label: String,
    count: Int,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reactionScale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp)
    ) {
        Text(text = emoji, modifier = Modifier.scale(scale))
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (count > 0) count.toString() else "",
            color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun TextIconAction(icon: String, count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp)
    ) {
        Text(text = icon)
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (count > 0) count.toString() else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
