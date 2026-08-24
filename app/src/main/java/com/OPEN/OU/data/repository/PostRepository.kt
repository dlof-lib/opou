package com.OPEN.OU.data.repository

import com.OPEN.OU.util.NativeBridge
import com.OPEN.OU.data.algorithm.TrendingAlgorithm
import com.OPEN.OU.data.algorithm.UserFameAlgorithm
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.model.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

/** يدير "الفقرات" (المنشورات) مع حرية التعبير الكاملة: نص مفتوح بدون قيود على المحتوى. */
class PostRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val postsRef get() = db.getReference(FirebasePaths.POSTS)
    private val commentsRef get() = db.getReference(FirebasePaths.COMMENTS)
    private val reactionsRef get() = db.getReference(FirebasePaths.REACTIONS)
    private val usersRef get() = db.getReference(FirebasePaths.USERS)
    private val userReactionsRef get() = db.getReference(FirebasePaths.USER_REACTIONS)
    private val commentLikesRef get() = db.getReference(FirebasePaths.COMMENT_LIKES)
    private val userCommentLikesRef get() = db.getReference(FirebasePaths.USER_COMMENT_LIKES)

    /**
     * الحد الأقصى الآمن لحجم أي حقل نصي واحد داخل عقدة Realtime Database (عدد الأحرف تقريبًا).
     * ملاحظة: ImageCodec.SAFE_BASE64_CHAR_LIMIT (850,000) مضبوط ليبقى دومًا أقل من هذا
     * الحد بهامش أمان مريح، لذا لن يُفعَّل هذا الفحص عمليًا إلا في حالات استثنائية
     * (مثل صورة جاهزة الترميز أصلًا ولم تمرّ عبر ImageCodec).
     */
    private val MAX_SAFE_FIELD_BYTES = 900_000

    /** نشر فقرة جديدة */
    suspend fun createPost(post: Post): String {
        require(post.imageBase64.length <= MAX_SAFE_FIELD_BYTES) {
            "حجم الصورة المرفقة أكبر من الحد المسموح حتى بعد الضغط. جرّب صورة أبسط أو أصغر."
        }
        val newRef = postsRef.push()
        val postId = newRef.key.orEmpty()
        // نحسب نقاط الشعبية الأولية فورًا (تعتمد على وقت النشر فقط بما أن
        // التفاعل صفر) — بدون هذا تبقى الفقرة عند 0 ولا تظهر مطلقًا في
        // تبويب "الشعبيات" حتى يتفاعل معها أحد لأول مرة.
        val initialScore = TrendingAlgorithm.computeScore(post.copy(postId = postId))
        newRef.setValue(post.copy(postId = postId, shaabiyaScore = initialScore)).await()
        usersRef.child(post.authorId).child("paragraphsCount")
            .setValue(ServerValue.increment(1)).await()
        return postId
    }

    /** يعدّل محتوى فقرة — يتحقق أولًا أن [uid] هو صاحبها الفعلي. */
    suspend fun updatePostContent(postId: String, uid: String, newContent: String) {
        val post = postsRef.child(postId).get().await().getValue(Post::class.java)
            ?: throw IllegalStateException("الفقرة غير موجودة")
        require(post.authorId == uid) { "لا يمكنك تعديل فقرة لا تملكها" }
        postsRef.child(postId).child("content").setValue(newContent).await()
    }

    /** يحذف فقرة نهائيًا (مع تعليقاتها وتفاعلاتها) — يتحقق أولًا أن [uid] هو صاحبها الفعلي. */
    suspend fun deletePost(postId: String, uid: String) {
        val post = postsRef.child(postId).get().await().getValue(Post::class.java)
            ?: throw IllegalStateException("الفقرة غير موجودة")
        require(post.authorId == uid) { "لا يمكنك حذف فقرة لا تملكها" }
        postsRef.child(postId).removeValue().await()
        commentsRef.child(postId).removeValue().await()
        reactionsRef.child(postId).removeValue().await()
        usersRef.child(uid).child("paragraphsCount").setValue(ServerValue.increment(-1)).await()
    }

    /** إعادة نشر فقرة أصلية باسم "تيك" — تُنشئ فقرة جديدة مرتبطة بالأصل، وتزيد عداد التيك للأصل */
    suspend fun tekPost(
        original: Post,
        tekingUserId: String,
        tekingUsername: String,
        tekingAvatar: String,
        tekingAvatarBase64: String = ""
    ): String {
        val newRef = postsRef.push()
        val postId = newRef.key.orEmpty()
        val tekPost = Post(
            postId = postId,
            authorId = tekingUserId,
            authorUsername = tekingUsername,
            authorAvatarUrl = tekingAvatar,
            authorAvatarBase64 = tekingAvatarBase64,
            content = original.content,
            imageBase64 = original.imageBase64,
            isTek = true,
            originalPostId = original.postId,
            originalAuthorId = original.authorId,
            originalAuthorUsername = original.authorUsername
        )
        val initialScore = TrendingAlgorithm.computeScore(tekPost)
        newRef.setValue(tekPost.copy(shaabiyaScore = initialScore)).await()
        postsRef.child(original.postId).child("teksCount")
            .setValue(ServerValue.increment(1)).await()
        // كان هذا مفقودًا سابقًا: التيك (إعادة النشر) كان يزيد teksCount لكن لا
        // يُعيد حساب shaabiyaScore للفقرة الأصلية إلا إن حدث تفاعل آخر عليها لاحقًا،
        // فكانت الفقرات المُعاد نشرها بكثرة لا تصعد في "الشعبيات" فورًا رغم انتشارها الفعلي.
        recomputeShaabiya(original.postId)
        bumpAuthorFame(original.postId, TrendingAlgorithm.WEIGHT_TEK.toLong())
        return postId
    }

    /**
     * يجلب كل الفقرات دفعة واحدة (وليس Realtime) — تُستخدم من عامل التوصيات
     * (SuggestionsWorker) في الخلفية لحساب "فقرات مقترحة لك" دون فتح استماع دائم.
     */
    suspend fun getAllPostsOnce(): List<Post> =
        postsRef.get().await().children.mapNotNull { runCatching { it.getValue(Post::class.java) }.getOrNull() }

    /**
     * يجلب خريطة تفاعلات مستخدم معيّن دفعة واحدة: postId -> "LIKE"|"DISLIKE".
     * تُستخدم كمؤشر اهتمام (المؤلفون الذين أعجبته فقراتهم سابقًا) في عامل التوصيات.
     */
    suspend fun getMyReactionsOnce(uid: String): Map<String, String> =
        userReactionsRef.child(uid).get().await().children
            .mapNotNull { child -> child.getValue(String::class.java)?.let { child.key.orEmpty() to it } }
            .toMap()

    /** يتحقق من ظهور فقرة لمستخدم زائر بعينه وفق قواعد الخصوصية — يُستخدم خارج هذا الملف أيضًا (مثال: عامل التوصيات). */
    fun isVisibleToViewer(post: Post, viewerId: String?, viewerFollowingIds: Set<String> = emptySet()): Boolean =
        isVisibleTo(post, viewerId, viewerFollowingIds, emptySet())

    /** الاستماع الفوري (Realtime) لتدفق الفقرات الأحدث أولًا، مع تطبيق خصوصية الفقرة وحالة الجدولة
     * و[mutedIds] (من حظرهم المستخدم أو من حظروه — يُستبعدون بالكامل من التغذية). */
    fun observeFeed(
        limit: Int = 50,
        viewerId: String? = null,
        viewerFollowingIds: Set<String> = emptySet(),
        mutedIds: Set<String> = emptySet()
    ): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("createdAt").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { runCatching { it.getValue(Post::class.java) }.getOrNull() }
                    .filter { isVisibleTo(it, viewerId, viewerFollowingIds, mutedIds) }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.catch {
        // مثال: صلاحيات Firebase مرفوضة أو انقطاع اتصال. بدون هذا كان الخطأ يتسرّب من
        // الـ Flow ويُسقط التطبيق فور فتح الشاشة الرئيسية.
        emit(emptyList())
    }

    /** تبويب "الشعبيات": الأعلى نقاط شعبية، مع نفس قواعد الخصوصية/الجدولة/الحظر */
    fun observeShaabiyat(
        limit: Int = 30,
        viewerId: String? = null,
        viewerFollowingIds: Set<String> = emptySet(),
        mutedIds: Set<String> = emptySet()
    ): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("shaabiyaScore").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { runCatching { it.getValue(Post::class.java) }.getOrNull() }
                    .filter { isVisibleTo(it, viewerId, viewerFollowingIds, mutedIds) }
                    .sortedByDescending { it.shaabiyaScore }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.catch { emit(emptyList()) }

    /**
     * فقرات غرفة مستخدم واحد فقط (للعرض في ProfileScreen)، مع الفقرة المثبّتة أولًا
     * ثم البقية الأحدث فأحدث، وتطبيق نفس قواعد الخصوصية/الحظر.
     */
    fun observeUserPosts(
        authorUid: String,
        viewerId: String?,
        viewerFollowingIds: Set<String> = emptySet(),
        mutedIds: Set<String> = emptySet()
    ): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("authorId").equalTo(authorUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { runCatching { it.getValue(Post::class.java) }.getOrNull() }
                    .filter { isVisibleTo(it, viewerId, viewerFollowingIds, mutedIds) }
                    .sortedWith(compareByDescending<Post> { it.isPinned }.thenByDescending { it.createdAt })
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.catch {
        // بدون هذا كان أي خطأ Firebase (صلاحيات مثلاً) يُسقط التطبيق فور فتح "الحساب".
        emit(emptyList())
    }

    /** يثبّت/يلغي تثبيت فقرة في غرفتها. يلغي أي تثبيت سابق لنفس المستخدم تلقائيًا (تثبيت واحد كحد أقصى). */
    suspend fun setPinned(authorUid: String, postId: String, pinned: Boolean, previousPinnedPostId: String?) {
        if (pinned && !previousPinnedPostId.isNullOrBlank() && previousPinnedPostId != postId) {
            postsRef.child(previousPinnedPostId).child("isPinned").setValue(false).await()
        }
        postsRef.child(postId).child("isPinned").setValue(pinned).await()
        usersRef.child(authorUid).child("pinnedPostId").setValue(if (pinned) postId else "").await()
    }

    /**
     * يحضّر معلومات ربط فقرة جديدة كمتابعة لسلسلة انطلاقًا من [anchorPost] (آخر فقرة يختار
     * المستخدم المتابعة بعدها). إن لم تكن [anchorPost] جزءًا من أي سلسلة بعد، يحوّلها هذا
     * الاستدعاء إلى رأس سلسلة جديدة (threadId = معرّفها هي نفسها) قبل إرجاع بيانات الفقرة التالية.
     * يُعيد Triple(threadId, previousPostId, nextPosition) لاستخدامها عند بناء الفقرة الجديدة.
     */
    suspend fun continueThread(anchorPost: Post): Triple<String, String, Int> {
        if (anchorPost.threadId.isBlank()) {
            postsRef.child(anchorPost.postId).updateChildren(
                mapOf("threadId" to anchorPost.postId, "threadPosition" to 1)
            ).await()
            return Triple(anchorPost.postId, anchorPost.postId, 2)
        }
        return Triple(anchorPost.threadId, anchorPost.postId, anchorPost.threadPosition + 1)
    }

    /** يستمع فوريًا (Realtime) لكل فقرات سلسلة معيّنة، مرتّبة حسب موضعها في السلسلة (الأقدم أولًا). */
    fun observeThread(threadId: String): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("threadId").equalTo(threadId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { runCatching { it.getValue(Post::class.java) }.getOrNull() }
                    .sortedBy { it.threadPosition }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }.catch { emit(emptyList()) }

    /**
     * يطبّق قواعد خصوصية الفقرة (PUBLIC/PRIVATE/LIMITED/CUSTOM)، حالة الجدولة (scheduledAt)،
     * والحظر المتبادل [mutedIds] لتحديد ما إذا كانت فقرة معينة يجب أن تظهر لمستخدم زائر بعينه.
     * الفقرات المجدولة لموعد مستقبلي لا تظهر إلا لصاحبها (كمعاينة).
     */
    private fun isVisibleTo(post: Post, viewerId: String?, viewerFollowingIds: Set<String>, mutedIds: Set<String>): Boolean {
        val isOwner = viewerId != null && viewerId == post.authorId
        if (!isOwner && post.authorId in mutedIds) return false
        if (post.isScheduledForFuture() && !isOwner) return false
        if (isOwner) return true

        return when (com.OPEN.OU.data.model.ParagraphPrivacy.fromValue(post.privacy)) {
            com.OPEN.OU.data.model.ParagraphPrivacy.PUBLIC -> true
            com.OPEN.OU.data.model.ParagraphPrivacy.PRIVATE -> false
            com.OPEN.OU.data.model.ParagraphPrivacy.LIMITED ->
                viewerId != null && post.authorId in viewerFollowingIds
            com.OPEN.OU.data.model.ParagraphPrivacy.CUSTOM ->
                viewerId != null && viewerId in post.allowedViewerIds
        }
    }

    /** تفاعل ⭐ إعجاب أو 💔 عدم إعجاب — يمنع الازدواجية ويعالج التبديل بين الحالتين */
    suspend fun react(postId: String, uid: String, newType: ReactionType) {
        val ref = reactionsRef.child(postId).child(uid)
        val current = ref.get().await().getValue(String::class.java)
        val currentType = when (current) {
            "LIKE" -> ReactionType.LIKE
            "DISLIKE" -> ReactionType.DISLIKE
            else -> ReactionType.NONE
        }
        if (currentType == newType) return // لا تغيير

        // إزالة أثر التفاعل السابق
        when (currentType) {
            ReactionType.LIKE -> adjustCount(postId, "likesCount", -1)
            ReactionType.DISLIKE -> adjustCount(postId, "dislikesCount", -1)
            ReactionType.NONE -> {}
        }

        val userReactionRef = userReactionsRef.child(uid).child(postId)
        when (newType) {
            ReactionType.LIKE -> {
                ref.setValue("LIKE").await()
                userReactionRef.setValue("LIKE").await()
                adjustCount(postId, "likesCount", 1)
            }
            ReactionType.DISLIKE -> {
                ref.setValue("DISLIKE").await()
                userReactionRef.setValue("DISLIKE").await()
                adjustCount(postId, "dislikesCount", 1)
            }
            ReactionType.NONE -> {
                ref.removeValue().await()
                userReactionRef.removeValue().await()
            }
        }

        recomputeShaabiya(postId)

        // فرق وزن التفاعل بين الحالة القديمة والجديدة — يُستخدم لتحديث "شهرة"
        // صاحب الفقرة (رصيد تراكمي مدى الحياة، راجع UserFameAlgorithm).
        val oldNativeType = when (currentType) {
            ReactionType.LIKE -> 1
            ReactionType.DISLIKE -> 2
            ReactionType.NONE -> 0
        }
        val newNativeType = when (newType) {
            ReactionType.LIKE -> 1
            ReactionType.DISLIKE -> 2
            ReactionType.NONE -> 0
        }
        val delta = NativeBridge.reactionDelta(oldNativeType, newNativeType)
        if (delta != 0L) bumpAuthorFame(postId, delta)
    }

    private fun reactionWeight(type: ReactionType): Int = when (type) {
        ReactionType.LIKE -> TrendingAlgorithm.WEIGHT_LIKE
        ReactionType.DISLIKE -> -TrendingAlgorithm.WEIGHT_DISLIKE
        ReactionType.NONE -> 0
    }

    /**
     * يراقب فوريًا (Realtime) خريطة تفاعلات المستخدم الحالي على كل الفقرات: postId -> ReactionType.
     * تُستخدم في FeedScreen لتلوين زر ⭐/💔 الصحيح لكل فقرة دون طلب شبكة إضافي لكل عنصر.
     */
    fun observeMyReactions(uid: String): Flow<Map<String, ReactionType>> = callbackFlow {
        val ref = userReactionsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = snapshot.children.associate { child ->
                    val type = when (child.getValue(String::class.java)) {
                        "LIKE" -> ReactionType.LIKE
                        "DISLIKE" -> ReactionType.DISLIKE
                        else -> ReactionType.NONE
                    }
                    child.key.orEmpty() to type
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.catch { emit(emptyMap()) }

    private suspend fun adjustCount(postId: String, field: String, delta: Int) {
        postsRef.child(postId).child(field).setValue(ServerValue.increment(delta.toLong())).await()
    }

    /**
     * يعيد حساب نقاط الشعبية عبر [TrendingAlgorithm] (Hot Ranking بتخميد زمني
     * لوغاريتمي — راجع توثيق الخوارزمية هناك للتفاصيل والمبرر).
     */
    private suspend fun recomputeShaabiya(postId: String) {
        val snapshot = postsRef.child(postId).get().await()
        val post = snapshot.getValue(Post::class.java) ?: return
        val score = TrendingAlgorithm.computeScore(post)
        postsRef.child(postId).child("shaabiyaScore").setValue(score).await()
    }

    /**
     * يضيف [engagementDelta] إلى رصيد "شهرة" صاحب الفقرة [postId] التراكمي
     * (User.totalEngagementScore)، ثم يعيد حساب User.shaabiyaScore عبر
     * [UserFameAlgorithm]. يُستدعى من كل نقطة تفاعل تخصّ الفقرات (تفاعل ⭐/💔،
     * تعليق، تيك) — راجع توثيق UserFameAlgorithm لتفسير الفرق بين هذا الرصيد
     * التراكمي الذي لا يتخامد أبدًا وبين shaabiyaScore الفقرة الواحدة المتخامد.
     */
    private suspend fun bumpAuthorFame(postId: String, engagementDelta: Long) {
        val authorId = postsRef.child(postId).child("authorId").get().await()
            .getValue(String::class.java) ?: return
        usersRef.child(authorId).child("totalEngagementScore")
            .setValue(ServerValue.increment(engagementDelta)).await()
        recomputeUserFame(authorId)
    }

    /** يعيد حساب User.shaabiyaScore (شهرة "مدى الحياة") عبر [UserFameAlgorithm]. */
    private suspend fun recomputeUserFame(uid: String) {
        val snapshot = usersRef.child(uid).get().await()
        val user = snapshot.getValue(User::class.java) ?: return
        val score = UserFameAlgorithm.computeScore(user)
        usersRef.child(uid).child("shaabiyaScore").setValue(score).await()
    }

    suspend fun addComment(comment: Comment): String {
        val newRef = commentsRef.child(comment.postId).push()
        val commentId = newRef.key.orEmpty()
        newRef.setValue(comment.copy(commentId = commentId)).await()
        adjustCount(comment.postId, "commentsCount", 1)
        recomputeShaabiya(comment.postId)
        bumpAuthorFame(comment.postId, TrendingAlgorithm.WEIGHT_COMMENT.toLong())
        return commentId
    }

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val ref = commentsRef.child(postId).orderByChild("createdAt")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { runCatching { it.getValue(Comment::class.java) }.getOrNull() }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.catch {
        // بدون هذا كان أي خطأ Firebase يُسقط التطبيق فور فتح ورقة التعليقات.
        emit(emptyList())
    }

    /** يحذف تعليقًا نهائيًا (يُستدعى من صاحب التعليق أو صاحب الفقرة — التحقق من الصلاحية يتم في الواجهة). */
    suspend fun deleteComment(postId: String, commentId: String) {
        commentsRef.child(postId).child(commentId).removeValue().await()
        adjustCount(postId, "commentsCount", -1)
        recomputeShaabiya(postId)
        bumpAuthorFame(postId, -TrendingAlgorithm.WEIGHT_COMMENT.toLong())
    }

    /** إعجاب/إلغاء إعجاب ⭐ بتعليق — يمنع الازدواجية عبر فهرس /commentLikes مع فهرس معكوس لعرض حالة المستخدم فوريًا. */
    suspend fun toggleCommentLike(postId: String, commentId: String, uid: String) {
        val likeRef = commentLikesRef.child(commentId).child(uid)
        val alreadyLiked = likeRef.get().await().exists()
        val commentCountRef = commentsRef.child(postId).child(commentId).child("likesCount")
        if (alreadyLiked) {
            likeRef.removeValue().await()
            userCommentLikesRef.child(uid).child(commentId).removeValue().await()
            commentCountRef.setValue(ServerValue.increment(-1)).await()
        } else {
            likeRef.setValue(true).await()
            userCommentLikesRef.child(uid).child(commentId).setValue(true).await()
            commentCountRef.setValue(ServerValue.increment(1)).await()
        }
    }

    /** يراقب فوريًا معرّفات التعليقات التي أعجب بها [uid] — تُستخدم لتلوين زر ⭐ الصحيح في ورقة التعليقات. */
    fun observeMyCommentLikes(uid: String): Flow<Set<String>> = callbackFlow {
        val ref = userCommentLikesRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key }.toSet())
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.catch { emit(emptySet()) }
}
