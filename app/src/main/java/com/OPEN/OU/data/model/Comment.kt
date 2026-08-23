package com.OPEN.OU.data.model

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
