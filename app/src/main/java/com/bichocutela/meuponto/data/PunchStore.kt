package com.bichocutela.meuponto.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bichocutela.meuponto.domain.Punch
import com.bichocutela.meuponto.domain.PunchType
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.punchDataStore by preferencesDataStore(name = "punches")

class PunchStore(private val context: Context) {
    private object Keys {
        val DATE = stringPreferencesKey("date")
        val PUNCHES = stringPreferencesKey("punches")
    }

    val todayPunches: Flow<List<Punch>> = context.punchDataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        if (prefs[Keys.DATE] != today) {
            emptyList()
        } else {
            decode(prefs[Keys.PUNCHES].orEmpty())
        }
    }

    suspend fun saveToday(punches: List<Punch>) {
        context.punchDataStore.edit { prefs ->
            prefs[Keys.DATE] = LocalDate.now().toString()
            prefs[Keys.PUNCHES] = encode(punches)
        }
    }

    private fun encode(punches: List<Punch>): String = punches.joinToString("|") {
        "${it.type.name}@${it.time}"
    }

    private fun decode(raw: String): List<Punch> = raw
        .split('|')
        .mapNotNull { item ->
            val parts = item.split('@')
            if (parts.size != 2) return@mapNotNull null
            runCatching {
                Punch(
                    type = PunchType.valueOf(parts[0]),
                    time = LocalTime.parse(parts[1])
                )
            }.getOrNull()
        }
}
