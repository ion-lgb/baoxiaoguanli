package cn.loxx.expense.ui.report

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.loxx.expense.ExpenseApp
import cn.loxx.expense.data.model.AmountFormatter
import cn.loxx.expense.ui.component.DateFormats
import java.io.File

private data class ExportResult(val file: File, val mime: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(tripId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ExpenseApp
    val viewModel: ReportViewModel = viewModel {
        ReportViewModel(
            tripId,
            app.container.tripRepository,
            app.container.expenseRepository,
            app.container.categoryRepository,
            app.container.settingsRepository,
            context,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    var exported by remember { mutableStateOf<ExportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pendingSaveFile = remember { mutableStateOf<File?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(exported?.mime ?: "application/octet-stream"),
    ) { uri ->
        val file = pendingSaveFile.value
        if (uri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报销单预览") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val trip = uiState.trip
            Text(
                text = trip?.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (trip != null) {
                Text(
                    text = "${DateFormats.day(trip.startDate)} ~ ${DateFormats.day(trip.endDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("费用分类汇总", style = MaterialTheme.typography.titleMedium)
                    uiState.categorySummary.forEach { summary ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = summary.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${summary.count} 笔",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "¥${AmountFormatter.formatCents(summary.totalCents)}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("合计", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "¥${AmountFormatter.formatCents(uiState.totalCents)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (isExporting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("正在生成报销单…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(
                onClick = {
                    viewModel.exportPdf(
                        onResult = { exported = ExportResult(it, "application/pdf") },
                        onError = { errorMessage = it },
                    )
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("导出 PDF 报销单")
            }

            OutlinedButton(
                onClick = {
                    viewModel.exportZip(
                        onResult = { exported = ExportResult(it, "application/zip") },
                        onError = { errorMessage = it },
                    )
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("导出 Excel + 凭证包")
            }
        }
    }

    exported?.let { result ->
        AlertDialog(
            onDismissRequest = { exported = null },
            title = { Text("导出成功") },
            text = { Text("文件已生成：${result.file.name}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exported = null
                        val uri = FileProvider.getUriForFile(
                            context,
                            "cn.loxx.expense.fileprovider",
                            result.file,
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = result.mime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享报销单"))
                    },
                ) {
                    Text("分享")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSaveFile.value = result.file
                        exported = null
                        saveLauncher.launch(result.file.name)
                    },
                ) {
                    Text("保存到文件")
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("导出失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("确定") }
            },
        )
    }
}
