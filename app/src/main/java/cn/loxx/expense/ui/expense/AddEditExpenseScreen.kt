package cn.loxx.expense.ui.expense

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.model.AmountFormatter
import cn.loxx.expense.ui.component.CategoryIcons
import cn.loxx.expense.ui.component.DateFormats
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    tripId: Long,
    expenseId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ExpenseApp
    val viewModel: AddEditExpenseViewModel = viewModel {
        AddEditExpenseViewModel(
            tripId,
            expenseId,
            app.container.expenseRepository,
            app.container.categoryRepository,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var pendingReceipts by remember { mutableStateOf(listOf<PendingReceipt>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.expense) {
        val e = uiState.expense
        if (e != null && !initialized) {
            amountText = AmountFormatter.formatCents(e.amountCents)
            categoryId = e.categoryId
            description = e.description
            date = e.date
            initialized = true
        }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraUri?.let { uri ->
                pendingReceipts = pendingReceipts + PendingReceipt(uri, "image", "照片.jpg")
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris ->
        pendingReceipts = pendingReceipts + uris.map { PendingReceipt(it, "image", "图片.jpg") }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingReceipts = pendingReceipts + PendingReceipt(uri, "pdf", "文档.pdf")
        }
    }

    fun takePhoto() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val photoFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "cn.loxx.expense.fileprovider", photoFile)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId > 0) "编辑费用" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val cents = AmountFormatter.parseToCents(amountText)
                    when {
                        cents == null || cents <= 0 -> error = "请输入有效金额"
                        categoryId == null -> error = "请选择分类"
                        else -> viewModel.save(
                            cents,
                            categoryId!!,
                            description.trim(),
                            date,
                            pendingReceipts,
                            onBack,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("保存")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("金额（元）") },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            Text("分类", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(
                                CategoryIcons.fromName(cat.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                placeholder = { Text("打车去机场") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = DateFormats.day(date),
                onValueChange = {},
                label = { Text("日期") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
            )
            Spacer(Modifier.height(24.dp))

            Text("凭证", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.receipts, key = { it.id }) { receipt ->
                    ReceiptBox(
                        model = File(context.filesDir, receipt.filePath),
                        isPdf = receipt.fileType == "pdf",
                        onRemove = { viewModel.deleteReceipt(receipt) },
                    )
                }
                items(pendingReceipts.size) { index ->
                    val p = pendingReceipts[index]
                    ReceiptBox(
                        model = p.uri,
                        isPdf = p.fileType == "pdf",
                        onRemove = { pendingReceipts = pendingReceipts.filterIndexed { i, _ -> i != index } },
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showAddOptions = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "添加凭证")
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { date = it }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showAddOptions) {
        AlertDialog(
            onDismissRequest = { showAddOptions = false },
            title = { Text("添加凭证") },
            text = {
                Column {
                    DialogOption(Icons.Filled.PhotoCamera, "拍照") { showAddOptions = false; takePhoto() }
                    DialogOption(Icons.Filled.PhotoLibrary, "从相册选择") {
                        showAddOptions = false
                        galleryLauncher.launch(PickVisualMediaRequestDefaults())
                    }
                    DialogOption(Icons.Filled.PictureAsPdf, "选择 PDF") {
                        showAddOptions = false
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                }
            },
            confirmButton = {},
        )
    }
}

private fun PickVisualMediaRequestDefaults(): androidx.activity.result.PickVisualMediaRequest =
    androidx.activity.result.PickVisualMediaRequest(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
    )

@Composable
private fun DialogOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun ReceiptBox(
    model: Any,
    isPdf: Boolean,
    onRemove: () -> Unit,
) {
    Box {
        if (isPdf) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text("PDF", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            AsyncImage(
                model = model,
                contentDescription = "凭证",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = "×",
            color = MaterialTheme.colorScheme.onError,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove)
                .padding(horizontal = 6.dp),
        )
    }
}
