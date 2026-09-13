package com.bichocutela.meuponto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bichocutela.meuponto.data.PunchDay
import com.bichocutela.meuponto.data.PunchStore
import com.bichocutela.meuponto.data.ScheduleStore
import com.bichocutela.meuponto.domain.Punch
import com.bichocutela.meuponto.domain.PunchType
import com.bichocutela.meuponto.domain.StatisticsCalculator
import com.bichocutela.meuponto.domain.WorkDayCalculator
import com.bichocutela.meuponto.domain.WorkDayState
import com.bichocutela.meuponto.domain.WorkSchedule
import com.bichocutela.meuponto.domain.asHourMinuteText
import com.bichocutela.meuponto.domain.nextPunchType
import com.bichocutela.meuponto.domain.state
import com.bichocutela.meuponto.notifications.LunchReminderReceiver
import com.bichocutela.meuponto.notifications.LunchReminderScheduler
import com.bichocutela.meuponto.ui.ManualPunchEditor
import com.bichocutela.meuponto.ui.StatisticsSection
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MeuPontoApp() }
    }
}

private enum class Panel { TODAY, SETTINGS, HISTORY, STATISTICS, CORRECTION }

@Composable
private fun MeuPontoApp() {
    val context = LocalContext.current.applicationContext
    val punchStore = remember(context) { PunchStore(context) }
    val scheduleStore = remember(context) { ScheduleStore(context) }
    val reminderScheduler = remember(context) { LunchReminderScheduler(context) }
    val punches by punchStore.todayPunches.collectAsState(initial = emptyList())
    val history by punchStore.history.collectAsState(initial = emptyList())
    val schedule by scheduleStore.schedule.collectAsState(initial = WorkSchedule())
    val scope = rememberCoroutineScope()
    var panel by remember { mutableStateOf(Panel.TODAY) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        LunchReminderReceiver.createChannel(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val state = punches.state()
    val nextPunch = state.nextPunchType()
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val statistics = remember(history, schedule) {
        StatisticsCalculator.calculate(history.map { it.punches }, schedule)
    }

    val transition = rememberInfiniteTransition(label = "glassPulse")
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Reverse),
        label = "glow"
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B17)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF3F2B96),
                                Color(0xFF151A3A),
                                Color(0xFF0B6E69),
                                Color(0xFF8B3A62)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text("MEU PONTO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(statusText(state), color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)
                    Spacer(Modifier.height(20.dp))

                    GlassCard(glow) {
                        Text(
                            punches.lastOrNull()?.time?.format(formatter) ?: "--:--",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 54.sp
                        )
                        Text(
                            punches.lastOrNull()?.let { punchLabel(it.type) } ?: "Nenhum ponto registrado hoje",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp
                        )
                        if (state == WorkDayState.ON_LUNCH) {
                            Spacer(Modifier.height(18.dp))
                            val expectedReturn = WorkDayCalculator.lunchExpectedReturn(punches, schedule)
                            Text(
                                "Retorno previsto ${expectedReturn?.format(formatter) ?: "--:--"}",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                "Lembrete ${schedule.reminderMinutesBefore} min antes",
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            Modifier.weight(1f),
                            "Trabalhado",
                            WorkDayCalculator.workedMinutes(punches).asHourMinuteText()
                        )
                        MetricCard(
                            Modifier.weight(1f),
                            if (state == WorkDayState.FINISHED) "Saldo" else "Falta",
                            if (state == WorkDayState.FINISHED) {
                                (WorkDayCalculator.balanceMinutes(punches, schedule) ?: 0).asHourMinuteText(showSign = true)
                            } else {
                                WorkDayCalculator.remainingMinutes(punches, schedule).asHourMinuteText()
                            }
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            nextPunch?.let { type ->
                                val now = LocalTime.now().withSecond(0).withNano(0)
                                val updated = punches + Punch(type, now)
                                when (type) {
                                    PunchType.LUNCH_OUT -> reminderScheduler.schedule(
                                        expectedReturn = now.plusMinutes(schedule.lunchMinutes.toLong()),
                                        minutesBefore = schedule.reminderMinutesBefore
                                    )
                                    PunchType.LUNCH_RETURN -> reminderScheduler.cancel()
                                    else -> Unit
                                }
                                scope.launch { punchStore.saveToday(updated) }
                            }
                        },
                        enabled = nextPunch != null,
                        modifier = Modifier.fillMaxWidth().height(60.dp).alpha(if (nextPunch == null) 0.55f else glow),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF17132E),
                            disabledContainerColor = Color.White.copy(alpha = 0.45f),
                            disabledContentColor = Color(0xFF17132E).copy(alpha = 0.65f)
                        )
                    ) {
                        Text(buttonText(nextPunch), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NavButton("AJUSTES", panel == Panel.SETTINGS, Modifier.weight(1f)) {
                            panel = togglePanel(panel, Panel.SETTINGS)
                        }
                        NavButton("HISTÓRICO", panel == Panel.HISTORY, Modifier.weight(1f)) {
                            panel = togglePanel(panel, Panel.HISTORY)
                        }
                        NavButton("DADOS", panel == Panel.STATISTICS, Modifier.weight(1f)) {
                            panel = togglePanel(panel, Panel.STATISTICS)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    NavButton("CORRIGIR PONTOS DE HOJE", panel == Panel.CORRECTION, Modifier.fillMaxWidth()) {
                        panel = togglePanel(panel, Panel.CORRECTION)
                    }

                    when (panel) {
                        Panel.SETTINGS -> {
                            Spacer(Modifier.height(12.dp))
                            SettingsCard(schedule) { updated -> scope.launch { scheduleStore.save(updated) } }
                        }
                        Panel.HISTORY -> {
                            Spacer(Modifier.height(12.dp))
                            HistorySection(history, schedule)
                        }
                        Panel.STATISTICS -> {
                            Spacer(Modifier.height(12.dp))
                            StatisticsSection(statistics)
                        }
                        Panel.CORRECTION -> {
                            Spacer(Modifier.height(12.dp))
                            ManualPunchEditor(punches) { corrected ->
                                scope.launch {
                                    punchStore.saveToday(corrected)
                                    reminderScheduler.cancel()
                                    if (corrected.state() == WorkDayState.ON_LUNCH) {
                                        WorkDayCalculator.lunchExpectedReturn(corrected, schedule)?.let { expectedReturn ->
                                            reminderScheduler.schedule(expectedReturn, schedule.reminderMinutesBefore)
                                        }
                                    }
                                    panel = Panel.TODAY
                                }
                            }
                        }
                        Panel.TODAY -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Jornada ${schedule.dailyMinutes.asHourMinuteText()}  •  Almoço ${schedule.lunchMinutes.asHourMinuteText()}",
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun NavButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(if (selected) "HOJE" else text, color = Color.White, fontSize = 12.sp)
    }
}

private fun togglePanel(current: Panel, target: Panel): Panel = if (current == target) Panel.TODAY else target

@Composable
private fun HistorySection(history: List<PunchDay>, schedule: WorkSchedule) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM • EEE", Locale("pt", "BR")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("HISTÓRICO / CALENDÁRIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (history.isEmpty()) {
            Text("Seu calendário começa com a primeira batida.", color = Color.White.copy(alpha = 0.66f))
        } else {
            history.take(31).forEach { day ->
                val worked = WorkDayCalculator.workedMinutes(day.punches)
                val finished = day.punches.state() == WorkDayState.FINISHED
                val balance = WorkDayCalculator.balanceMinutes(day.punches, schedule)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            day.date.format(dateFormatter).uppercase(Locale("pt", "BR")),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            day.punches.joinToString("  •  ") { it.time.format(timeFormatter) },
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(worked.asHourMinuteText(), color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (finished && balance != null) balance.asHourMinuteText(showSign = true) else "em aberto",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(schedule: WorkSchedule, onChange: (WorkSchedule) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingRow(
            "Jornada",
            schedule.dailyMinutes.asHourMinuteText(),
            { onChange(schedule.copy(dailyMinutes = (schedule.dailyMinutes - 15).coerceAtLeast(60))) },
            { onChange(schedule.copy(dailyMinutes = (schedule.dailyMinutes + 15).coerceAtMost(16 * 60))) }
        )
        SettingRow(
            "Almoço",
            schedule.lunchMinutes.asHourMinuteText(),
            { onChange(schedule.copy(lunchMinutes = (schedule.lunchMinutes - 5).coerceAtLeast(15))) },
            { onChange(schedule.copy(lunchMinutes = (schedule.lunchMinutes + 5).coerceAtMost(240))) }
        )
        SettingRow(
            "Lembrete",
            "${schedule.reminderMinutesBefore} min",
            { onChange(schedule.copy(reminderMinutesBefore = previousReminder(schedule.reminderMinutesBefore))) },
            { onChange(schedule.copy(reminderMinutesBefore = nextReminder(schedule.reminderMinutesBefore))) }
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White.copy(alpha = 0.76f), modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onMinus) { Text("−", color = Color.White) }
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
        OutlinedButton(onClick = onPlus) { Text("+", color = Color.White) }
    }
}

@Composable
private fun GlassCard(glow: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.12f * glow))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun MetricCard(modifier: Modifier, title: String, value: String) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.10f)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
    }
}

private val reminderOptions = listOf(3, 5, 7, 10, 15, 20, 30)

private fun previousReminder(current: Int): Int {
    val index = reminderOptions.indexOf(current).let { if (it < 0) 1 else it }
    return reminderOptions[(index - 1).coerceAtLeast(0)]
}

private fun nextReminder(current: Int): Int {
    val index = reminderOptions.indexOf(current).let { if (it < 0) 1 else it }
    return reminderOptions[(index + 1).coerceAtMost(reminderOptions.lastIndex)]
}

private fun statusText(state: WorkDayState): String = when (state) {
    WorkDayState.NOT_STARTED -> "Pronto para começar seu dia"
    WorkDayState.WORKING_BEFORE_LUNCH -> "Trabalhando"
    WorkDayState.ON_LUNCH -> "Intervalo de almoço"
    WorkDayState.WORKING_AFTER_LUNCH -> "De volta ao trabalho"
    WorkDayState.FINISHED -> "Jornada encerrada"
}

private fun punchLabel(type: PunchType): String = when (type) {
    PunchType.ENTRY -> "Entrada registrada"
    PunchType.LUNCH_OUT -> "Saída para almoço"
    PunchType.LUNCH_RETURN -> "Retorno do almoço"
    PunchType.EXIT -> "Saída registrada"
}

private fun buttonText(type: PunchType?): String = when (type) {
    PunchType.ENTRY -> "REGISTRAR ENTRADA"
    PunchType.LUNCH_OUT -> "SAIR PARA ALMOÇO"
    PunchType.LUNCH_RETURN -> "VOLTAR DO ALMOÇO"
    PunchType.EXIT -> "ENCERRAR JORNADA"
    null -> "JORNADA ENCERRADA"
}
