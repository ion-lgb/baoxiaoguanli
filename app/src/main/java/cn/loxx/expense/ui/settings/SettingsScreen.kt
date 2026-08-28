package cn.loxx.expense.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.local.CategoryEntity
import cn.loxx.expense.data.webdav.WebDavClient
import cn.loxx.expense.ui.component.CategoryIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ExpenseApp
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            app.container.categoryRepository,
            app.container.settingsRepository,
            app.container.syncManager,
        )
    }
    val settings = viewModel.settingsRepository
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(CategoryIcons.selectable.first().first) }
    var renameTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var userNameField by remember { mutableStateOf(settings.userName) }
    var departmentField by remember { mutableStateOf(settings.department) }

    var showResult by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    fun clientOrNull(): WebDavClient? {
        if (settings.webdavUrl.isBlank()) {
            resultTitle = "WebDAV 配置"
            resultText = "请先填写服务器地址"
            showResult = true
            return null
        }
        return WebDavClient(settings.webdavUrl, settings.webdavUser, settings.webdavPass)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("个人信息") {
                OutlinedTextField(
                    value = userNameField,
                    onValueChange = { userNameField = it; settings.userName = it },
                    label = { Text("报销人姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = departmentField,
                    onValueChange = { departmentField = it; settings.department = it },
                    label = { Text("所属部门") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard("分类管理") {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = CategoryIcons.fromName(category.icon),
                            contentDescription = category.icon,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (category.isBuiltin) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "内置分类",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    renameTarget = category
                                    renameText = category.name
                                },
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "重命名")
                            }
                            IconButton(onClick = { deleteTarget = category }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        newCategoryName = ""
                        selectedIcon = CategoryIcons.selectable.first().first
                        showAddCategory = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("新增分类")
                }
            }

            SectionCard("WebDAV 配置") {
                OutlinedTextField(
                    value = settings.webdavUrl,
                    onValueChange = { settings.webdavUrl = it },
                    label = { Text("服务器地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.webdavUser,
                    onValueChange = { settings.webdavUser = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.webdavPass,
                    onValueChange = { settings.webdavPass = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val client = clientOrNull() ?: return@Button
                            viewModel.testConnection(client) { ok ->
                                resultTitle = "测试连接"
                                resultText = if (ok) "连接成功" else "连接失败"
                                showResult = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("测试连接") }
                    Button(
                        onClick = {
                            val client = clientOrNull() ?: return@Button
                            viewModel.backup(client) { error ->
                                resultTitle = "立即备份"
                                resultText = if (error == null) "备份成功" else "备份失败：$error"
                                showResult = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("立即备份") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val client = clientOrNull() ?: return@Button
                            viewModel.restore(client) { error ->
                                resultTitle = "恢复备份"
                                resultText = if (error == null) "恢复成功" else "恢复失败：$error"
                                showResult = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("恢复备份") }
                    Button(
                        onClick = {
                            val client = clientOrNull() ?: return@Button
                            viewModel.listBackups(client) { backups ->
                                resultTitle = "备份列表"
                                resultText =
                                    if (backups.isEmpty()) "暂无备份" else backups.joinToString("\n")
                                showResult = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("备份列表") }
                }

            }

            SectionCard("开源与许可证") {
                Text(
                    text = "Copyright © 2026 ion-lgb",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "本应用按 GNU AGPL v3.0 only 发布，不提供任何担保。PDF 功能使用 iText 7 Community（AGPLv3）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/ion-lgb/baoxiaoguanli"),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("查看源码与许可证")
                }
            }
        }
    }
    deleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除分类？") },
            text = { Text("仅未被费用使用的自定义分类可以删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category) { error ->
                            resultTitle = "删除分类"
                            resultText = error ?: "删除成功"
                            showResult = true
                        }
                        deleteTarget = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("新增分类") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("分类名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CategoryIcons.selectable.forEach { (name, vector) ->
                            val selected = name == selectedIcon
                            IconButton(onClick = { selectedIcon = name }) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = name,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCategory(newCategoryName.trim(), selectedIcon)
                        showAddCategory = false
                    },
                    enabled = newCategoryName.isNotBlank(),
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) { Text("取消") }
            },
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTarget?.let { viewModel.renameCategory(it, renameText.trim()) }
                        renameTarget = null
                    },
                    enabled = renameText.isNotBlank(),
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
        )
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false },
            title = { Text(resultTitle) },
            text = { Text(resultText) },
            confirmButton = {
                TextButton(onClick = { showResult = false }) { Text("确定") }
            },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
