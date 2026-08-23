package com.OPEN.OU.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    currentReaction: ReactionType,
    onReact: (ReactionType) -> Unit,
    onComment: () -> Unit,
    onTek: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            if (post.isTek) {
                Text(
                    text = "🔁 أعاد ${post.authorUsername} النشر عن ${post.originalAuthorUsername}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.authorAvatarBase64.isNotBlank()) {
                    com.OPEN.OU.ui.components.Base64Image(
                        base64 = post.authorAvatarBase64,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onOpenProfile(post.authorId) }
                    )
                } else {
                    AsyncImage(
                        model = post.authorAvatarUrl.ifBlank { null },
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B7A4A))
                            .clickable { onOpenProfile(post.authorId) }
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(post.authorUsername, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = formatTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (post.content.isNotBlank()) {
                Text(post.content, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
            }
            if (post.imageBase64.isNotBlank()) {
                com.OPEN.OU.ui.components.Base64Image(
                    base64 = post.imageBase64,
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            ReactionBar(
                likesCount = post.likesCount,
                dislikesCount = post.dislikesCount,
                commentsCount = post.commentsCount,
                teksCount = post.teksCount,
                currentReaction = currentReaction,
                onReact = onReact,
                onComment = onComment,
                onTek = onTek
            )
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy - HH:mm", Locale("ar")).format(Date(millis))
