package com.OPEN.OU.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * محوّل HTML "قصير ومخصّص" آمن: يدعم مجموعة وسوم محدودة فقط ويُصدر AnnotatedString
 * تُعرض عبر Text() الأصلي في Compose — وليس عبر WebView — لذلك لا يوجد أي خطر تنفيذ سكربت.
 *
 * الوسوم المدعومة: <b>/<strong> تعريض، <i>/<em> مائل، <u> تسطير، <br> سطر جديد،
 * <span style="color:#RRGGBB"> لون نص، وأي وسم آخر أو محتواه (script, style, ...) يُهمَل ويُعرض كنص عادي مُنقّى.
 *
 * الحد الأقصى للطول: MAX_CUSTOM_HTML_LENGTH حرفًا لمنع إثقال الفقرة.
 */
object SafeHtml {

    const val MAX_CUSTOM_HTML_LENGTH = 300

    private val ALLOWED_TAGS = setOf("b", "strong", "i", "em", "u", "br", "span")

    // وسوم خطيرة تُزال بمحتواها بالكامل قبل أي معالجة أخرى
    private val DANGEROUS_TAG_REGEX = Regex(
        "<(script|style|iframe|object|embed|link|meta|form|onclick)[^>]*>.*?</\\1>|<(script|style|iframe|object|embed|link|meta|form)[^>]*/?>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val TAG_REGEX = Regex("<(/?)([a-zA-Z]+)([^>]*)>")
    private val COLOR_ATTR_REGEX = Regex("color:\\s*(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3})")

    fun sanitizeInput(raw: String): String =
        raw.take(MAX_CUSTOM_HTML_LENGTH).replace(DANGEROUS_TAG_REGEX, "")

    /** يحوّل النص المُدخل (بعد التقييد) إلى AnnotatedString قابل للعرض مباشرة. */
    fun render(raw: String): AnnotatedString {
        val cleaned = sanitizeInput(raw)
        val builder = AnnotatedString.Builder()
        var cursor = 0
        val openSpans = ArrayDeque<Pair<String, Int>>() // اسم الوسم -> بداية الفتح داخل البناء (للتتبع فقط)
        val styleStack = ArrayDeque<SpanStyle>()

        fun currentMergedStyle(): SpanStyle {
            var style = SpanStyle()
            for (s in styleStack) {
                style = style.merge(s)
            }
            return style
        }

        for (match in TAG_REGEX.findAll(cleaned)) {
            val literalStart = cursor
            val literalText = cleaned.substring(literalStart, match.range.first)
            if (literalText.isNotEmpty()) {
                if (styleStack.isEmpty()) {
                    builder.append(literalText)
                } else {
                    builder.withStyle(currentMergedStyle()) { append(literalText) }
                }
            }
            cursor = match.range.last + 1

            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()
            val attrs = match.groupValues[3]

            if (tagName !in ALLOWED_TAGS) continue // وسم غير مسموح: يُتجاهل الوسم نفسه فقط، نصّه يبقى كنص عادي بالأعلى

            when (tagName) {
                "br" -> builder.append("\n")
                "b", "strong" -> {
                    if (!isClosing) styleStack.addLast(SpanStyle(fontWeight = FontWeight.Bold))
                    else if (styleStack.isNotEmpty()) styleStack.removeLastOrNull()
                }
                "i", "em" -> {
                    if (!isClosing) styleStack.addLast(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    else if (styleStack.isNotEmpty()) styleStack.removeLastOrNull()
                }
                "u" -> {
                    if (!isClosing) styleStack.addLast(SpanStyle(textDecoration = TextDecoration.Underline))
                    else if (styleStack.isNotEmpty()) styleStack.removeLastOrNull()
                }
                "span" -> {
                    if (!isClosing) {
                        val colorMatch = COLOR_ATTR_REGEX.find(attrs)
                        val color = colorMatch?.groupValues?.get(1)?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                        styleStack.addLast(if (color != null) SpanStyle(color = color) else SpanStyle())
                    } else if (styleStack.isNotEmpty()) styleStack.removeLastOrNull()
                }
            }
        }
        val remaining = cleaned.substring(cursor)
        if (remaining.isNotEmpty()) {
            if (styleStack.isEmpty()) builder.append(remaining)
            else builder.withStyle(currentMergedStyle()) { append(remaining) }
        }
        return builder.toAnnotatedString()
    }
}
