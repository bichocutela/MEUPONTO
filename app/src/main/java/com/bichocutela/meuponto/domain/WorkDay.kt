package com.bichocutela.meuponto.domain

import java.time.LocalTime

enum class PunchType {
    ENTRY,
    LUNCH_OUT,
    LUNCH_RETURN,
    EXIT
}

data class Punch(
    val type: PunchType,
    val time: LocalTime
)

data class WorkSchedule(
    val expectedStart: LocalTime = LocalTime.of(8, 0),
    val dailyMinutes: Int = 8 * 60 + 48,
    val lunchMinutes: Int = 70,
    val reminderMinutesBefore: Int = 5
)

enum class WorkDayState {
    NOT_STARTED,
    WORKING_BEFORE_LUNCH,
    ON_LUNCH,
    WORKING_AFTER_LUNCH,
    FINISHED
}

fun List<Punch>.state(): WorkDayState = when (size) {
    0 -> WorkDayState.NOT_STARTED
    1 -> WorkDayState.WORKING_BEFORE_LUNCH
    2 -> WorkDayState.ON_LUNCH
    3 -> WorkDayState.WORKING_AFTER_LUNCH
    else -> WorkDayState.FINISHED
}

fun WorkDayState.nextPunchType(): PunchType? = when (this) {
    WorkDayState.NOT_STARTED -> PunchType.ENTRY
    WorkDayState.WORKING_BEFORE_LUNCH -> PunchType.LUNCH_OUT
    WorkDayState.ON_LUNCH -> PunchType.LUNCH_RETURN
    WorkDayState.WORKING_AFTER_LUNCH -> PunchType.EXIT
    WorkDayState.FINISHED -> null
}
