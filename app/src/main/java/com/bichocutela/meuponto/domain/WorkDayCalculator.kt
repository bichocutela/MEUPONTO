package com.bichocutela.meuponto.domain

import java.time.Duration
import java.time.LocalTime

object WorkDayCalculator {
    fun lunchExpectedReturn(punches: List<Punch>, schedule: WorkSchedule): LocalTime? {
        val lunchOut = punches.firstOrNull { it.type == PunchType.LUNCH_OUT } ?: return null
        return lunchOut.time.plusMinutes(schedule.lunchMinutes.toLong())
    }

    fun lunchReminderTime(punches: List<Punch>, schedule: WorkSchedule): LocalTime? =
        lunchExpectedReturn(punches, schedule)?.minusMinutes(schedule.reminderMinutesBefore.toLong())

    fun workedMinutes(punches: List<Punch>, now: LocalTime = LocalTime.now()): Int {
        val entry = punches.firstOrNull { it.type == PunchType.ENTRY }?.time ?: return 0
        val lunchOut = punches.firstOrNull { it.type == PunchType.LUNCH_OUT }?.time
        val lunchReturn = punches.firstOrNull { it.type == PunchType.LUNCH_RETURN }?.time
        val exit = punches.firstOrNull { it.type == PunchType.EXIT }?.time

        val morningEnd = lunchOut ?: exit ?: now
        val morning = minutesBetween(entry, morningEnd)

        val afternoon = when {
            lunchReturn == null -> 0
            exit != null -> minutesBetween(lunchReturn, exit)
            else -> minutesBetween(lunchReturn, now)
        }

        return (morning + afternoon).coerceAtLeast(0)
    }

    fun remainingMinutes(punches: List<Punch>, schedule: WorkSchedule, now: LocalTime = LocalTime.now()): Int =
        (schedule.dailyMinutes - workedMinutes(punches, now)).coerceAtLeast(0)

    fun balanceMinutes(punches: List<Punch>, schedule: WorkSchedule): Int? {
        if (punches.none { it.type == PunchType.EXIT }) return null
        return workedMinutes(punches) - schedule.dailyMinutes
    }

    fun expectedExit(punches: List<Punch>, schedule: WorkSchedule): LocalTime? {
        val entry = punches.firstOrNull { it.type == PunchType.ENTRY }?.time ?: return null
        val lunchOut = punches.firstOrNull { it.type == PunchType.LUNCH_OUT }?.time
        val lunchReturn = punches.firstOrNull { it.type == PunchType.LUNCH_RETURN }?.time

        return when {
            lunchOut == null -> entry.plusMinutes(schedule.dailyMinutes.toLong() + schedule.lunchMinutes)
            lunchReturn == null -> lunchOut
                .plusMinutes(schedule.lunchMinutes.toLong())
                .plusMinutes(remainingBeforeLunch(punches, schedule).toLong())
            else -> lunchReturn.plusMinutes(remainingBeforeLunch(punches, schedule).toLong())
        }
    }

    fun entryDelayMinutes(punches: List<Punch>, schedule: WorkSchedule): Int? {
        val entry = punches.firstOrNull { it.type == PunchType.ENTRY }?.time ?: return null
        return minutesBetween(schedule.expectedStart, entry).coerceAtLeast(0)
    }

    private fun remainingBeforeLunch(punches: List<Punch>, schedule: WorkSchedule): Int {
        val entry = punches.firstOrNull { it.type == PunchType.ENTRY }?.time ?: return schedule.dailyMinutes
        val lunchOut = punches.firstOrNull { it.type == PunchType.LUNCH_OUT }?.time ?: return schedule.dailyMinutes
        return (schedule.dailyMinutes - minutesBetween(entry, lunchOut)).coerceAtLeast(0)
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Int =
        Duration.between(start, end).toMinutes().toInt()
}

fun Int.asHourMinuteText(showSign: Boolean = false): String {
    val sign = when {
        this < 0 -> "-"
        showSign && this > 0 -> "+"
        else -> ""
    }
    val absolute = kotlin.math.abs(this)
    return "$sign%02d:%02d".format(absolute / 60, absolute % 60)
}
