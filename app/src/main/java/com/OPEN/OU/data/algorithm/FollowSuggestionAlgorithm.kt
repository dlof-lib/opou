package com.OPEN.OU.data.algorithm

import kotlin.math.ln
import kotlin.math.max

/**
 * خوارزمية "قد تعرفهم" — اقتراح غرف/مستخدمين للمتابعة. هذه الخوارزمية بالذات
 * هي الأكثر تأثيرًا مباشرًا على "الشهرة" ضمن مجموعة اليوم: هي التي *تصنع*
 * متابعين جددًا فعليًا، بعكس [TrendingAlgorithm] و[UserFameAlgorithm] اللتين
 * تقيسان شهرة موجودة أصلًا فقط.
 *
 * المدخلات لكل مستخدم مرشَّح:
 *  - mutualConnections: عدد من يتابعهم المستخدم الحالي ويتابعون هذا المرشَّح
 *    أيضًا ("صداقة الصديق" — friend-of-friend). أقوى إشارة ثقة: لو 5 ممن
 *    تتابعهم يتابعون شخصًا معيّنًا، احتمال أنه يهمّك فعلاً أعلى بكثير من مجرد
 *    كونه مشهورًا بشكل عام.
 *  - sharedInterestBonus: علم بسيط (true/false) — هل هذا المرشَّح من "مؤلفين
 *    مهتم بهم" المستخدم (استنتاجًا من فقرات أعجب بها سابقًا، بنفس منطق
 *    SuggestionsWorker الحالي لاقتراح المحتوى).
 *  - fameScore: شهرة المرشَّح مدى الحياة (User.shaabiyaScore) — إشارة جودة/
 *    نشاط عامة، لكن بوزن أقل ومخمَّدة لوغاريتميًا حتى لا تُقصي كل الحسابات
 *    الصغيرة تلقائيًا من الاقتراحات (وهو بالضبط ما يمنع الخوارزمية من الاكتفاء
 *    الدائم بتقوية "المشهورين أصلًا" على حساب أي حساب جديد).
 *
 * تنويع متعمَّد (Diversity slot):
 * ------------------------------
 * لو رتّبنا المرشحين فقط حسب النقاط الكلية، الحسابات الكبيرة أصلًا (فامها
 * اللوغاريتمي مرتفع + مرتبطة بشبكة واسعة) ستهيمن باستمرار على قائمة
 * الاقتراحات — وهو عكس تمامًا هدف "المساعدة على الشهرة" لحسابات صغيرة/جديدة.
 * لذلك [pickSuggestions] تحجز عمدًا نسبة من المقاعد لأفضل المرشحين من ضمن
 * "النجوم الصاعدة" (isRising = true) حتى لو نقاطهم الكلية أقل، طالما تجاوزوا
 * حدًا أدنى معقولًا من الصلة (mutualConnections > 0 أو sharedInterestBonus).
 */
object FollowSuggestionAlgorithm {

    private const val MUTUAL_WEIGHT = 25.0
    private const val INTEREST_BONUS = 40.0
    private const val FAME_LOG_WEIGHT = 6.0

    /** نسبة المقاعد المحجوزة لصالح "النجوم الصاعدة" ذات الصلة (0.0–1.0). */
    private const val RISING_SLOT_RATIO = 0.34

    data class Candidate(
        val uid: String,
        val mutualConnections: Int,
        val sharedInterest: Boolean,
        val fameScore: Long,
        val isRising: Boolean
    )

    /** النقاط الكلية لمرشّح واحد — تُستخدم للترتيب العام (غير المقاعد المحجوزة). */
    fun score(candidate: Candidate): Double {
        val mutualTerm = candidate.mutualConnections.toDouble() * MUTUAL_WEIGHT
        val interestTerm = if (candidate.sharedInterest) INTEREST_BONUS else 0.0
        val fameTerm = ln(max(candidate.fameScore.toDouble(), 0.0) + 1.0) * FAME_LOG_WEIGHT
        return mutualTerm + interestTerm + fameTerm
    }

    /**
     * يختار أفضل [limit] مرشَّح من [candidates]، مع حجز [RISING_SLOT_RATIO] من
     * المقاعد لأفضل النجوم الصاعدة ذات الصلة الحقيقية (تفادي اقتراح صاعدين
     * عشوائيين تمامًا بلا أي رابط)، والباقي بالترتيب العام حسب [score].
     */
    fun pickSuggestions(candidates: List<Candidate>, limit: Int): List<Candidate> {
        if (candidates.size <= limit) return candidates.sortedByDescending { score(it) }

        val risingSlots = (limit * RISING_SLOT_RATIO).toInt().coerceAtLeast(if (limit > 0) 1 else 0)
        val relevantRising = candidates
            .filter { it.isRising && (it.mutualConnections > 0 || it.sharedInterest) }
            .sortedByDescending { score(it) }
            .take(risingSlots)

        val pickedIds = relevantRising.map { it.uid }.toSet()
        val remainingSlots = limit - relevantRising.size
        val rest = candidates
            .filter { it.uid !in pickedIds }
            .sortedByDescending { score(it) }
            .take(remainingSlots)

        // نُعيد ترتيب القائمة النهائية حسب النقاط حتى لا تظهر مجموعة الصاعدين
        // ككتلة منفصلة بصريًا في أعلى أو أسفل القائمة.
        return (relevantRising + rest).sortedByDescending { score(it) }
    }
}
