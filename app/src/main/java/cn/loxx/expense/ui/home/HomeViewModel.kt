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

data class HomeUiState(
    val trips: List<TripWithTotal> = emptyList(),
)

class HomeViewModel(private val tripRepository: TripRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = tripRepository.getTripsWithTotal()
        .map { HomeUiState(it) }
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
