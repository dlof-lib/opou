package com.OPEN.OU.data.model

/** نوع التفاعل على الفقرة: إعجاب بنجمة ⭐ أو عدم إعجاب بقلب مكسور 💔 */
enum class ReactionType { LIKE, DISLIKE, NONE }

data class Reaction(
    val userId: String = "",
    val postId: String = "",
    val type: ReactionType = ReactionType.NONE,
    val timestamp: Long = System.currentTimeMillis()
)

/** علاقة متابعة: "تيكينغ" (Teking) هو مستخدم يتابع "تيكر" (Teker) */
data class TekRelation(
    val tekerId: String = "",   // المتابَع
    val tekingId: String = "",  // المتابِع
    val timestamp: Long = System.currentTimeMillis()
)
