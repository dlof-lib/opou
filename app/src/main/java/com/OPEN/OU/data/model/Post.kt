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
    val originalAuthorUsername: String? = null,

    // ===== تنسيق الفقرة (Paragraph styling) =====
    /** لون خلفية بطاقة الفقرة كاملة، بصيغة Hex مثل "#0B7A4A"، فارغ = بلا لون (افتراضي السطح) */
    val backgroundColor: String = "",
    /** إيموجي الفقرة — ميزة قيد التطوير، القيم المسموحة حاليًا محدودة في ParagraphEmoji.AVAILABLE */
    val emoji: String = "",
    /** لون نص المحتوى، بصيغة Hex، فارغ = اللون الافتراضي */
    val textColor: String = "",
    /** تعريض النص (Bold) */
    val textBold: Boolean = false,
    /** تسطير النص (Underline) */
    val textUnderline: Boolean = false,
    /** لون خلفية النص نفسه (تظليل)، بصيغة Hex، فارغ = بلا تظليل */
    val textBackgroundColor: String = "",
    /** روابط مرفقة مع الفقرة (تُعرض كعناصر قابلة للنقر أسفل المحتوى) */
    val links: List<String> = emptyList(),
    /** كود HTML قصير مخصّص (مجموعة وسوم محدودة وآمنة فقط، تُعرض داخل التطبيق وليس عبر WebView) */
    val customHtml: String = "",

    // ===== خصوصية ووقت نشر الفقرة =====
    /** مستوى الخصوصية: PUBLIC / PRIVATE / LIMITED / CUSTOM — راجع ParagraphPrivacy */
    val privacy: String = "PUBLIC",
    /** قائمة معرّفات المستخدمين المسموح لهم بالمشاهدة عند privacy = CUSTOM */
    val allowedViewerIds: List<String> = emptyList(),
    /** إن كانت أكبر من الوقت الحالي، تُعتبر الفقرة "مجدولة" ولا تظهر في التغذية حتى يحين موعدها */
    val scheduledAt: Long? = null,

    /** فقرة مثبّتة أعلى غرفة صاحبها — راجع User.pinnedPostId (تحديث واحد يبقيهما متطابقين) */
    val isPinned: Boolean = false,

    // ===== "الرد بفقرة" — نشر فقرة جديدة اقتباسًا عن تعليق (راجع CommentsSheet.onQuoteAsParagraph) =====
    /** معرّف التعليق المقتبَس، فارغ إن لم تكن هذه الفقرة ردًا على تعليق */
    val replyCommentId: String = "",
    /** معرّف صاحب التعليق المقتبَس */
    val replyCommentAuthorId: String = "",
    /** اسم صاحب التعليق المقتبَس (يُعرض كبطاقة اقتباس أعلى الفقرة) */
    val replyCommentAuthorUsername: String = "",
    /** نص التعليق المقتبَس وقت الاقتباس (نسخة ثابتة، لا تتغيّر إن عُدِّل التعليق الأصلي لاحقًا) */
    val replyCommentContent: String = "",

    // ===== سلسلة الفقرات (Thread) — فقرات مرتبطة بالترتيب يكتبها نفس المستخدم كموضوع واحد متسلسل =====
    /** معرّف السلسلة — يساوي postId أول فقرة في السلسلة. فارغ = هذه الفقرة ليست جزءًا من أي سلسلة. */
    val threadId: String = "",
    /** معرّف الفقرة السابقة مباشرة في نفس السلسلة، فارغة إن كانت هذه أول فقرة في السلسلة */
    val threadPreviousPostId: String = "",
    /** ترتيب هذه الفقرة داخل السلسلة، بدءًا من 1 لأول فقرة */
    val threadPosition: Int = 0
) {
    /** هل الفقرة لا تزال بانتظار موعد نشرها المجدول؟ */
    fun isScheduledForFuture(nowMillis: Long = System.currentTimeMillis()): Boolean =
        scheduledAt != null && scheduledAt > nowMillis

    /** هل هذه الفقرة جزء من سلسلة فقرات؟ */
    val isThreadPost: Boolean get() = threadId.isNotBlank()

    /** هل هذه أول فقرة في سلسلتها (رأس السلسلة)؟ */
    val isThreadStart: Boolean get() = isThreadPost && threadPreviousPostId.isBlank()
}
