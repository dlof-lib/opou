package com.OPEN.OU.data.model

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    // صورة رمزية مخزّنة كـ Base64 (نفس نمط Post/User) — تُستخدم أولًا إن وُجدت
    val authorAvatarBase64: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
