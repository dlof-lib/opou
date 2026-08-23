package com.OPEN.OU.data.model

/**
 * "فقرة" — المنشور النصي في أوبو. حرية التعبير الكاملة: نص مفتوح بلا قيود شكل.
 * يدعم أيضًا كونه "تيك" (إعادة نشر) لفقرة أصلية عبر originalPostId/originalAuthorId.
 */
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val authorAvatarBase64: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // صورة اختيارية داخل الفقرة، مخزّنة كـ Base64 مباشرة في Realtime Database
    val imageBase64: String = "",

    // تفاعلات: ⭐ إعجاب و 💔 عدم إعجاب
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val commentsCount: Int = 0,
    val teksCount: Int = 0,           // عدد مرات إعادة النشر (التيك)

    // نقاط الشعبية المحسوبة من مجموع التفاعلات (تُستخدم في تبويب "الشعبيات")
    val shaabiyaScore: Long = 0L,

    // في حال كانت هذه الفقرة "تيك" لفقرة أصلية
    val isTek: Boolean = false,
    val originalPostId: String? = null,
    val originalAuthorId: String? = null,
    val originalAuthorUsername: String? = null
)
