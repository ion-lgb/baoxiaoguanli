package cn.loxx.expense.ui.trip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.local.ExpenseEntity
import cn.loxx.expense.data.local.ReceiptEntity
import cn.loxx.expense.data.model.AmountFormatter
import cn.loxx.expense.ui.component.CategoryIcons
import cn.loxx.expense.ui.component.DateFormats
import cn.loxx.expense.ui.component.TripFormSheet
import cn.loxx.expense.ui.expense.ExpenseFormSheet
import cn.loxx.expense.ui.theme.GlassCard
import cn.loxx.expense.ui.theme.GlassShapes
import cn.loxx.expense.ui.theme.GlassScaffold
import cn.loxx.expense.ui.theme.categoryColor
import cn.loxx.expense.ui.theme.rememberStatusColors
import coil3.compose.AsyncImage
import java.io.File

private data class CategoryShare(
    val name: String,
    val totalCents: Long,
    val fraction: Float,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    onViewReceipt: (Long) -> Unit,
    onGenerateReport: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ExpenseApp
    val viewModel: TripDetailViewModel = viewModel {
        TripDetailViewModel(
            tripId,
            app.container.tripRepository,
            app.container.expenseRepository,
            app.container.categoryRepository,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEdit by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var expandedExpenseIds by remember { mutableStateOf(setOf<Long>()) }
    var editingExpenseId by remember { mutableStateOf<Long?>(null) }
    var deleteExpenseTarget by remember { mutableStateOf<ExpenseEntity?>(null) }

    val receiptsByExpense = remember(uiState.receipts) { uiState.receipts.groupBy { it.expenseId } }
    val grouped = remember(uiState.expenses) { uiState.expenses.groupBy { DateFormats.day(it.date) } }
    val categoryShares = remember(uiState.expenses, uiState.categories) {
        buildCategoryShares(uiState.expenses, uiState.categories.associateBy { it.id })
    }

    GlassScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.trip?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑行程") },
                            onClick = { menuOpen = false; showEdit = true },
                        )
                        DropdownMenuItem(
                            text = { Text("标记完成") },
                            onClick = { menuOpen = false; viewModel.markCompleted() },
                        )
                        DropdownMenuItem(
                            text = { Text("标记已报销") },
                            onClick = { menuOpen = false; viewModel.markReported() },
                        )
                        DropdownMenuItem(
                            text = { Text("生成报销单") },
                            onClick = { menuOpen = false; onGenerateReport() },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingExpenseId = 0L },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        },
    ) { innerPadding ->
        if (uiState.trip == null) {
            Box(Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "total") {
                    TotalCard(
                        title = uiState.trip?.title.orEmpty(),
                        totalCents = uiState.totalCents,
                        shares = categoryShares,
                    )
                }
                if (uiState.expenses.isEmpty()) {
                    item(key = "empty") {
                        EmptyExpenses(onAdd = { editingExpenseId = 0L })
                    }
                } else {
                    grouped.forEach { (dateLabel, expenses) ->
                        stickyHeader(key = "header-$dateLabel") {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            )
                        }
                        items(expenses, key = { it.id }) { expense ->
                            ExpenseCard(
                                expense = expense,
                                categoryIcon = CategoryIcons.fromName(
                                    uiState.categoryIcon(expense.categoryId),
                                ),
                                receipts = receiptsByExpense[expense.id] ?: emptyList(),
                                expanded = expense.id in expandedExpenseIds,
                                onToggle = {
                                    expandedExpenseIds = if (expense.id in expandedExpenseIds) {
                                        expandedExpenseIds - expense.id
                                    } else {
                                        expandedExpenseIds + expense.id
                                    }
                                },
                                onEdit = { editingExpenseId = expense.id },
                                onDelete = { deleteExpenseTarget = expense },
                                onViewReceipt = onViewReceipt,
                            )
                        }
                    }
                }
            }
        }
    }

    editingExpenseId?.let { expenseId ->
        ExpenseFormSheet(
            tripId = tripId,
            expenseId = expenseId,
            tripStartDate = uiState.trip?.startDate ?: 0L,
            tripEndDate = uiState.trip?.endDate ?: 0L,
            onDismiss = { editingExpenseId = null },
        )
    }

    if (showEdit) {
        TripFormSheet(
            initial = uiState.trip,
            onDismiss = { showEdit = false },
            onSubmit = { title, destination, start, end, note ->
                viewModel.updateTrip(title, destination, start, end, note)
                showEdit = false
            },
        )
    }

    deleteExpenseTarget?.let { expense ->
        AlertDialog(
            onDismissRequest = { deleteExpenseTarget = null },
            title = { Text("删除这笔费用？") },
            text = { Text("关联的凭证会一并删除，此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        deleteExpenseTarget = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteExpenseTarget = null }) { Text("取消") }
            },
        )
    }
}

private fun buildCategoryShares(
    expenses: List<ExpenseEntity>,
    nameById: Map<Long, cn.loxx.expense.data.local.CategoryEntity>,
): List<CategoryShare> {
    val total = expenses.sumOf { it.amountCents }
    if (total <= 0L) return emptyList()
    return expenses
        .groupBy { it.categoryId }
        .map { (categoryId, grouped) ->
            CategoryShare(
                name = nameById[categoryId]?.name ?: "其他",
                totalCents = grouped.sumOf { it.amountCents },
                fraction = grouped.sumOf { it.amountCents }.toFloat() / total,
            )
        }
        .sortedByDescending { it.totalCents }
}

@Composable
private fun TotalCard(
    title: String,
    totalCents: Long,
    shares: List<CategoryShare>,
) {
    GlassCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "费用总额",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "¥${AmountFormatter.formatCents(totalCents)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        if (shares.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                shares.forEachIndexed { index, share ->
                    Box(
                        Modifier
                            .weight(share.fraction)
                            .fillMaxSize()
                            .background(categoryColor(index)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            shares.forEachIndexed { index, share ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(categoryColor(index), CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = share.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(share.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = "¥${AmountFormatter.formatCentsGrouped(share.totalCents)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(96.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    categoryIcon: ImageVector,
    receipts: List<ReceiptEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (Long) -> Unit,
) {
    val context = LocalContext.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.item,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (receipts.isNotEmpty()) {
                    MiniReceiptThumb(
                        receipt = receipts.first(),
                        count = receipts.size,
                        onClick = { onViewReceipt(receipts.first().id) },
                        context = context,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description.ifBlank { "（无描述）" },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = DateFormats.dayMonth(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "¥${AmountFormatter.formatCentsGrouped(expense.amountCents)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column {
                    if (receipts.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp),
                        ) {
                            receipts.forEach { receipt ->
                                ReceiptThumbnail(receipt, onClick = { onViewReceipt(receipt.id) })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(modifier = Modifier.padding(horizontal = 6.dp)) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = onDelete) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniReceiptThumb(
    receipt: ReceiptEntity,
    count: Int,
    onClick: () -> Unit,
    context: android.content.Context,
) {
    Box {
        if (receipt.fileType == "pdf") {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Receipt,
                    contentDescription = "查看凭证",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            AsyncImage(
                model = File(context.filesDir, receipt.filePath),
                contentDescription = "查看凭证",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClick),
            )
        }
        if (count > 1) {
            Text(
                text = "+${count - 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun ReceiptThumbnail(receipt: ReceiptEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    if (receipt.fileType == "pdf") {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("PDF", style = MaterialTheme.typography.labelMedium)
        }
    } else {
        AsyncImage(
            model = File(context.filesDir, receipt.filePath),
            contentDescription = "凭证",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun EmptyExpenses(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(contentPadding = PaddingValues(28.dp)) {
            Icon(
                Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "还没有费用记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每笔开支都可以附上凭证，报销时一键打包",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("记一笔")
            }
        }
    }
}
