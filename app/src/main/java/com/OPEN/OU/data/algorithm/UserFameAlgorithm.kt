package com.OPEN.OU.data.algorithm

import com.OPEN.OU.data.model.User
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * خوارزمية "شهرة المستخدم" — تملأ حقل [User.shaabiyaScore] الذي كان موجودًا في
 * النموذج من البداية كـ"مجموع نقاط الشعبية" لكنه لم يُحسب فعليًا في أي مكان
 * بالمشروع (يبقى 0 دائمًا)، بالإضافة إلى مقياس ثانٍ منفصل تمامًا: [computeRisingScore].
 *
 * لماذا مقياسان لا مقياس واحد؟
 * ----------------------------
 * مقياس شهرة "مدى الحياة" وحده (متابعون + تفاعل تراكمي) يعني عمليًا أن أول
 * مستخدمين انضموا للتطبيق سيتصدّروا أي قائمة "الأكثر شهرة" إلى الأبد — لأن
 * عندهم وقتًا أطول لتجميع الأرقام، بغض النظر عن مدى نشاطهم الحالي. هذا يقتل
 * فرصة أي حساب جديد أو صاعد في الظهور، وهو عكس الهدف من "مساعدة على الشهرة".
 *
 * لذلك نحسب:
 *  1) [computeScore] — شهرة "الكل"، تُستخدم لقائمة "الأكثر شهرة"، بتخامد
 *     لوغاريتمي (عائد متناقص) حتى لا يُغرق حساب ضخم كل الحسابات الأصغر.
 *  2) [computeRisingScore] — سرعة النمو (تفاعل/يوم منذ الانضمام)، تُستخدم
 *     لقائمة منفصلة "النجوم الصاعدة"، تمنح حسابًا عمره أسبوع وينمو بسرعة
 *     فرصة ظهور حقيقية حتى لو أرقامه المطلقة ما زالت أصغر من حساب قديم راكد.
 */
object UserFameAlgorithm {

    // ===== شهرة "مدى الحياة" =====
    private const val FOLLOWER_LOG_WEIGHT = 30.0
    private const val ENGAGEMENT_LOG_WEIGHT = 20.0
    /** مكافأة ثابتة للحسابات الموثّقة (Verified) — إشارة جودة/موثوقية إضافية عن الأرقام الخام. */
    private const val VERIFIED_BONUS = 40.0

    /**
     * شهرة "مدى الحياة": مزيج لوغاريتمي من عدد المتابعين (تيكرز) وإجمالي
     * التفاعل التراكمي الذي جمعته فقرات المستخدم، مع مكافأة توثيق.
     * اللوغاريتم يعني أن الانتقال من 10 إلى 100 متابع يزيد النقاط بقدر
     * الانتقال من 100,000 إلى 1,000,000 — عائد متناقص يمنع الهيمنة الأبدية.
     */
    fun computeScore(
        tekersCount: Int,
        totalEngagementScore: Long,
        verified: Boolean
    ): Long {
        val followerTerm = ln(max(tekersCount.toDouble(), 0.0) + 1.0) * FOLLOWER_LOG_WEIGHT
        val engagementTerm = ln(max(totalEngagementScore.toDouble(), 0.0) + 1.0) * ENGAGEMENT_LOG_WEIGHT
        val verifiedTerm = if (verified) VERIFIED_BONUS else 0.0
        return (followerTerm + engagementTerm + verifiedTerm).roundToLong()
    }

    /** نسخة مختصرة تأخذ [User] مباشرة. */
    fun computeScore(user: User): Long =
        computeScore(user.tekersCount, user.totalEngagementScore, user.verified)

    // ===== "النجوم الصاعدة" (Rising Stars) =====
    /**
     * ثابت تنعيم بايزي (Bayesian smoothing) على البسط والمقام معًا — يمنع حسابًا
     * عمره ساعة واحدة وحقق تفاعلاً عاليًا من عيّنة صغيرة (حظ فقرة واحدة فيروسية)
     * من الظهور فورًا كـ"الأسرع نموًا على الإطلاق". بدون هذا التنعيم، القسمة على
     * عمر شبه صفري تُنتج نسبًا ضخمة وغير موثوقة إحصائيًا (small-sample overconfidence).
     */
    private const val SMOOTHING_ENGAGEMENT = 15.0
    private const val SMOOTHING_DAYS = 2.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    /**
     * سرعة النمو: متوسط التفاعل اليومي منذ الانضمام (مع تنعيم بايزي لعينة
     * صغيرة)، مُعاد إلى مقياس صحيح (×1000) لتخزينه/فرزه كـ Long.
     */
    fun computeRisingScore(
        totalEngagementScore: Long,
        createdAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val ageDays = max((nowMillis - createdAtMillis) / MILLIS_PER_DAY, 0.0)
        val smoothedRate =
            (totalEngagementScore.toDouble() + SMOOTHING_ENGAGEMENT) / (ageDays + SMOOTHING_DAYS)
        return (smoothedRate * 1000.0).roundToLong()
    }

    /** نسخة مختصرة تأخذ [User] مباشرة. */
    fun computeRisingScore(user: User, nowMillis: Long = System.currentTimeMillis()): Long =
        computeRisingScore(user.totalEngagementScore, user.createdAt, nowMillis)
}
