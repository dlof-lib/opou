package com.OPEN.OU.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.ui.theme.OpouAccentGreen
import kotlinx.coroutines.launch

/** يدعم أحرف عربية/إنجليزية وأرقامًا وشرطة سفلية، بحد أدنى حرفين بعد @ */
private val MENTION_REGEX = Regex("(?<![\\w@\\u0600-\\u06FF])@([A-Za-z0-9_\\u0600-\\u06FF]{2,30})")

/** أدوات استخراج ميزة "المنشن" (@اسم_المستخدم) من نص فقرة أو تعليق. */
object MentionUtils {
    /** يستخرج كل أسماء المستخدمين المذكورة (بدون @) من نص، بلا تكرار. */
    fun extractMentions(text: String): List<String> =
        if (text.isBlank()) emptyList()
        else MENTION_REGEX.findAll(text).map { it.groupValues[1] }.distinct().toList()
}

/**
 * نص يعرض منشنز (@اسم) مُلوَّنة وقابلة للنقر لفتح غرفة صاحبها — يُستخدم بدلاً
 * من Text العادي في محتوى الفقرات والتعليقات. عند النقر على منشن، يبحث عن
 * uid صاحب الاسم عبر UserRepository (فهرس /usernames) وينادي [onOpenProfile].
 * إن لم يوجد مستخدم بهذا الاسم، لا يحدث شيء (منشن غير صالح/محذوف الحساب).
 */
@Composable
fun MentionAwareText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onOpenProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val userRepo = remember { UserRepository() }

    val annotated = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            MENTION_REGEX.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                pushStringAnnotation(tag = MENTION_TAG, annotation = match.groupValues[1])
                withStyle(SpanStyle(color = OpouAccentGreen, fontWeight = FontWeight.SemiBold)) {
                    append(match.value)
                }
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex <= text.length) append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = MENTION_TAG, start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    scope.launch {
                        val uid = runCatching { userRepo.getUidByUsername(annotation.item) }.getOrNull()
                        if (uid != null) onOpenProfile(uid)
                    }
                }
        }
    )
}

private const val MENTION_TAG = "mention"
