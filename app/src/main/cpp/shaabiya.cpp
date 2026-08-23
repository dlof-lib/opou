#include "shaabiya.h"

namespace opou {

// نفس الصيغة المستخدمة في PostRepository.kt (طبقة Kotlin) وفي القواعد الخادمية،
// موجودة هنا أصليًا (Native) للاستخدام في حسابات محلية سريعة بدون طلب شبكة.
int64_t computeShaabiyaScore(int32_t likes, int32_t dislikes, int32_t teks, int32_t comments) {
    const int64_t score =
        static_cast<int64_t>(likes) * 3 +
        static_cast<int64_t>(teks) * 5 +
        static_cast<int64_t>(comments) -
        static_cast<int64_t>(dislikes);
    return score;
}

}  // namespace opou
