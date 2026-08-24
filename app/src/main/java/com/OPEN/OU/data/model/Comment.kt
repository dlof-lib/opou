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
    val createdAt: Long = System.currentTimeMillis(),

    // عدد الإعجابات على التعليق (⭐)
    val likesCount: Int = 0,

    // ===== الرد على تعليق (خيط تعليقات بمستوى واحد) =====
    /** معرّف التعليق الأب إن كان هذا التعليق ردًا على تعليق آخر، فارغ إن لم يكن ردًا */
    val parentCommentId: String = "",
    /** اسم صاحب التعليق الذي تم الرد عليه (يُعرض كـ "ردًا على @اسم") */
    val replyToUsername: String = ""
) {
    val isReply: Boolean get() = parentCommentId.isNotBlank()
}
