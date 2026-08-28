package cn.loxx.expense.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.loxx.expense.data.local.TripEntity

/** Shared create/edit form for a trip. [initial] null means create, else edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFormSheet(
    initial: TripEntity?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, destination: String, start: Long, end: Long, note: String) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var destination by remember { mutableStateOf(initial?.destination ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var startDate by remember { mutableStateOf(initial?.startDate ?: System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(initial?.endDate ?: 0L) }
    var pickingField by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (initial == null) "新建行程" else "编辑行程",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                placeholder = { Text("8月北京出差") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("目的地") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = DateFormats.day(startDate),
                        onValueChange = {},
                        label = { Text("开始日期") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { pickingField = "start" },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = if (endDate == 0L) "进行中" else DateFormats.day(endDate),
                        onValueChange = {},
                        label = { Text("结束日期") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { pickingField = "end" },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSubmit(
                        title.trim(),
                        destination.trim(),
                        startDate,
                        endDate,
                        note.trim(),
                    )
                },
                enabled = title.isNotBlank() && destination.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initial == null) "创建行程" else "保存修改")
            }
        }
    }

    if (pickingField.isNotEmpty()) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (pickingField == "start") startDate else endDate,
        )
        DatePickerDialog(
            onDismissRequest = { pickingField = "" },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            if (pickingField == "start") startDate = millis else endDate = millis
                        }
                        pickingField = ""
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pickingField = "" }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
