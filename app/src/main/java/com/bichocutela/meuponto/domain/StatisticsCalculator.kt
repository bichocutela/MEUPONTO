package com.bichocutela.meuponto.domain

data class WorkStatistics(
    val completedDays: Int,
    val totalWorkedMinutes: Int,
    val totalBalanceMinutes: Int,
    val positiveDays: Int,
    val negativeDays: Int,
    val onTargetDays: Int,
    val averageWorkedMinutes: Int,
    val averageEntryDelayMinutes: Int
)

object StatisticsCalculator {
    fun calculate(
        days: List<List<Punch>>,
        schedule: WorkSchedule
    ): WorkStatistics {
        val completed = days.filter { it.state() == WorkDayState.FINISHED }

        if (completed.isEmpty()) {
            return WorkStatistics(
                completedDays = 0,
                totalWorkedMinutes = 0,
                totalBalanceMinutes = 0,
                positiveDays = 0,
                negativeDays = 0,
                onTargetDays = 0,
                averageWorkedMinutes = 0,
                averageEntryDelayMinutes = 0
            )
        }

        val worked = completed.map { WorkDayCalculator.workedMinutes(it) }
        val balances = completed.mapNotNull { WorkDayCalculator.balanceMinutes(it, schedule) }
        val delays = completed.mapNotNull { WorkDayCalculator.entryDelayMinutes(it, schedule) }

        return WorkStatistics(
            completedDays = completed.size,
            totalWorkedMinutes = worked.sum(),
            totalBalanceMinutes = balances.sum(),
            positiveDays = balances.count { it > 0 },
            negativeDays = balances.count { it < 0 },
            onTargetDays = balances.count { it == 0 },
            averageWorkedMinutes = worked.average().toInt(),
            averageEntryDelayMinutes = if (delays.isEmpty()) 0 else delays.average().toInt()
        )
    }
}
