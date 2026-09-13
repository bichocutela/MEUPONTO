package com.bichocutela.meuponto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.bichocutela.meuponto.data.PunchStore
import com.bichocutela.meuponto.data.ScheduleStore
import com.bichocutela.meuponto.domain.Punch
import com.bichocutela.meuponto.domain.PunchType
import com.bichocutela.meuponto.domain.WorkDayCalculator
import com.bichocutela.meuponto.domain.WorkDayState
import com.bichocutela.meuponto.domain.WorkSchedule
import com.bichocutela.meuponto.domain.asHourMinuteText
import com.bichocutela.meuponto.domain.nextPunchType
import com.bichocutela.meuponto.domain.state
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MeuPontoApp() }
    }
}

@Composable
private fun MeuPontoApp() {
    val context = LocalContext.current.applicationContext
    val punchStore = remember(context) { PunchStore(context) }
    val scheduleStore = remember(context) { ScheduleStore(context) }
    val punches by punchStore.todayPunches.collectAsState(initial = emptyList())
    val schedule by scheduleStore.schedule.collectAsState(initial = WorkSchedule())
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    val state = punches.state()
    val nextPunch = state.nextPunchType()
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val transition = rememberInfiniteTransition(label = "glassPulse")
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
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
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("MEU PONTO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(statusText(state), color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)
                    Spacer(Modifier.height(20.dp))

                    GlassCard(glow = glow) {
                        Text(
                            text = punches.lastOrNull()?.time?.format(formatter) ?: "--:--",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 54.sp
                        )
                        Text(
                            text = punches.lastOrNull()?.let { punchLabel(it.type) } ?: "Nenhum ponto registrado hoje",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp
                        )

                        if (state == WorkDayState.ON_LUNCH) {
                            Spacer(Modifier.height(18.dp))
                            val expectedReturn = WorkDayCalculator.lunchExpectedReturn(punches, schedule)
                            Text(
                                text = "Retorno previsto ${expectedReturn?.format(formatter) ?: "--:--"}",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Lembrete ${schedule.reminderMinutesBefore} min antes",
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
                            modifier = Modifier.weight(1f),
                            title = "Trabalhado",
                            value = WorkDayCalculator.workedMinutes(punches).asHourMinuteText()
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = if (state == WorkDayState.FINISHED) "Saldo" else "Falta",
                            value = if (state == WorkDayState.FINISHED) {
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
                                val updated = punches + Punch(type, LocalTime.now().withSecond(0).withNano(0))
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
                    OutlinedButton(onClick = { showSettings = !showSettings }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showSettings) "FECHAR AJUSTES" else "AJUSTAR JORNADA", color = Color.White)
                    }

                    if (showSettings) {
                        Spacer(Modifier.height(12.dp))
                        SettingsCard(
                            schedule = schedule,
                            onChange = { updated -> scope.launch { scheduleStore.save(updated) } }
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Jornada ${schedule.dailyMinutes.asHourMinuteText()}  •  Almoço ${schedule.lunchMinutes.asHourMinuteText()}",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 13.sp
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
            title = "Jornada",
            value = schedule.dailyMinutes.asHourMinuteText(),
            onMinus = { onChange(schedule.copy(dailyMinutes = (schedule.dailyMinutes - 15).coerceAtLeast(60))) },
            onPlus = { onChange(schedule.copy(dailyMinutes = (schedule.dailyMinutes + 15).coerceAtMost(16 * 60))) }
        )
        SettingRow(
            title = "Almoço",
            value = schedule.lunchMinutes.asHourMinuteText(),
            onMinus = { onChange(schedule.copy(lunchMinutes = (schedule.lunchMinutes - 5).coerceAtLeast(15))) },
            onPlus = { onChange(schedule.copy(lunchMinutes = (schedule.lunchMinutes + 5).coerceAtMost(240))) }
        )
        SettingRow(
            title = "Lembrete",
            value = "${schedule.reminderMinutesBefore} min",
            onMinus = { onChange(schedule.copy(reminderMinutesBefore = previousReminder(schedule.reminderMinutesBefore))) },
            onPlus = { onChange(schedule.copy(reminderMinutesBefore = nextReminder(schedule.reminderMinutesBefore))) }
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
private fun GlassCard(glow: Float, content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.12f * glow)).padding(24.dp),
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
