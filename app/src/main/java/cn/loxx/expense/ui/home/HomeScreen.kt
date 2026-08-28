package cn.loxx.expense.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.model.AmountFormatter
import cn.loxx.expense.data.model.TripWithTotal
import cn.loxx.expense.ui.component.DateFormats
import cn.loxx.expense.ui.component.TripFormSheet
import cn.loxx.expense.ui.theme.GlassCard
import cn.loxx.expense.ui.theme.GlassShapes
import cn.loxx.expense.ui.theme.GlassScaffold
import cn.loxx.expense.ui.theme.rememberStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTripClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ExpenseApp
    val viewModel: HomeViewModel = viewModel { HomeViewModel(app.container.tripRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TripWithTotal?>(null) }

    GlassScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "我的行程",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建行程")
            }
        },
    ) { innerPadding ->
        if (uiState.loaded && uiState.trips.isEmpty()) {
            EmptyHome(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onCreate = { showCreate = true },
            )
        } else if (uiState.trips.isNotEmpty()) {
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
                item(key = "summary") { SummaryBar(uiState.summary) }
                items(uiState.trips, key = { it.trip.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onClick = { onTripClick(trip.trip.id) },
                        onDelete = { deleteTarget = trip },
                    )
                }
            }
        }
    }

    if (showCreate) {
        TripFormSheet(
            initial = null,
            onDismiss = { showCreate = false },
            onSubmit = { title, destination, start, end, note ->
                viewModel.createTrip(title, destination, start, end, note)
                showCreate = false
            },
        )
    }

    deleteTarget?.let { trip ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除行程？") },
            text = { Text("将同时删除该行程的全部费用和凭证，此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip(trip.trip)
                        deleteTarget = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SummaryBar(summary: HomeSummary) {
    val statusColors = rememberStatusColors()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = "${summary.ongoingCount}",
            label = "进行中行程",
            accent = statusColors.ongoing,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = "¥${AmountFormatter.formatCentsGrouped(summary.yearTotalCents)}",
            label = "今年累计",
            accent = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.4f),
        )
        StatCard(
            value = "¥${AmountFormatter.formatCentsGrouped(summary.pendingTotalCents)}",
            label = "待报销",
            accent = statusColors.reported,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, shape = GlassShapes.item, contentPadding = PaddingValues(14.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(
    trip: TripWithTotal,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, GlassShapes.card)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trip.trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(trip.trip.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${trip.trip.destination} · ${buildDateRange(trip.trip.startDate, trip.trip.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "¥${AmountFormatter.formatCentsGrouped(trip.totalCents)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val statusColors = rememberStatusColors()
    val (label, accent) = when (status) {
        "completed" -> "已完成" to statusColors.completed
        "reported" -> "已报销" to statusColors.reported
        else -> "进行中" to statusColors.ongoing
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

@Composable
private fun EmptyHome(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard(contentPadding = PaddingValues(28.dp)) {
            Icon(
                Icons.Filled.Luggage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "还没有行程",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "创建一个行程，开始记录差旅开支",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("新建行程")
            }
        }
    }
}

private fun buildDateRange(start: Long, end: Long): String {
    val startLabel = DateFormats.day(start)
    return if (end == 0L) "$startLabel 起" else "$startLabel ~ ${DateFormats.day(end)}"
}
