package com.OPEN.OU.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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

/**
 * شريط تفاعلات الفقرة: أربعة أزرار موزّعة بتباعد متساوٍ — نجمة (إعجاب)،
 * قلب مكسور (لم يعجبني)، تعليق، وتيك (إعادة نشر). كل زر يحمل خلفية
 * "كبسولة" لطيفة عند التفعيل مع نبضة حركية خفيفة، لإحساس أكثر رقيًا
 * من مجرّد أيقونة ورقم عاريين.
 */
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
        IconReactionButton(
            icon = Icons.Filled.ChatBubbleOutline,
            contentDescription = stringResource(R.string.action_comment),
            count = commentsCount,
            selected = false,
            activeColor = OpouAccentBlue,
            onClick = onComment
        )
        IconReactionButton(
            icon = Icons.Filled.Repeat,
            contentDescription = stringResource(R.string.action_tek),
            count = teksCount,
            selected = false,
            activeColor = OpouAccentGreen,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reactionScale"
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent)
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
            tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp).scale(scale)
        )
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = count.toString(),
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        }
    }
}
