package com.OPEN.OU.data.repository

import com.OPEN.OU.data.model.User
import com.OPEN.OU.util.TwoFactorGate
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/** يدير تسجيل الدخول الخاص بأوبو (بريد/كلمة مرور) وإنشاء "الغرفة" الأولى للمستخدم. */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    val currentUserId: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email

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

    fun logout() {
        // نمسح حالة "اجتاز PIN" الخاصة بهذا المستخدم حتى لا يُعفى تلقائيًا من خطوة
        // التحقق بخطوتين عند تسجيل دخول لاحق (نفسه أو لمستخدم آخر على نفس الجهاز).
        auth.currentUser?.uid?.let { TwoFactorGate.clear(it) }
        auth.signOut()
    }

    /** إعادة التوثيق بكلمة المرور الحالية — مطلوبة قبل عمليات حسّاسة (تغيير كلمة المرور/حذف الحساب)
     * لأن Firebase Auth يرفضها إن لم يكن تسجيل الدخول "حديثًا". */
    suspend fun reauthenticate(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("لا يوجد مستخدم مسجّل دخول"))
            val email = user.email ?: return Result.failure(IllegalStateException("لا يوجد بريد مرتبط بالحساب"))
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** تغيير كلمة المرور — يعيد التوثيق تلقائيًا أولًا بكلمة المرور الحالية. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = try {
        val reauth = reauthenticate(currentPassword)
        if (reauth.isFailure) {
            Result.failure(reauth.exceptionOrNull() ?: IllegalStateException("تعذّر التحقق من كلمة المرور الحالية"))
        } else {
            auth.currentUser?.updatePassword(newPassword)?.await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** يحذف حساب المصادقة نفسه نهائيًا (بعد إعادة التوثيق). حذف بيانات Realtime Database يتم بشكل منفصل. */
    suspend fun deleteAuthAccount(currentPassword: String): Result<Unit> = try {
        val reauth = reauthenticate(currentPassword)
        if (reauth.isFailure) {
            Result.failure(reauth.exceptionOrNull() ?: IllegalStateException("تعذّر التحقق من كلمة المرور الحالية"))
        } else {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
