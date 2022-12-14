package sk.sandeep.runningapp.view_model

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import sk.sandeep.runningapp.repository.MainRepository
import javax.inject.Inject

@HiltViewModel
class StatisticViewModel @Inject constructor(
    private  val mainRepository: MainRepository
) :ViewModel() {

    val totalTimeRun = mainRepository.getTotalTimeInMillis()
    val totalDistance = mainRepository.getTotalDistance()
    val totalCaloriesBurned = mainRepository.getTotalCaloriesBurned()
    val totalAvgSpeed = mainRepository.getTotalAvgSpeed()

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
}