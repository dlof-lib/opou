package com.OPEN.OU.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * يدير حظر المستخدمين. الحظر أحادي الاتجاه من منظور المُحظِر (uid يحظر targetUid)،
 * لكن يُطبَّق فعليًا في كلا الاتجاهين عند تصفية المحتوى (لا يرى أي منهما فقرات/غرفة الآخر).
 */
class BlockRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val blocksRef get() = db.getReference(FirebasePaths.BLOCKS)
    private val blockedByRef get() = db.getReference(FirebasePaths.BLOCKED_BY)

    suspend fun block(uid: String, targetUid: String) {
        if (uid == targetUid) return
        blocksRef.child(uid).child(targetUid).setValue(true).await()
        blockedByRef.child(targetUid).child(uid).setValue(true).await()
    }

    suspend fun unblock(uid: String, targetUid: String) {
        blocksRef.child(uid).child(targetUid).removeValue().await()
        blockedByRef.child(targetUid).child(uid).removeValue().await()
    }

    suspend fun isBlocked(uid: String, targetUid: String): Boolean =
        blocksRef.child(uid).child(targetUid).get().await().exists()

    /** مستخدمون قام uid بحظرهم */
    suspend fun getBlockedIds(uid: String): List<String> =
        blocksRef.child(uid).get().await().children.mapNotNull { it.key }

    /** مستخدمون قاموا بحظر uid — يُستخدم لإخفاء غرفة/فقرات uid عنهم */
    suspend fun getBlockedByIds(uid: String): List<String> =
        blockedByRef.child(uid).get().await().children.mapNotNull { it.key }

    /** يجمع الاتجاهين دفعة واحدة لتصفية سريعة (feed/profile): من حظرني + من حظرته أنا */
    suspend fun getMutedIds(uid: String): Set<String> =
        (getBlockedIds(uid) + getBlockedByIds(uid)).toSet()

    fun observeBlockedIds(uid: String): Flow<List<String>> = callbackFlow {
        val ref = blocksRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
