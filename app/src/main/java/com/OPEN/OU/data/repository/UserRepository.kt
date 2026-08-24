package com.OPEN.OU.data.repository

import com.OPEN.OU.data.model.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef get() = db.getReference(FirebasePaths.USERS)
    private val tekingRef get() = db.getReference(FirebasePaths.TEKING)
    private val tekersRef get() = db.getReference(FirebasePaths.TEKERS)

    /** يستمع مباشرة (Realtime) لتحديثات غرفة المستخدم */
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateRoom(uid: String, updates: Map<String, Any?>) {
        usersRef.child(uid).updateChildren(updates).await()
    }

    /** يحدّث الصورة الرمزية (Base64) للغرفة — يُستخدم بعد ImageCodec.encode(...). */
    suspend fun updateAvatar(uid: String, base64: String) {
        usersRef.child(uid).child("avatarBase64").setValue(base64).await()
    }

    /** يحدّث صورة البانر (Base64) للغرفة. */
    suspend fun updateBanner(uid: String, base64: String) {
        usersRef.child(uid).child("bannerBase64").setValue(base64).await()
    }

    /** "تيك" لغرفة مستخدم آخر = متابعته. tekingId يتابع tekerId */
    suspend fun tek(tekingId: String, tekerId: String) {
        tekingRef.child(tekingId).child(tekerId).setValue(true).await()
        tekersRef.child(tekerId).child(tekingId).setValue(true).await()

        usersRef.child(tekingId).child("tekingCount")
            .setValue(com.google.firebase.database.ServerValue.increment(1)).await()
        usersRef.child(tekerId).child("tekersCount")
            .setValue(com.google.firebase.database.ServerValue.increment(1)).await()
    }

    /** إلغاء التيك (إلغاء المتابعة) */
    suspend fun unTek(tekingId: String, tekerId: String) {
        tekingRef.child(tekingId).child(tekerId).removeValue().await()
        tekersRef.child(tekerId).child(tekingId).removeValue().await()

        usersRef.child(tekingId).child("tekingCount")
            .setValue(com.google.firebase.database.ServerValue.increment(-1)).await()
        usersRef.child(tekerId).child("tekersCount")
            .setValue(com.google.firebase.database.ServerValue.increment(-1)).await()
    }

    suspend fun isTeking(tekingId: String, tekerId: String): Boolean =
        tekingRef.child(tekingId).child(tekerId).get().await().exists()

    /** جلب لمرة واحدة (وليس استماعًا فوريًا) لبيانات مستخدم — يُستخدم قبل إرسال إشعار PHP مثلًا. */
    suspend fun getUser(uid: String): User? =
        usersRef.child(uid).get().await().getValue(User::class.java)

    /** يحفظ رمز إشعارات FCM الحالي للمستخدم (يُستدعى من OpouMessagingService.onNewToken). */
    suspend fun saveFcmToken(uid: String, token: String) {
        usersRef.child(uid).child("fcmToken").setValue(token).await()
    }

    /** يحفظ لغة الواجهة المفضّلة للمستخدم (تُزامن مع util/LanguagePrefs المحلي). */
    suspend fun saveLanguage(uid: String, language: String) {
        usersRef.child(uid).child("language").setValue(language).await()
    }

    /** يجلب معرّفات المستخدمين الذين يتابعهم uid (تيكينغ) — لعرضهم في شاشة "التيكرز". */
    suspend fun getTekingIds(uid: String): List<String> =
        tekingRef.child(uid).get().await().children.mapNotNull { it.key }

    /** يجلب معرّفات المستخدمين الذين يتابعون uid (تيكرز) — لعرضهم في شاشة "التيكرز". */
    suspend fun getTekerIds(uid: String): List<String> =
        tekersRef.child(uid).get().await().children.mapNotNull { it.key }

    /** يجلب بيانات عدة مستخدمين دفعة واحدة (بالتوازي) اعتمادًا على قائمة معرّفاتهم. */
    suspend fun getUsersByIds(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()
        return uids.mapNotNull { id ->
            usersRef.child(id).get().await().getValue(User::class.java)
        }
    }

    // ===== ميزات الحساب =====

    /** تعطيل الحساب مؤقتًا (يبقى تسجيل الدخول ممكنًا، لكن يُخفى المستخدم عن غيره حتى يُعاد تفعيله). */
    suspend fun setAccountStatus(uid: String, status: String) {
        usersRef.child(uid).child("accountStatus").setValue(status).await()
    }

    /** يفعّل التحقق بخطوتين ويخزّن بصمة PIN فقط (وليس الرمز نفسه). */
    suspend fun enableTwoFactor(uid: String, pinHash: String) {
        usersRef.child(uid).updateChildren(
            mapOf("twoFactorEnabled" to true, "twoFactorPinHash" to pinHash)
        ).await()
    }

    suspend fun disableTwoFactor(uid: String) {
        usersRef.child(uid).updateChildren(
            mapOf("twoFactorEnabled" to false, "twoFactorPinHash" to "")
        ).await()
    }

    suspend fun updateCategories(uid: String, categories: List<String>) {
        usersRef.child(uid).child("categories").setValue(categories).await()
    }

    suspend fun updateSocialLinks(uid: String, links: Map<String, String>) {
        usersRef.child(uid).child("socialLinks").setValue(links).await()
    }

    suspend fun updateCustomButtons(uid: String, buttons: List<com.OPEN.OU.data.model.CustomButton>) {
        usersRef.child(uid).child("customButtons").setValue(buttons).await()
    }

    /** يثبّت فقرة أعلى الغرفة (أو يلغي التثبيت إن كانت postId فارغة). */
    suspend fun setPinnedPost(uid: String, postId: String) {
        usersRef.child(uid).child("pinnedPostId").setValue(postId).await()
    }

    /** يحذف بيانات المستخدم نهائيًا من Realtime Database (يُستدعى مع حذف حساب المصادقة). */
    suspend fun deleteUserDataPermanently(uid: String, username: String) {
        usersRef.child(uid).removeValue().await()
        if (username.isNotBlank()) {
            db.getReference(FirebasePaths.USERNAMES).child(username).removeValue().await()
        }
        runCatching { db.getReference(FirebasePaths.TEKING).child(uid).removeValue().await() }
        runCatching { db.getReference(FirebasePaths.TEKERS).child(uid).removeValue().await() }
        runCatching { db.getReference(FirebasePaths.USER_REACTIONS).child(uid).removeValue().await() }
        runCatching { db.getReference(FirebasePaths.BLOCKS).child(uid).removeValue().await() }
        runCatching { db.getReference(FirebasePaths.BLOCKED_BY).child(uid).removeValue().await() }
    }

    // ===== ميزات الخصوصية =====

    suspend fun updatePrivacySettings(
        uid: String,
        isPrivateRoom: Boolean,
        hideLastSeen: Boolean,
        whoCanComment: String
    ) {
        usersRef.child(uid).updateChildren(
            mapOf(
                "isPrivateRoom" to isPrivateRoom,
                "hideLastSeen" to hideLastSeen,
                "whoCanComment" to whoCanComment
            )
        ).await()
    }

    /** يحدّث وقت آخر نشاط — يُستدعى Best-effort عند فتح التطبيق، ويُعرض فقط إن لم يكن hideLastSeen مفعّلاً. */
    suspend fun touchLastSeen(uid: String) {
        usersRef.child(uid).child("lastSeenAt").setValue(System.currentTimeMillis()).await()
    }

    // ===== ميزة المنشن (@اسم_المستخدم) =====

    /** يحوّل اسم مستخدم (بدون @) إلى معرّفه uid عبر فهرس /usernames، أو null إن لم يوجد. */
    suspend fun getUidByUsername(username: String): String? {
        if (username.isBlank()) return null
        return db.getReference(FirebasePaths.USERNAMES).child(username).get().await()
            .getValue(String::class.java)
    }
}
