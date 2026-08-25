package com.OPEN.OU.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.OPEN.OU.R

/** الشريط العلوي لصفحة الحساب: عنوان، رجوع، تعديل/إعدادات لصاحب الحساب، وقائمة "حظر" للآخرين. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileTopBar(
    showBackButton: Boolean,
    onBack: () -> Unit,
    isOwnProfile: Boolean,
    onEditRoom: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    showMoreMenu: Boolean,
    onShowMoreMenuChange: (Boolean) -> Unit,
    canShowMoreMenu: Boolean,
    isBlocked: Boolean,
    onToggleBlockMenuItem: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.profile_room), fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = {
            if (isOwnProfile) {
                IconButton(onClick = onEditRoom) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.profile_edit_profile))
                }
            }
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                }
            }
            if (canShowMoreMenu) {
                Box {
                    IconButton(onClick = { onShowMoreMenuChange(true) }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { onShowMoreMenuChange(false) }) {
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) "إلغاء حظر المستخدم" else "حظر المستخدم") },
                            leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                            onClick = onToggleBlockMenuItem
                        )
                    }
                }
            }
        }
    )
}
