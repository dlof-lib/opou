package com.OPEN.OU.data.repository

import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /** الحد الأقصى الآمن لحجم أي حقل نصي واحد داخل عقدة Realtime Database (بايت تقريبي). */
    private val MAX_SAFE_FIELD_BYTES = 900_000

    /** نشر فقرة جديدة */
    suspend fun createPost(post: Post): String {
        require(post.imageBase64.length <= MAX_SAFE_FIELD_BYTES) {
            "حجم الصورة المرفقة أكبر من الحد المسموح — يجب ضغطها عبر ImageCodec أولًا"
        }
        val newRef = postsRef.push()
        val postId = newRef.key.orEmpty()
        newRef.setValue(post.copy(postId = postId)).await()
        usersRef.child(post.authorId).child("paragraphsCount")
            .setValue(ServerValue.increment(1)).await()
        return postId
    }

    /** إعادة نشر فقرة أصلية باسم "تيك" — تُنشئ فقرة جديدة مرتبطة بالأصل، وتزيد عداد التيك للأصل */
    suspend fun tekPost(original: Post, tekingUserId: String, tekingUsername: String, tekingAvatar: String): String {
        val newRef = postsRef.push()
        val postId = newRef.key.orEmpty()
        val tekPost = Post(
            postId = postId,
            authorId = tekingUserId,
            authorUsername = tekingUsername,
            authorAvatarUrl = tekingAvatar,
            content = original.content,
            imageBase64 = original.imageBase64,
            isTek = true,
            originalPostId = original.postId,
            originalAuthorId = original.authorId,
            originalAuthorUsername = original.authorUsername
        )
        newRef.setValue(tekPost).await()
        postsRef.child(original.postId).child("teksCount")
            .setValue(ServerValue.increment(1)).await()
        return postId
    }

    /** الاستماع الفوري (Realtime) لتدفق الفقرات الأحدث أولًا */
    fun observeFeed(limit: Int = 50): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("createdAt").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /** تبويب "الشعبيات": الأعلى نقاط شعبية */
    fun observeShaabiyat(limit: Int = 30): Flow<List<Post>> = callbackFlow {
        val query: Query = postsRef.orderByChild("shaabiyaScore").limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    .sortedByDescending { it.shaabiyaScore }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
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
    }

    private suspend fun adjustCount(postId: String, field: String, delta: Int) {
        postsRef.child(postId).child(field).setValue(ServerValue.increment(delta.toLong())).await()
    }

    /** نقاط الشعبية = (الإعجابات × 3) + (التيكات × 5) + التعليقات - عدم الإعجاب */
    private suspend fun recomputeShaabiya(postId: String) {
        val snapshot = postsRef.child(postId).get().await()
        val post = snapshot.getValue(Post::class.java) ?: return
        val score = (post.likesCount * 3L) + (post.teksCount * 5L) + post.commentsCount - post.dislikesCount
        postsRef.child(postId).child("shaabiyaScore").setValue(score).await()
    }

    suspend fun addComment(comment: Comment): String {
        val newRef = commentsRef.child(comment.postId).push()
        val commentId = newRef.key.orEmpty()
        newRef.setValue(comment.copy(commentId = commentId)).await()
        adjustCount(comment.postId, "commentsCount", 1)
        recomputeShaabiya(comment.postId)
        return commentId
    }

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val ref = commentsRef.child(postId).orderByChild("createdAt")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Comment::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
