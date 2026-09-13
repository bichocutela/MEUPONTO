package com.bichocutela.meuponto.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bichocutela.meuponto.domain.WorkSchedule
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule")

class ScheduleStore(private val context: Context) {
    private object Keys {
        val EXPECTED_START = stringPreferencesKey("expected_start")
        val DAILY_MINUTES = intPreferencesKey("daily_minutes")
        val LUNCH_MINUTES = intPreferencesKey("lunch_minutes")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
    }

    val schedule: Flow<WorkSchedule> = context.scheduleDataStore.data.map { prefs ->
        WorkSchedule(
            expectedStart = prefs[Keys.EXPECTED_START]
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: LocalTime.of(8, 0),
            dailyMinutes = prefs[Keys.DAILY_MINUTES] ?: (8 * 60 + 48),
            lunchMinutes = prefs[Keys.LUNCH_MINUTES] ?: 70,
            reminderMinutesBefore = prefs[Keys.REMINDER_MINUTES] ?: 5
        )
    }

    suspend fun save(schedule: WorkSchedule) {
        context.scheduleDataStore.edit { prefs ->
            prefs[Keys.EXPECTED_START] = schedule.expectedStart.toString()
            prefs[Keys.DAILY_MINUTES] = schedule.dailyMinutes
            prefs[Keys.LUNCH_MINUTES] = schedule.lunchMinutes
            prefs[Keys.REMINDER_MINUTES] = schedule.reminderMinutesBefore
        }
    }
}
