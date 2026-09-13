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

data class PunchDay(
    val date: LocalDate,
    val punches: List<Punch>
)

class PunchStore(private val context: Context) {
    private object Keys {
        val DATE = stringPreferencesKey("date")
        val PUNCHES = stringPreferencesKey("punches")
        val HISTORY = stringPreferencesKey("history_v2")
    }

    val history: Flow<List<PunchDay>> = context.punchDataStore.data.map { prefs ->
        val stored = decodeHistory(prefs[Keys.HISTORY].orEmpty()).toMutableMap()
        migrateLegacyInto(stored, prefs[Keys.DATE], prefs[Keys.PUNCHES])
        stored.entries
            .sortedByDescending { it.key }
            .map { PunchDay(it.key, it.value) }
    }

    val todayPunches: Flow<List<Punch>> = history.map { days ->
        days.firstOrNull { it.date == LocalDate.now() }?.punches.orEmpty()
    }

    suspend fun saveToday(punches: List<Punch>) = saveDay(LocalDate.now(), punches)

    suspend fun saveDay(date: LocalDate, punches: List<Punch>) {
        context.punchDataStore.edit { prefs ->
            val stored = decodeHistory(prefs[Keys.HISTORY].orEmpty()).toMutableMap()
            migrateLegacyInto(stored, prefs[Keys.DATE], prefs[Keys.PUNCHES])

            if (punches.isEmpty()) {
                stored.remove(date)
            } else {
                stored[date] = punches
            }

            prefs[Keys.HISTORY] = encodeHistory(stored)
            prefs.remove(Keys.DATE)
            prefs.remove(Keys.PUNCHES)
        }
    }

    private fun migrateLegacyInto(
        target: MutableMap<LocalDate, List<Punch>>,
        legacyDate: String?,
        legacyPunches: String?
    ) {
        val date = legacyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        if (target.containsKey(date)) return
        val punches = decodePunches(legacyPunches.orEmpty())
        if (punches.isNotEmpty()) target[date] = punches
    }

    private fun encodeHistory(days: Map<LocalDate, List<Punch>>): String = days.entries
        .sortedBy { it.key }
        .joinToString("\n") { (date, punches) ->
            "$date#${encodePunches(punches)}"
        }

    private fun decodeHistory(raw: String): Map<LocalDate, List<Punch>> = raw
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val separator = line.indexOf('#')
            if (separator <= 0) return@mapNotNull null
            val date = runCatching { LocalDate.parse(line.substring(0, separator)) }.getOrNull()
                ?: return@mapNotNull null
            date to decodePunches(line.substring(separator + 1))
        }
        .toMap()

    private fun encodePunches(punches: List<Punch>): String = punches.joinToString("|") {
        "${it.type.name}@${it.time}"
    }

    private fun decodePunches(raw: String): List<Punch> = raw
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
