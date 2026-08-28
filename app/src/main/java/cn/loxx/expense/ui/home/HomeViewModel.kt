package cn.loxx.expense.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.loxx.expense.data.local.TripEntity
import cn.loxx.expense.data.model.TripWithTotal
import cn.loxx.expense.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeSummary(
    val ongoingCount: Int = 0,
    val yearTotalCents: Long = 0L,
    val pendingTotalCents: Long = 0L,
)

data class HomeUiState(
    val trips: List<TripWithTotal> = emptyList(),
    val summary: HomeSummary = HomeSummary(),
    val loaded: Boolean = false,
)

/** Pure aggregation behind the home header, kept top-level for unit testing. */
fun summarizeHome(
    trips: List<TripWithTotal>,
    thisYear: Int = Calendar.getInstance().get(Calendar.YEAR),
): HomeSummary {
    val calendar = Calendar.getInstance()
    fun startYear(trip: TripEntity): Int {
        calendar.timeInMillis = trip.startDate
        return calendar.get(Calendar.YEAR)
    }
    return HomeSummary(
        ongoingCount = trips.count { it.trip.status == "ongoing" },
        yearTotalCents = trips.filter { startYear(it.trip) == thisYear }.sumOf { it.totalCents },
        pendingTotalCents = trips.filter { it.trip.status != "reported" }.sumOf { it.totalCents },
    )
}

class HomeViewModel(private val tripRepository: TripRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = tripRepository.getTripsWithTotal()
        .map { trips -> HomeUiState(trips, summarizeHome(trips), loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createTrip(
        title: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        note: String,
    ) {
        viewModelScope.launch {
            tripRepository.createTrip(title, destination, startDate, endDate, note)
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch { tripRepository.deleteTrip(trip) }
    }
}
