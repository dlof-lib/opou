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
}
