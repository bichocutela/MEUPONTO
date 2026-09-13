package com.bichocutela.meuponto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bichocutela.meuponto.domain.Punch
import com.bichocutela.meuponto.domain.PunchType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ManualPunchEditor(
    punches: List<Punch>,
    onSave: (List<Punch>) -> Unit
) {
    var draft by remember(punches) { mutableStateOf(punches) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CORRIGIR PONTOS DE HOJE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Ajuste os horários minuto a minuto. A ordem Entrada → Almoço → Retorno → Saída é preservada.",
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 12.sp
        )

        PunchType.entries.forEachIndexed { index, type ->
            val punch = draft.getOrNull(index)?.takeIf { it.type == type }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label(type), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                    Text(
                        punch?.time?.format(formatter) ?: "--:--",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                }

                if (punch != null) {
                    OutlinedButton(onClick = { draft = adjust(draft, index, -1) }) {
                        Text("−", color = Color.White)
                    }
                    OutlinedButton(onClick = { draft = adjust(draft, index, 1) }) {
                        Text("+", color = Color.White)
                    }
                }
            }
        }

        if (draft.isNotEmpty()) {
            OutlinedButton(
                onClick = { draft = draft.dropLast(1) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REMOVER ÚLTIMA BATIDA", color = Color.White)
            }
        }

        if (draft.size < PunchType.entries.size) {
            OutlinedButton(
                onClick = {
                    val type = PunchType.entries[draft.size]
                    val suggested = draft.lastOrNull()?.time?.plusMinutes(defaultGap(type))
                        ?: LocalTime.now().withSecond(0).withNano(0)
                    draft = draft + Punch(type, suggested)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ADICIONAR ${label(PunchType.entries[draft.size]).uppercase()}", color = Color.White)
            }
        }

        Spacer(Modifier.height(2.dp))
        Button(
            onClick = { onSave(draft) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SALVAR CORREÇÕES", fontWeight = FontWeight.Bold)
        }
    }
}

private fun adjust(punches: List<Punch>, index: Int, delta: Long): List<Punch> {
    val current = punches[index]
    val previous = punches.getOrNull(index - 1)?.time
    val next = punches.getOrNull(index + 1)?.time
    val candidate = current.time.plusMinutes(delta)

    if (previous != null && !candidate.isAfter(previous)) return punches
    if (next != null && !candidate.isBefore(next)) return punches

    return punches.toMutableList().also {
        it[index] = current.copy(time = candidate)
    }
}

private fun defaultGap(type: PunchType): Long = when (type) {
    PunchType.ENTRY -> 0L
    PunchType.LUNCH_OUT -> 240L
    PunchType.LUNCH_RETURN -> 70L
    PunchType.EXIT -> 240L
}

private fun label(type: PunchType): String = when (type) {
    PunchType.ENTRY -> "Entrada"
    PunchType.LUNCH_OUT -> "Saída para almoço"
    PunchType.LUNCH_RETURN -> "Retorno do almoço"
    PunchType.EXIT -> "Saída"
}
