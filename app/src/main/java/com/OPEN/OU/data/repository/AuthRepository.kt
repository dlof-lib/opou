package com.OPEN.OU.data.repository

import com.OPEN.OU.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/** يدير تسجيل الدخول الخاص بأوبو (بريد/كلمة مرور) وإنشاء "الغرفة" الأولى للمستخدم. */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<String> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user?.uid.orEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(
        email: String,
        password: String,
        username: String,
        communityName: String
    ): Result<String> = try {
        // تأكد أن اسم المستخدم غير مستخدم مسبقًا
        val usernameSnapshot = db.getReference(FirebasePaths.USERNAMES).child(username).get().await()
        if (usernameSnapshot.exists()) {
            Result.failure(IllegalStateException("اسم المستخدم مستخدم بالفعل"))
        } else {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid.orEmpty()

            val newUser = User(
                uid = uid,
                username = username,
                communityName = communityName
            )
            db.getReference(FirebasePaths.USERS).child(uid).setValue(newUser).await()
            db.getReference(FirebasePaths.USERNAMES).child(username).setValue(uid).await()

            Result.success(uid)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()
}
