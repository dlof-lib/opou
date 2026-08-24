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
    val shaabiyaScore: Long = 0L,     // مجموع نقاط الشعبية — يُحسب عبر UserFameAlgorithm (راجع توثيقه)
    /** مجموع وزن كل التفاعلات (إعجاب/تيك/تعليق) التي جمعتها كل فقرات هذا المستخدم عبر الزمن —
     *  رقم تراكمي "مدى الحياة" لا يتناقص أبدًا (بعكس shaabiyaScore على الفقرة الواحدة الذي
     *  يتخامد مع الوقت)، ويُستخدم كمُدخل خام لحساب شهرة المستخدم عبر UserFameAlgorithm. */
    val totalEngagementScore: Long = 0L,

    val verified: Boolean = false,

    // رمز إشعارات FCM الحالي للمستخدم — يُستخدم من network/PhpApiClient لإرسال إشعار عبر notify.php
    val fcmToken: String = "",
    // لغة واجهة المستخدم المفضّلة، تُزامن مع util/LanguagePrefs المحلي (ar / en)
    val language: String = "ar",

    // ===== ميزات الحساب =====
    /** حالة الحساب: ACTIVE / DEACTIVATED — راجع AccountStatus */
    val accountStatus: String = "ACTIVE",
    /** تفعيل التحقق بخطوتين (رمز PIN إضافي بعد كلمة المرور) — العلم فقط هنا لأنه غير حسّاس؛
     *  بصمة PIN نفسها مخزّنة في عقدة منفصلة محمية /userSecrets/{uid} وليس هنا، لأن /users/{uid}
     *  مقروءة للجميع (لعرض الغرف العامة) ولا يجب أن تكون بصمة PIN قابلة للقراءة من أي زائر. */
    val twoFactorEnabled: Boolean = false,
    /** تصنيفات إضافية متعددة للغرفة (بجانب اسم المجتمع الأساسي) */
    val categories: List<String> = emptyList(),
    /** روابط التواصل الاجتماعي: المفتاح = اسم المنصة (instagram/x/youtube/tiktok/snapchat/website)، القيمة = الرابط */
    val socialLinks: Map<String, String> = emptyMap(),
    /** أزرار مخصّصة يضيفها صاحب الغرفة (تسمية + رابط) */
    val customButtons: List<CustomButton> = emptyList(),
    /** معرّف الفقرة المثبّتة أعلى قائمة فقرات الغرفة، فارغ = لا يوجد تثبيت */
    val pinnedPostId: String = "",

    // ===== ميزات الخصوصية =====
    /** غرفة خاصة: فقراتها ومعلوماتها الكاملة لا تظهر إلا لصاحبها ومن يتابعهم (تيكرز مقبولون) */
    val isPrivateRoom: Boolean = false,
    /** إخفاء حالة "آخر ظهور" عن الآخرين */
    val hideLastSeen: Boolean = false,
    /** آخر وقت نشاط مسجَّل (ملّي ثانية) — يُستخدم لعرض "آخر ظهور" إن لم تكن مخفيّة */
    val lastSeenAt: Long = 0L,
    /** من يُسمح له بالتعليق على فقرات هذا المستخدم: EVERYONE / TEKERS / NOBODY */
    val whoCanComment: String = "EVERYONE"
)
// ملاحظة: جميع الحقول لها قيم افتراضية، لذلك يتوفر تلقائيًا مُنشئ بلا معاملات
// وهو ما يتطلبه Firebase Realtime Database لفك التسلسل (deserialization).
