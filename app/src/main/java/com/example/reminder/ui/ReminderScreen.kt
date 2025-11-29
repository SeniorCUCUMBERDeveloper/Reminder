package com.example.reminder.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.reminder.viewmodel.ReminderViewModel
import com.example.reminder.worker.ReminderWorker
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(viewModel: ReminderViewModel = viewModel()) {
    val context = viewModel.context
    val workManager = WorkManager.getInstance(context)

    var interval by remember { mutableStateOf("15") }
    var onlyCharging by remember { mutableStateOf(false) }
    var onlyFullBattery by remember { mutableStateOf(false) }

    val isIntervalValid = interval.toIntOrNull()?.let { it >= 15 } == true

    val workInfos =
        workManager.getWorkInfosForUniqueWorkLiveData("reminder_unique").observeAsState()
    val workState = workInfos.value?.firstOrNull()?.state?.name ?: "Остановлено"

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("⏰ Напоминания") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = interval,
                onValueChange = { new ->
                    interval = new.filter { it.isDigit() }.take(4)
                },
                label = { Text("Интервал (мин, ≥15)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = interval.isNotEmpty() && !isIntervalValid,
                supportingText = {
                    if (interval.isNotEmpty() && !isIntervalValid) {
                        Text("Минимальный интервал — 15 минут")
                    }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = onlyCharging, onCheckedChange = { onlyCharging = it })
                Text("Только при зарядке")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = onlyFullBattery, onCheckedChange = { onlyFullBattery = it })
                Text("Только при полной батарее")
            }

            Button(
                onClick = {
                    val minutes = interval.toIntOrNull() ?: 0
                    if (minutes < 15) {
                        Toast.makeText(
                            context,
                            "Минимальный интервал — 15 минут",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    workManager.cancelUniqueWork("reminder_unique")
                    workManager.cancelAllWorkByTag("periodic")
                    workManager.pruneWork()

                    val constraints = Constraints.Builder()
                        .setRequiresCharging(onlyCharging)
                        .build()

                    val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                        minutes.toLong(), TimeUnit.MINUTES
                    )
                        .setInputData(
                            workDataOf(
                                "interval" to minutes,
                                "onlyCharging" to onlyCharging,
                                "onlyFullBattery" to onlyFullBattery
                            )
                        )
                        .setConstraints(constraints)
                        .addTag("periodic")
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        "reminder_unique",
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        workRequest
                    )

                    Toast.makeText(context, "Напоминание запущено ✅", Toast.LENGTH_SHORT).show()
                },
                enabled = isIntervalValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚀 Старт")
            }

            Button(
                onClick = {
                    workManager.cancelUniqueWork("reminder_unique")
                    workManager.cancelAllWorkByTag("periodic")
                    workManager.pruneWork()
                    Toast.makeText(context, "Все задачи остановлены 🧹", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("🛑 Стоп")
            }

            var manualWorkState by remember { mutableStateOf<String?>(null) }

            Button(
                onClick = {
                    val minutes = interval.toIntOrNull() ?: 0
                    if (minutes < 15) {
                        Toast.makeText(
                            context,
                            "Минимальный интервал — 15 минут",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val constraints = Constraints.Builder()
                        .setRequiresCharging(onlyCharging)
                        .build()

                    val oneTimeWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setConstraints(constraints)
                        .setInputData(
                            workDataOf(
                                "interval" to minutes,
                                "onlyCharging" to onlyCharging,
                                "onlyFullBattery" to onlyFullBattery
                            )
                        )
                        .addTag("manual_check")
                        .build()

                    workManager.enqueue(oneTimeWork)
                    workManager.getWorkInfoByIdLiveData(oneTimeWork.id)
                        .observeForever { info ->
                            manualWorkState = info?.state?.name
                        }

                    Toast.makeText(context, "Проверка запущена ⚡", Toast.LENGTH_SHORT).show()
                },
                enabled = isIntervalValid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("⚡ Проверить сейчас")
            }

            if (manualWorkState != null) {
                val displayManualState = when (manualWorkState) {
                    "ENQUEUED" -> "Ждёт запуска ⏳"
                    "RUNNING" -> "Выполняется 🔄"
                    "SUCCEEDED" -> "Выполнено ✅"
                    "FAILED" -> "Ошибка ❌"
                    "BLOCKED" -> "Ожидает условий ⚡"
                    else -> manualWorkState
                }
                Text(
                    text = "Статус ручной проверки: $displayManualState",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            val displayState = when (workState) {
                "ENQUEUED" -> "Ждёт следующего запуска ⏳"
                "RUNNING" -> "Выполняется 🔄"
                "SUCCEEDED" -> "Выполнено ✅"
                "FAILED" -> "Ошибка ❌"
                "CANCELLED" -> "Остановлено ⛔"
                else -> workState
            }

            Text(
                text = "Текущий статус: $displayState",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
