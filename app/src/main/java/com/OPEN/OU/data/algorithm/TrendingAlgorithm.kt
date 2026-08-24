package com.OPEN.OU.data.algorithm

import com.OPEN.OU.data.model.Post
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * خوارزمية "الشعبيات" (Trending / Hot Ranking) — النسخة الثانية.
 *
 * لماذا التغيير عن الجمع الخطي البسيط (likes*3 + teks*5 + comments - dislikes)؟
 * ------------------------------------------------------------------------
 * 1) لا يوجد فيها أي بُعد زمني: فقرة حصلت على 1000 إعجاب قبل شهر تبقى أعلى
 *    الشعبيات إلى الأبد، حتى لو ماتت الحركة عليها تمامًا، وتحجب أي فقرة
 *    جديدة نشطة الآن مهما بلغ تفاعلها.
 * 2) نمو خطي بلا تباطؤ: الفرق بين 10 إعجابات و110 إعجاب (+100) يساوي
 *    تمامًا الفرق بين 10000 و10100 — رغم أن +100 على فقرة صغيرة حدث
 *    أهم بكثير (يدل على انفجار حقيقي) من +100 على فقرة ضخمة أصلًا.
 * 3) لا حماية عملية من "التصويت السلبي المنظّم" (dislike brigading):
 *    وزن عدم الإعجاب ضعيف جدًا مقارنة بالإعجاب.
 *
 * الحل: صيغة على نمط خوارزمية Reddit "Hot" — تجمع بين:
 *   - لوغاريتم حجم التفاعل (عائد متناقص: أول 10 تفاعلات تُحدث فرقًا أكبر
 *     من الـ10 تفاعلات رقم 10000)، مع إشارة (+/-) حسب اتجاه التفاعل الصافي.
 *   - حد زمني يتزايد خطيًا مع وقت النشر (بالثواني منذ epoch مقسومة على
 *     ثابت تخميد) — وهذا هو الجزء الأهم عمليًا: بما أن Realtime Database
 *     تخزّن قيمة ثابتة لا تُعاد حسابها تلقائيًا مع مرور الوقت، فإن الحد
 *     الزمني يضمن أن أي فقرة *جديدة* تُنشر الآن ستُخزَّن بقيمة أساس أعلى
 *     تلقائيًا من فقرة قديمة — أي أن السلوك الصحيح (الفقرات الحديثة تعلو
 *     تدريجيًا) يتحقق *بدون* الحاجة لإعادة حساب أي فقرة قديمة أو تشغيل Job
 *     دوري في الخلفية.
 *
 * النتيجة: فقرة بتفاعل قوي وحديث تتصدّر، ثم تتراجع تدريجيًا وبسلاسة (بدل
 * الاختفاء الفجائي) كلما تقادمت وظهرت فقرات أحدث بنفس مستوى التفاعل أو أعلى.
 */
object TrendingAlgorithm {

    /** ثابت التخميد الزمني بالثواني: كل 12.5 ساعة تمر تضيف نقطة كاملة لحد الأساس. */
    private const val TIME_DECAY_SECONDS = 45_000.0

    /** دقة التخزين: نحوّل القيمة العشرية إلى Long بضربها في هذا العامل لحفظ 4 خانات عشرية. */
    private const val FIXED_POINT_SCALE = 10_000.0

    /** أوزان أنواع التفاعل — التيك (إعادة النشر) أقوى إشارة انتشار، فالإعجاب، فالتعليق، وعدم الإعجاب يعاقب بقوة.
     *  عامّة (وليست private) لأن [com.OPEN.OU.data.repository.PostRepository] و[UserFameAlgorithm]
     *  يحتاجانها أيضًا لحساب "رصيد شهرة" صاحب الفقرة من نفس مصدر الحقيقة — بدل تكرار
     *  الأرقام في أكثر من مكان واحتمال أن تتباعد بمرور الوقت. */
    const val WEIGHT_LIKE = 3
    const val WEIGHT_TEK = 5
    const val WEIGHT_COMMENT = 2
    const val WEIGHT_DISLIKE = 4

    /**
     * يحسب "الوزن الصافي" الخام للتفاعل (قبل اللوغاريتم)، تُستخدم أيضًا لعرض
     * تفسير مبسّط أو لأغراض تصحيح الأخطاء.
     */
    fun rawEngagement(likes: Int, teks: Int, comments: Int, dislikes: Int): Long =
        (likes.toLong() * WEIGHT_LIKE) +
            (teks.toLong() * WEIGHT_TEK) +
            (comments.toLong() * WEIGHT_COMMENT) -
            (dislikes.toLong() * WEIGHT_DISLIKE)

    /**
     * يحسب نقاط الشعبية (shaabiyaScore) النهائية الجاهزة للتخزين في Firebase،
     * كقيمة Long ثابتة النقطة (fixed-point) تحافظ على ترتيب صحيح عند
     * `orderByChild`.
     *
     * @param createdAtMillis وقت نشر الفقرة (لحساب الحد الزمني)
     * @param nowMillis الوقت الحالي — معامل اختياري لتسهيل الاختبار
     */
    fun computeScore(
        likes: Int,
        teks: Int,
        comments: Int,
        dislikes: Int,
        createdAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val engagement = rawEngagement(likes, teks, comments, dislikes)
        val sign = when {
            engagement > 0 -> 1.0
            engagement < 0 -> -1.0
            else -> 0.0
        }
        // log10(max(|engagement|, 1)) — عائد متناقص، ونتجنّب log10(0) بحد أدنى 1
        val magnitude = log10(max(abs(engagement).toDouble(), 1.0))

        // الحد الزمني: كلما كانت الفقرة أحدث (createdAt أكبر)، زاد هذا الحد.
        // نستخدم createdAt بدل now حتى تبقى القيمة المخزَّنة ثابتة المعنى —
        // ترتيبها النسبي مقابل فقرات أخرى يتغيّر طبيعيًا فقط بسبب تخزين
        // فقرات جديدة بحدود زمنية أعلى، لا بسبب تغيّر قيمة هذه الفقرة نفسها.
        val timeTerm = (createdAtMillis / 1000.0) / TIME_DECAY_SECONDS

        val hotScore = (sign * magnitude) + timeTerm
        return (hotScore * FIXED_POINT_SCALE).roundToLong()
    }

    /** نسخة مختصرة تأخذ [Post] مباشرة. */
    fun computeScore(post: Post, nowMillis: Long = System.currentTimeMillis()): Long =
        computeScore(
            likes = post.likesCount,
            teks = post.teksCount,
            comments = post.commentsCount,
            dislikes = post.dislikesCount,
            createdAtMillis = post.createdAt,
            nowMillis = nowMillis
        )
}
