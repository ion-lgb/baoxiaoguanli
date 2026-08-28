package cn.loxx.expense.ui.expense

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.model.AmountFormatter
import cn.loxx.expense.data.ocr.ReceiptOcr
import cn.loxx.expense.ui.component.CategoryIcons
import cn.loxx.expense.ui.component.DateFormats
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Create/edit an expense in a bottom sheet. [expenseId] 0 creates a new
 * expense; otherwise the given expense is loaded for editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseFormSheet(
    tripId: Long,
    expenseId: Long,
    tripStartDate: Long,
    tripEndDate: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ExpenseApp
    val viewModel: AddEditExpenseViewModel = viewModel(key = "expense-form-$expenseId") {
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
    var date by remember {
        mutableStateOf(defaultExpenseDate(System.currentTimeMillis(), tripStartDate, tripEndDate))
    }
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
    val scope = rememberCoroutineScope()
    var ocrHint by remember { mutableStateOf<String?>(null) }

    fun fillFromOcr(uri: Uri, isPdf: Boolean) {
        scope.launch {
            val parsed = if (isPdf) {
                ReceiptOcr.recognizePdf(context, uri)
            } else {
                ReceiptOcr.recognize(context, uri)
            } ?: return@launch
            val found = mutableListOf<String>()
            parsed.amountCents?.let { cents ->
                if (amountText.isBlank()) {
                    amountText = AmountFormatter.formatCents(cents)
                    found.add("¥${AmountFormatter.formatCents(cents)}")
                }
            }
            parsed.dateMillis?.let { millis ->
                date = millis
                found.add(DateFormats.day(millis))
            }
            if (description.isBlank() && !parsed.description.isNullOrBlank()) {
                description = parsed.description
            }
            ocrHint = if (found.isEmpty()) null else "已从凭证识别：${found.joinToString(" · ")}"
            delay(4_000)
            if (ocrHint?.startsWith("已从凭证识别") == true) ocrHint = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraUri?.let { uri ->
                pendingReceipts = pendingReceipts + PendingReceipt(uri, "image", "照片.jpg")
                fillFromOcr(uri, isPdf = false)
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris ->
        pendingReceipts = pendingReceipts + uris.map { PendingReceipt(it, "image", "图片.jpg") }
        uris.firstOrNull()?.let { fillFromOcr(it, isPdf = false) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingReceipts = pendingReceipts + PendingReceipt(uri, "pdf", "文档.pdf")
            fillFromOcr(uri, isPdf = true)
        }
    }

    fun takePhoto() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val photoFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "cn.loxx.expense.fileprovider", photoFile)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Full expansion: a form must show all its fields at once instead of
    // forcing a two-step (expand sheet, then scroll) interaction.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expenseId > 0) "编辑费用" else "记一笔",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    textStyle = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    style = TextStyle(
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Text(
                text = "金额（元）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(20.dp))
            Text("分类", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = DateFormats.day(date),
                    onValueChange = {},
                    label = { Text("日期") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("凭证", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.receipts.forEach { receipt ->
                    ReceiptBox(
                        model = File(context.filesDir, receipt.filePath),
                        isPdf = receipt.fileType == "pdf",
                        onRemove = { viewModel.deleteReceipt(receipt) },
                    )
                }
                pendingReceipts.forEachIndexed { index, pending ->
                    ReceiptBox(
                        model = pending.uri,
                        isPdf = pending.fileType == "pdf",
                        onRemove = {
                            pendingReceipts = pendingReceipts.filterIndexed { i, _ -> i != index }
                        },
                    )
                }
                AddReceiptTile(onClick = { showAddOptions = true })
            }

            ocrHint?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
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
                        ) {
                            onSaved()
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (expenseId > 0) "保存修改" else "保存")
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
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
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

/** Defaults to today, clamped into the trip range when today lies outside it. */
internal fun defaultExpenseDate(today: Long, tripStart: Long, tripEnd: Long): Long =
    when {
        today < tripStart -> tripStart
        tripEnd > 0 && today > tripEnd -> tripEnd
        else -> today
    }

@Composable
private fun AddReceiptTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "添加凭证")
    }
}

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
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp)),
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
