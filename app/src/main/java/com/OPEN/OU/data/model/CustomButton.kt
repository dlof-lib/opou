package com.OPEN.OU.data.model

/**
 * زر مخصّص يضيفه صاحب الغرفة إلى ملفه الشخصي (مثال: "تواصل معي واتساب" -> رابط).
 * يُعرض في ProfileScreen كصف أزرار قابلة للنقر تفتح الرابط في المتصفح.
 */
data class CustomButton(
    val label: String = "",
    val url: String = ""
)
