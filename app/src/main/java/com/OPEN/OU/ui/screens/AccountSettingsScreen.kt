package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * شاشة "إعدادات الحساب": تغيير كلمة المرور، التحقق بخطوتين، تعطيل الحساب مؤقتًا،
 * وحذفه نهائيًا. كل عملية حسّاسة تتطلب تأكيدًا صريحًا (وكلمة المرور عند الحاجة).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AccountSettingsViewModel = viewModel()
) {
    val room by viewModel.room.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEnable2FADialog by remember { mutableStateOf(false) }
    var showDisable2FAConfirm by remember { mutableStateOf(false) }
    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الحساب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            viewModel.currentEmail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }

            AccSectionLabel("الأمان")
            AccSettingsCard {
                AccSettingsRow(
                    icon = Icons.Filled.Key,
                    title = "تغيير كلمة المرور",
                    subtitle = null,
                    onClick = { showPasswordDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                AccSettingsRow(
                    icon = Icons.Filled.Shield,
                    title = "التحقق بخطوتين",
                    subtitle = if (room?.twoFactorEnabled == true) "مفعّل" else "غير مفعّل",
                    onClick = {
                        if (room?.twoFactorEnabled == true) showDisable2FAConfirm = true
                        else showEnable2FADialog = true
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
            AccSectionLabel("منطقة الخطر")
            AccSettingsCard {
                AccSettingsRow(
                    icon = Icons.Filled.PauseCircle,
                    title = "تعطيل الحساب مؤقتًا",
                    subtitle = "يمكنك إعادة تفعيله بتسجيل الدخول مجددًا",
                    onClick = { showDeactivateConfirm = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                AccSettingsRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "حذف الحساب نهائيًا",
                    subtitle = "لا يمكن التراجع عن هذا الإجراء",
                    danger = true,
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLoading = isLoading,
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new, confirm ->
                viewModel.changePassword(current, new, confirm)
                showPasswordDialog = false
            }
        )
    }

    if (showEnable2FADialog) {
        EnableTwoFactorDialog(
            isLoading = isLoading,
            onDismiss = { showEnable2FADialog = false },
            onConfirm = { pin, confirmPin ->
                viewModel.enableTwoFactor(pin, confirmPin)
                showEnable2FADialog = false
            }
        )
    }

    if (showDisable2FAConfirm) {
        AlertDialog(
            onDismissRequest = { showDisable2FAConfirm = false },
            title = { Text("إيقاف التحقق بخطوتين", fontWeight = FontWeight.Bold) },
            text = { Text("سيصبح حسابك أقل أمانًا. هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(onClick = { showDisable2FAConfirm = false; viewModel.disableTwoFactor() }) {
                    Text("إيقاف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDisable2FAConfirm = false }) { Text("إلغاء") } }
        )
    }

    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirm = false },
            title = { Text("تعطيل الحساب مؤقتًا", fontWeight = FontWeight.Bold) },
            text = { Text("سيُخفى حسابك عن باقي المستخدمين وسيُسجَّل خروجك الآن. يعاد تفعيله تلقائيًا فور تسجيل دخولك مجددًا.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeactivateConfirm = false
                    viewModel.deactivateAccount(onLoggedOut)
                }) {
                    Text("تعطيل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeactivateConfirm = false }) { Text("إلغاء") } }
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            isLoading = isLoading,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { password ->
                viewModel.deleteAccountPermanently(password, onLoggedOut)
            }
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (current: String, new: String, confirm: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير كلمة المرور", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("كلمة المرور الحالية") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = new, onValueChange = { new = it },
                    label = { Text("كلمة المرور الجديدة") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("تأكيد كلمة المرور الجديدة") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && current.isNotBlank() && new.isNotBlank() && confirm.isNotBlank(),
                onClick = { onConfirm(current, new, confirm) }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun EnableTwoFactorDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (pin: String, confirmPin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفعيل التحقق بخطوتين", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "حدّد رمز PIN (4 أرقام أو أكثر) سيُطلب منك بعد كلمة المرور عند كل تسجيل دخول.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                    label = { Text("رمز PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin, onValueChange = { if (it.length <= 8) confirmPin = it.filter(Char::isDigit) },
                    label = { Text("تأكيد رمز PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && pin.isNotBlank() && confirmPin.isNotBlank(),
                onClick = { onConfirm(pin, confirmPin) }
            ) { Text("تفعيل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف الحساب نهائيًا", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
        text = {
            Column {
                Text(
                    "سيتم حذف غرفتك وكل فقراتك وبياناتك نهائيًا ولا يمكن التراجع عن ذلك. أدخل كلمة المرور للتأكيد.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("كلمة المرور") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && password.isNotBlank(),
                onClick = { onConfirm(password) }
            ) { Text("حذف نهائيًا", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AccSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun AccSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) { Column(content = content) }
}

@Composable
private fun AccSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
