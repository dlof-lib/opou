package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.ParagraphEmoji
import com.OPEN.OU.data.model.ParagraphPrivacy
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.ui.components.Base64Image
import com.OPEN.OU.ui.components.ImagePickerButton
import com.OPEN.OU.ui.theme.OpouAccentGreen
import com.OPEN.OU.util.ImageCodec
import com.OPEN.OU.util.ParagraphColorPalette
import com.OPEN.OU.util.SafeHtml
import com.OPEN.OU.util.toColorOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: FeedViewModel,
    currentUsername: String,
    currentAvatar: String,
    currentAvatarBase64: String = "",
    onDone: () -> Unit,
    onBack: () -> Unit,
    /** إن كانت غير null، تفتح الشاشة في وضع "تعديل" لفقرة موجودة بدل إنشاء فقرة جديدة. */
    editingPost: Post? = null,
    /** إن كانت غير null (ووضع الإنشاء وليس التعديل)، تُنشر الفقرة الجديدة كرد على هذا التعليق. */
    quotedComment: Comment? = null,
    /** إن كانت غير null (ووضع الإنشاء وليس التعديل)، تُنشر الفقرة الجديدة كمتابعة لسلسلة تبدأ من (أو تمر عبر) هذه الفقرة. */
    continuingFromPost: Post? = null
) {
    val isEditing = editingPost != null
    var text by remember(editingPost?.postId) { mutableStateOf(editingPost?.content.orEmpty()) }
    var attachedImage by remember { mutableStateOf<ImageCodec.EncodedImage?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    // ===== تنسيق الفقرة =====
    var backgroundColor by remember { mutableStateOf(editingPost?.backgroundColor.orEmpty()) }
    var selectedEmoji by remember { mutableStateOf(editingPost?.emoji ?: ParagraphEmoji.NONE) }
    var textColor by remember { mutableStateOf(editingPost?.textColor.orEmpty()) }
    var textBold by remember { mutableStateOf(editingPost?.textBold ?: false) }
    var textUnderline by remember { mutableStateOf(editingPost?.textUnderline ?: false) }
    var textBackgroundColor by remember { mutableStateOf(editingPost?.textBackgroundColor.orEmpty()) }
    var linkInput by remember { mutableStateOf("") }
    val links = remember { mutableStateListOf<String>().apply { editingPost?.links?.let { addAll(it) } } }
    var customHtml by remember { mutableStateOf(editingPost?.customHtml.orEmpty()) }

    // ===== خصوصية وجدولة =====
    var privacy by remember { mutableStateOf(ParagraphPrivacy.fromValue(editingPost?.privacy ?: "PUBLIC")) }
    var scheduledAt by remember { mutableStateOf(editingPost?.scheduledAt) }
    var showStylingSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditing -> "تعديل الفقرة"
                            quotedComment != null -> "الرد بفقرة"
                            continuingFromPost != null -> "متابعة السلسلة"
                            else -> stringResource(R.string.feed_title)
                        }
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("إلغاء") }
                },
                actions = {
                    if (!isEditing) {
                        ImagePickerButton(
                            profile = ImageCodec.ImageProfile.POST_IMAGE,
                            onImageReady = { attachedImage = it; imageError = null },
                            onError = { imageError = it }
                        )
                    }
                    TextButton(
                        onClick = {
                            if (isEditing) {
                                viewModel.editPost(
                                    postId = editingPost!!.postId,
                                    newContent = text,
                                    onDone = onDone
                                )
                            } else {
                                viewModel.publish(
                                    content = text,
                                    authorUsername = currentUsername,
                                    authorAvatar = currentAvatar,
                                    authorAvatarBase64 = currentAvatarBase64,
                                    imageBase64 = attachedImage?.base64.orEmpty(),
                                    backgroundColor = backgroundColor,
                                    emoji = selectedEmoji,
                                    textColor = textColor,
                                    textBold = textBold,
                                    textUnderline = textUnderline,
                                    textBackgroundColor = textBackgroundColor,
                                    links = links.filter { it.isNotBlank() },
                                    customHtml = SafeHtml.sanitizeInput(customHtml),
                                    privacy = privacy.name,
                                    scheduledAt = scheduledAt,
                                    replyCommentId = quotedComment?.commentId.orEmpty(),
                                    replyCommentAuthorId = quotedComment?.authorId.orEmpty(),
                                    replyCommentAuthorUsername = quotedComment?.authorUsername.orEmpty(),
                                    replyCommentContent = quotedComment?.content.orEmpty(),
                                    continueFromPost = continuingFromPost,
                                    onDone = onDone
                                )
                            }
                        },
                        enabled = (text.isNotBlank() || attachedImage != null) && !viewModel.isPosting
                    ) {
                        Text(if (isEditing) "حفظ" else stringResource(R.string.post_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (quotedComment != null && !isEditing) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "ردًا على تعليق @${quotedComment.authorUsername}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            quotedComment.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (continuingFromPost != null && !isEditing) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = OpouAccentGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpouAccentGreen.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.FormatListNumbered,
                            contentDescription = null,
                            tint = OpouAccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "متابعة سلسلة فقرات",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OpouAccentGreen
                            )
                            Text(
                                "ستُنشر هذه الفقرة كجزء تالٍ لفقرتك السابقة في نفس السلسلة.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val previewBg = backgroundColor.toColorOrNull()
            val previewTextColor = textColor.toColorOrNull() ?: MaterialTheme.colorScheme.onSurface
            val previewTextBg = textBackgroundColor.toColorOrNull()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (previewBg != null) it.background(previewBg, MaterialTheme.shapes.medium) else it }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                    .padding(4.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.new_paragraph_hint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    minLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = previewTextColor,
                        fontWeight = if (textBold) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (textUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = previewTextBg ?: Color.Transparent,
                        focusedContainerColor = previewTextBg ?: Color.Transparent
                    )
                )
            }

            if (selectedEmoji.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("الإيموجي المختار: $selectedEmoji", style = MaterialTheme.typography.labelSmall)
            }

            attachedImage?.let { image ->
                Spacer(Modifier.height(12.dp))
                Box {
                    Base64Image(
                        base64 = image.base64,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    TextButton(
                        onClick = { attachedImage = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) { Text("إزالة") }
                }
                Text(
                    text = "حجم الصورة بعد الضغط: ${image.byteSize / 1024} كيلوبايت",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            imageError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            Text(
                text = "${text.length} حرف",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))

            // ===== أزرار فتح لوحتَي التنسيق والخصوصية =====
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showStylingSheet = true }) {
                    Text("تنسيق الفقرة")
                }
                OutlinedButton(onClick = { showPrivacySheet = true }) {
                    Text("الخصوصية والجدولة")
                }
            }

            Spacer(Modifier.height(8.dp))
            PrivacySummaryRow(privacy = privacy, scheduledAt = scheduledAt)
        }
    }

    if (showStylingSheet) {
        ModalBottomSheet(onDismissRequest = { showStylingSheet = false }) {
            StylingSheetContent(
                backgroundColor = backgroundColor,
                onBackgroundColor = { backgroundColor = it },
                selectedEmoji = selectedEmoji,
                onEmoji = { selectedEmoji = it },
                textColor = textColor,
                onTextColor = { textColor = it },
                textBold = textBold,
                onTextBold = { textBold = it },
                textUnderline = textUnderline,
                onTextUnderline = { textUnderline = it },
                textBackgroundColor = textBackgroundColor,
                onTextBackgroundColor = { textBackgroundColor = it },
                links = links,
                linkInput = linkInput,
                onLinkInputChange = { linkInput = it },
                onAddLink = {
                    if (linkInput.isNotBlank()) {
                        links.add(linkInput.trim())
                        linkInput = ""
                    }
                },
                onRemoveLink = { links.remove(it) },
                customHtml = customHtml,
                onCustomHtml = { if (it.length <= SafeHtml.MAX_CUSTOM_HTML_LENGTH) customHtml = it }
            )
        }
    }

    if (showPrivacySheet) {
        ModalBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            PrivacySheetContent(
                privacy = privacy,
                onPrivacy = { privacy = it },
                scheduledAt = scheduledAt,
                onScheduledAt = { scheduledAt = it }
            )
        }
    }
}

@Composable
private fun PrivacySummaryRow(privacy: ParagraphPrivacy, scheduledAt: Long?) {
    val label = when (privacy) {
        ParagraphPrivacy.PUBLIC -> "الخصوصية: عام"
        ParagraphPrivacy.PRIVATE -> "الخصوصية: خاص"
        ParagraphPrivacy.LIMITED -> "الخصوصية: محدود (تيكرز فقط)"
        ParagraphPrivacy.CUSTOM -> "الخصوصية: مخصّص"
    }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scheduledAt != null) {
            Text(
                "سيتم النشر في: ${SimpleDateFormat("d MMM yyyy - HH:mm", Locale("ar")).format(java.util.Date(scheduledAt))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StylingSheetContent(
    backgroundColor: String,
    onBackgroundColor: (String) -> Unit,
    selectedEmoji: String,
    onEmoji: (String) -> Unit,
    textColor: String,
    onTextColor: (String) -> Unit,
    textBold: Boolean,
    onTextBold: (Boolean) -> Unit,
    textUnderline: Boolean,
    onTextUnderline: (Boolean) -> Unit,
    textBackgroundColor: String,
    onTextBackgroundColor: (String) -> Unit,
    links: List<String>,
    linkInput: String,
    onLinkInputChange: (String) -> Unit,
    onAddLink: () -> Unit,
    onRemoveLink: (String) -> Unit,
    customHtml: String,
    onCustomHtml: (String) -> Unit
) {
    Column(
        Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("تنسيق الفقرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("لون خلفية الفقرة", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        ColorSwatchRow(selected = backgroundColor, onSelect = onBackgroundColor)

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("إيموجي الفقرة", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            AssistChip(onClick = {}, label = { Text("قيد التطوير — 3 فقط حاليًا", style = MaterialTheme.typography.labelSmall) })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmojiOption(emoji = ParagraphEmoji.NONE, selected = selectedEmoji == ParagraphEmoji.NONE, onClick = { onEmoji(ParagraphEmoji.NONE) })
            ParagraphEmoji.AVAILABLE.forEach { emoji ->
                EmojiOption(emoji = emoji, selected = selectedEmoji == emoji, onClick = { onEmoji(emoji) })
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("لون النص", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        ColorSwatchRow(selected = textColor, onSelect = onTextColor)

        Spacer(Modifier.height(20.dp))
        Text("خلفية النص (تظليل)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        ColorSwatchRow(selected = textBackgroundColor, onSelect = onTextBackgroundColor)

        Spacer(Modifier.height(20.dp))
        Text("تعريض/تسطير النص", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = textBold,
                onClick = { onTextBold(!textBold) },
                label = { Text("تعريض") },
                leadingIcon = { Icon(Icons.Filled.FormatBold, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = textUnderline,
                onClick = { onTextUnderline(!textUnderline) },
                label = { Text("تسطير") },
                leadingIcon = { Icon(Icons.Filled.FormatUnderlined, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("روابط", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = linkInput,
                onValueChange = onLinkInputChange,
                placeholder = { Text("https://example.com") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAddLink) {
                Icon(Icons.Filled.Check, contentDescription = "إضافة رابط")
            }
        }
        links.forEach { link ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(link, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemoveLink(link) }) {
                    Icon(Icons.Filled.Close, contentDescription = "إزالة")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("كود HTML قصير مخصّص", style = MaterialTheme.typography.labelLarge)
        Text(
            "وسوم مسموحة فقط: b, i, u, br, span (لون). أي وسم آخر يُتجاهل تلقائيًا لأمان التطبيق.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = customHtml,
            onValueChange = onCustomHtml,
            placeholder = { Text("<b>نص عريض</b> و<span style=\"color:#0B7A4A\">ملوّن</span>") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Text(
            "${customHtml.length}/${SafeHtml.MAX_CUSTOM_HTML_LENGTH} حرف",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmojiOption(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(if (emoji.isBlank()) "—" else emoji, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ColorSwatchRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // خيار "بلا لون"
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (selected.isBlank()) 2.dp else 1.dp,
                    color = if (selected.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable { onSelect("") },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "بلا لون", modifier = Modifier.size(14.dp))
        }
        ParagraphColorPalette.PRESETS.forEach { hex ->
            val color = hex.toColorOrNull() ?: Color.Gray
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == hex) 2.dp else 1.dp,
                        color = if (selected == hex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySheetContent(
    privacy: ParagraphPrivacy,
    onPrivacy: (ParagraphPrivacy) -> Unit,
    scheduledAt: Long?,
    onScheduledAt: (Long?) -> Unit
) {
    Column(Modifier.padding(20.dp)) {
        Text("خصوصية الفقرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        PrivacyOptionRow(
            title = "عام",
            subtitle = "تظهر الفقرة للجميع في التغذية",
            selected = privacy == ParagraphPrivacy.PUBLIC,
            onClick = { onPrivacy(ParagraphPrivacy.PUBLIC) }
        )
        PrivacyOptionRow(
            title = "خاص",
            subtitle = "تظهر لك فقط",
            selected = privacy == ParagraphPrivacy.PRIVATE,
            onClick = { onPrivacy(ParagraphPrivacy.PRIVATE) }
        )
        PrivacyOptionRow(
            title = "محدود",
            subtitle = "تظهر لك وللتيكرز (متابعيك) فقط",
            selected = privacy == ParagraphPrivacy.LIMITED,
            onClick = { onPrivacy(ParagraphPrivacy.LIMITED) }
        )
        PrivacyOptionRow(
            title = "مخصّص",
            subtitle = "تُدار لاحقًا من إعدادات الفقرة — قائمة مستخدمين محدَّدة",
            selected = privacy == ParagraphPrivacy.CUSTOM,
            onClick = { onPrivacy(ParagraphPrivacy.CUSTOM) }
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Spacer(Modifier.height(16.dp))

        Text("جدولة النشر (اختياري)", style = MaterialTheme.typography.labelLarge)
        Text(
            "ستُنشر الفقرة تلقائيًا في التاريخ والوقت المحدَّدين ولن تظهر في التغذية قبل ذلك.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var pendingMillis by remember { mutableStateOf(scheduledAt ?: System.currentTimeMillis()) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(if (scheduledAt != null) SimpleDateFormat("d MMM yyyy", Locale("ar")).format(java.util.Date(scheduledAt)) else "اختر التاريخ")
            }
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text(if (scheduledAt != null) SimpleDateFormat("HH:mm", Locale("ar")).format(java.util.Date(scheduledAt)) else "اختر الوقت")
            }
            if (scheduledAt != null) {
                TextButton(onClick = { onScheduledAt(null) }) { Text("إلغاء الجدولة") }
            }
        }

        if (showDatePicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = pendingMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { datePart ->
                            val cal = Calendar.getInstance().apply { timeInMillis = pendingMillis }
                            val datePartCal = Calendar.getInstance().apply { timeInMillis = datePart }
                            cal.set(Calendar.YEAR, datePartCal.get(Calendar.YEAR))
                            cal.set(Calendar.MONTH, datePartCal.get(Calendar.MONTH))
                            cal.set(Calendar.DAY_OF_MONTH, datePartCal.get(Calendar.DAY_OF_MONTH))
                            pendingMillis = cal.timeInMillis
                            onScheduledAt(pendingMillis)
                        }
                        showDatePicker = false
                    }) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") } }
            ) { DatePicker(state = state) }
        }

        if (showTimePicker) {
            val cal = Calendar.getInstance().apply { timeInMillis = pendingMillis }
            val state = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val newCal = Calendar.getInstance().apply { timeInMillis = pendingMillis }
                        newCal.set(Calendar.HOUR_OF_DAY, state.hour)
                        newCal.set(Calendar.MINUTE, state.minute)
                        pendingMillis = newCal.timeInMillis
                        onScheduledAt(pendingMillis)
                        showTimePicker = false
                    }) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("إلغاء") } },
                text = { TimePicker(state = state) }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyOptionRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
