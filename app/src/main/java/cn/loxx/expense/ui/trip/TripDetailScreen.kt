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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
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

    val receiptsByExpense = remember(uiState.receipts) { uiState.receipts.groupBy { it.expenseId } }
    val grouped = remember(uiState.expenses) { uiState.expenses.groupBy { DateFormats.day(it.date) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.title ?: "") },
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
                            text = { Text("生成报销单") },
                            onClick = { menuOpen = false; onGenerateReport() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TotalCard(uiState.trip?.title.orEmpty(), uiState.totalCents)
            }
            if (uiState.expenses.isEmpty()) {
                item {
                    Text(
                        text = "暂无费用记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            } else {
                grouped.forEach { (dateLabel, expenses) ->
                    stickyHeader {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
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
                            onEdit = { onEditExpense(expense.id) },
                            onDelete = { viewModel.deleteExpense(expense) },
                            onViewReceipt = onViewReceipt,
                        )
                    }
                }
            }
        }
    }

    if (showEdit && uiState.trip != null) {
        TripFormSheet(
            initial = uiState.trip,
            onDismiss = { showEdit = false },
            onSubmit = { title, destination, start, end, note ->
                viewModel.updateTrip(title, destination, start, end, note)
                showEdit = false
            },
        )
    }
}

@Composable
private fun TotalCard(title: String, totalCents: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "费用总额",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "¥${AmountFormatter.formatCents(totalCents)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onToggle),
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
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
                if (receipts.isNotEmpty()) {
                    Text(
                        text = "${receipts.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "¥${AmountFormatter.formatCents(expense.amountCents)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                if (receipts.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        receipts.forEach { receipt ->
                            ReceiptThumbnail(receipt, onClick = { onViewReceipt(receipt.id) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }
}

@Composable
private fun ReceiptThumbnail(receipt: ReceiptEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    if (receipt.fileType == "pdf") {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
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
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        )
    }
}
