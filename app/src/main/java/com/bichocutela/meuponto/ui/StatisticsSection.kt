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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bichocutela.meuponto.domain.WorkStatistics
import com.bichocutela.meuponto.domain.asHourMinuteText

@Composable
fun StatisticsSection(statistics: WorkStatistics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ESTATÍSTICAS",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        if (statistics.completedDays == 0) {
            Text(
                text = "As estatísticas aparecem quando a primeira jornada for encerrada.",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 13.sp
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Dias",
                value = statistics.completedDays.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total",
                value = statistics.totalWorkedMinutes.asHourMinuteText()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Saldo",
                value = statistics.totalBalanceMinutes.asHourMinuteText(showSign = true)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Média/dia",
                value = statistics.averageWorkedMinutes.asHourMinuteText()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Positivos",
                value = statistics.positiveDays.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Negativos",
                value = statistics.negativeDays.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Na meta",
                value = statistics.onTargetDays.toString()
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "Atraso médio na entrada: ${statistics.averageEntryDelayMinutes} min",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White.copy(alpha = 0.64f), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }
}
