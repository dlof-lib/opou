package com.OPEN.OU.util

/**
 * يتتبّع أي مستخدمين أكملوا خطوة PIN (التحقق بخطوتين) بنجاح خلال عمر عملية
 * التطبيق الحالية (In-memory فقط — يُعاد تصفيره تلقائيًا عند إغلاق التطبيق فعليًا).
 *
 * لماذا هذا ضروري: Firebase Auth يُبقي المستخدم "مسجَّل دخول" فعليًا بمجرد
 * نجاح كلمة المرور، حتى قبل إدخال رمز PIN. بدون هذا الحارس، كان يكفي المهاجم
 * (يملك كلمة المرور فقط) إغلاق التطبيق وإعادة فتحه ليتخطى شاشة PIN تمامًا،
 * لأن OpouNavGraph كان يتحقق فقط من وجود جلسة Firebase Auth سارية دون التأكد
 * من اكتمال خطوة PIN.
 */
object TwoFactorGate {
    private val verifiedUids = mutableSetOf<String>()

    fun isVerified(uid: String): Boolean = uid in verifiedUids

    fun markVerified(uid: String) { verifiedUids += uid }

    fun clear(uid: String) { verifiedUids -= uid }
}
