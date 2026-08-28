package cn.loxx.expense

import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.model.TripWithTotal
import cn.loxx.expense.ui.home.summarizeHome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HomeSummaryTest {
    private fun trip(
        id: Long,
        status: String,
        startYear: Int,
        totalCents: Long,
    ): TripWithTotal {
        val calendar = Calendar.getInstance()
        calendar.set(startYear, Calendar.MARCH, 10, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return TripWithTotal(
            trip = TripEntity(
                id = id,
                title = "trip$id",
                destination = "上海",
                startDate = calendar.timeInMillis,
                endDate = 0L,
                status = status,
                note = "",
                createdAt = calendar.timeInMillis,
                updatedAt = calendar.timeInMillis,
            ),
            totalCents = totalCents,
        )
    }

    @Test
    fun summarize_countsOngoing_andAggregatesByYearAndPending() {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val trips = listOf(
            trip(1, status = "ongoing", startYear = thisYear, totalCents = 10_000),
            trip(2, status = "completed", startYear = thisYear, totalCents = 5_500),
            trip(3, status = "reported", startYear = thisYear, totalCents = 2_000),
            trip(4, status = "ongoing", startYear = 2020, totalCents = 99_000),
        )

        val summary = summarizeHome(trips)

        assertEquals(2, summary.ongoingCount)
        // 今年累计 includes reported trips — money spent is money spent
        assertEquals(17_500L, summary.yearTotalCents)
        // reported trips are excluded from pending
        assertEquals(114_500L, summary.pendingTotalCents)
    }

    @Test
    fun summarize_emptyList() {
        assertEquals(cn.loxx.expense.ui.home.HomeSummary(), summarizeHome(emptyList()))
    }
}
