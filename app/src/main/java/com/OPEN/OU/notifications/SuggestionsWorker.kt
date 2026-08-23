package com.OPEN.OU.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.PostRepository
import com.OPEN.OU.data.repository.UserRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * "فقرات مقترحة لك" — توصيات محتوى تُبنى بالكامل على الجهاز، بدون أي خادم توصيات:
 *
 * 1) يقرأ الفقرات التي أعجبته المستخدم سابقًا (⭐) ليستنتج منها "مؤلفين يهتم بهم" (اهتمامات ضمنية).
 * 2) يجلب كل الفقرات العامة المرئية له وغير الخاصة به وغير المتفاعَل معها بعد.
 * 3) يمنح كل فقرة نقاطًا: شعبيتها العامة (shaabiyaScore) + مكافأة كبيرة إن كان مؤلفها
 *    من "اهتماماته"، ثم يختار أفضل 1-3 فقرات ويعرضها كإشعار محلي واحد أنيق.
 *
 * يعمل دوريًا كل 12 ساعة عبر WorkManager، مع حماية من التكرار المزعج: لا يُعاد إشعار
 * بنفس أفضل فقرة مرتين متتاليتين (يُخزَّن آخر معرّف فقرة مُقترَحة في SharedPreferences).
 */
class SuggestionsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val postRepo = PostRepository()
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()
    private val prefs: SharedPreferences =
        applicationContext.getSharedPreferences("opou_suggestions_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        val uid = authRepo.currentUserId ?: return Result.success() // لا مستخدم مسجّل، لا داعٍ للمحاولة

        return runCatching {
            val followingIds = runCatching { userRepo.getTekingIds(uid).toSet() }.getOrDefault(emptySet())
            val myReactions = postRepo.getMyReactionsOnce(uid)
            val allPosts = postRepo.getAllPostsOnce()

            // "اهتماماته": مؤلفو الفقرات التي أعجبته (⭐) سابقًا
            val likedPostIds = myReactions.filterValues { it == "LIKE" }.keys
            val interestedAuthorIds = allPosts.filter { it.postId in likedPostIds }
                .map { it.authorId }
                .toSet()

            val candidates = allPosts.filter { post ->
                post.authorId != uid &&
                    post.postId !in myReactions.keys &&
                    postRepo.isVisibleToViewer(post, uid, followingIds)
            }

            val ranked = candidates
                .map { post ->
                    val interestBonus = if (post.authorId in interestedAuthorIds) 1000L else 0L
                    post to (post.shaabiyaScore + interestBonus)
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(3)

            if (ranked.isEmpty()) return Result.success()

            // منع التكرار المزعج: لا نُعيد نفس أفضل اقتراح إن لم يتغيّر منذ آخر مرة
            val topId = ranked.first().postId
            val lastNotifiedTopId = prefs.getString(KEY_LAST_TOP_POST_ID, null)
            if (topId == lastNotifiedTopId) return Result.success()

            NotificationDisplay.showSuggestions(applicationContext, ranked)
            prefs.edit().putString(KEY_LAST_TOP_POST_ID, topId).apply()
            Result.success()
        }.getOrElse {
            Timber.tag("Suggestions").w(it, "فشل بناء الفقرات المقترحة")
            Result.retry()
        }
    }

    companion object {
        private const val KEY_LAST_TOP_POST_ID = "last_top_post_id"
        private const val UNIQUE_WORK_NAME = "opou_suggestions_worker"

        /**
         * يجدول عامل التوصيات دوريًا (كل 12 ساعة)، بشرط توفر اتصال إنترنت.
         * KEEP يمنع إعادة الجدولة من الصفر عند كل إقلاع للتطبيق إن كان مجدولًا فعلًا.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SuggestionsWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
