package com.OPEN.OU.data.model

/**
 * يمثل مستخدم أوبو (OPOU) — لكل مستخدم "غرفة" (Room) خاصة به
 * وهي بمثابة الملف الشخصي، مع اسم مجتمع اختياري (مثال: "يوتيوبر").
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val communityName: String = "",   // مثل: يوتيوبر، مصمم، لاعب...
    val bio: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    // صور مخزّنة مباشرة كـ Base64 داخل Realtime Database (بدون Firebase Storage)
    val avatarBase64: String = "",
    val bannerBase64: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // إحصائيات الغرفة
    val paragraphsCount: Int = 0,     // عدد الفقرات (المنشورات)
    val tekersCount: Int = 0,         // عدد المتابعين (تيكرز)
    val tekingCount: Int = 0,         // عدد من يتابعهم المستخدم (تيكينغ)
    val shaabiyaScore: Long = 0L,     // مجموع نقاط الشعبية

    val verified: Boolean = false
)
// ملاحظة: جميع الحقول لها قيم افتراضية، لذلك يتوفر تلقائيًا مُنشئ بلا معاملات
// وهو ما يتطلبه Firebase Realtime Database لفك التسلسل (deserialization).
