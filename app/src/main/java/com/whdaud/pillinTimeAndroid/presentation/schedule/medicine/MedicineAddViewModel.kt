package com.whdaud.pillinTimeAndroid.presentation.schedule.medicine

import android.util.Log
import androidx.collection.mutableIntListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.whdaud.pillinTimeAndroid.data.remote.dto.MedicineAdverse
import com.whdaud.pillinTimeAndroid.data.remote.dto.MedicineDTO
import com.whdaud.pillinTimeAndroid.data.remote.dto.request.ScheduleRequest
import com.whdaud.pillinTimeAndroid.domain.repository.MedicineRepository
import com.whdaud.pillinTimeAndroid.presentation.schedule.components.ScheduleOrderList
import com.whdaud.pillinTimeAndroid.presentation.schedule.components.schedulePages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MedicineAddViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository
) : ViewModel() {
    private val currentPageIndex = mutableIntStateOf(0)
    private val _medicineInfo = MutableStateFlow<List<MedicineDTO>>(emptyList())
    val medicineInfo = _medicineInfo.asStateFlow()
    private val _medicineInput = MutableStateFlow("")
    val medicineInput = _medicineInput.asStateFlow()
    private val _searchStatus = MutableStateFlow(false)
    val searchStatus = _searchStatus.asStateFlow()
    val initStatus = mutableStateOf(false)

    val selectedMedicine = mutableStateOf<MedicineDTO?>(null)
    val selectedDays = mutableStateListOf<Int>()
    val selectedTimes = mutableStateListOf<String>()
    val scheduleStartDate = mutableStateOf(getCurrentDateFormatted())
    val scheduleEndDate = mutableStateOf(getEndDateFormatted())
    val selectedIndex = mutableIntStateOf(-1)

    val currentUsedIndex = mutableIntListOf()
    private val _memberId = mutableIntStateOf(-1)

    fun setMemberId(memberId: Int) {
        _memberId.intValue = memberId
    }
    private fun getCurrentDateFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        return LocalDate.now().format(formatter)
    }

    private fun getEndDateFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        return LocalDate.now().plusDays(7).format(formatter)
    }
    fun getUsingCabinetIndex(memberId: Int) {
        viewModelScope.launch {
            val result = medicineRepository.getDoseSchedule(memberId)
            result.onSuccess { scheduleList ->
                Log.d("getDoseSchedule", "Succeeded to fetch: ${scheduleList.result}")
                scheduleList.result.forEach { schedule ->
                    currentUsedIndex.add(schedule.cabinetIndex)
                }
            }.onFailure {
                Log.d("getDoseSchedule", "failed to fetch: ${it.message}")
            }
        }
    }

    fun getMedicineInfo(memberId: Int, medicineName: String) {
        viewModelScope.launch {
            _searchStatus.value = true
            val trimmedName = medicineName.trim().replace(" ", "")
            val result = medicineRepository.getMedicineInfo(memberId, trimmedName)
            Log.d("getDoseSchedule", "trying to fetch: ${memberId}")
            result.onSuccess {
                Log.d("getMedicineInfo", "Succeeded to fetch: ${it.result}")
                _medicineInfo.value = it.result
            }.onFailure {
                Log.e("getMedicineInfo", "Failed to fetch: ${it.message}")
                _medicineInfo.value = emptyList()
            }
            initStatus.value = true
            _searchStatus.value = false
        }
    }

    fun postDoseSchedule(memberId: Int, navController: NavController) {
        val scheduleStartDateFormatted =
            formatDateString(scheduleStartDate.value, "yyyy년 MM월 dd일", "yyyy-MM-dd")
        val scheduleEndDateFormatted =
            formatDateString(scheduleEndDate.value, "yyyy년 MM월 dd일", "yyyy-MM-dd")

        viewModelScope.launch {
            if (selectedMedicine.value != null) {
                val medicineAdverse = selectedMedicine.value?.medicineAdverse

                val scheduleRequest =
                    ScheduleRequest(
                        memberId = memberId,
                        medicineId = selectedMedicine.value!!.medicineCode,
                        medicineName = selectedMedicine.value!!.medicineName,
                        medicineSeries = selectedMedicine.value!!.medicineSeries,
                        medicineAdverse = if(medicineAdverse != null)
                            selectedMedicine.value?.medicineAdverse else MedicineAdverse(),
                        cabinetIndex = selectedIndex.intValue,
                        weekdayList = selectedDays.map { it + 1 },
                        timeList = selectedTimes,
                        startAt = scheduleStartDateFormatted,
                        endAt = scheduleEndDateFormatted,
                    )
                val result = medicineRepository.postDoseSchedule(scheduleRequest)
                Log.e("request schedule", scheduleRequest.toString())
                result.onSuccess {
                    Log.d("getMedicineInfo", "Succeeded to post schedule: ${it.result}")
                    navController.navigate("scheduleScreen") {
                        popUpTo("scheduleScreen") {
                            inclusive = true
                        }
                    }
                }.onFailure {
                    Log.e("getMedicineInfo", "Failed to post schedule: ${it.message}")
                }
            }
        }
    }

    fun getCurrentPage(): ScheduleOrderList {
        return schedulePages.getOrElse(currentPageIndex.intValue) { schedulePages[0] }
    }

    fun getCurrentPageIndex(): Int {
        return currentPageIndex.intValue
    }

    fun getProgress(): Float {
        val totalPages = schedulePages.size
        val currentPage = currentPageIndex.intValue

        return (currentPage + 1).toFloat() / totalPages.toFloat()
    }

    fun updateInput(input: String) {
        _medicineInput.value = input
    }

    fun nextPage() {
        if (currentPageIndex.intValue < schedulePages.size - 1) {
            currentPageIndex.intValue++
        }
    }

    fun previousPage() {
        if (currentPageIndex.intValue > 0) {
            currentPageIndex.intValue--
        }
    }

    fun selectMedicine(medicine: MedicineDTO) {
        selectedMedicine.value = medicine
    }

    fun selectDays(dayIndex: Int) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
        } else {
            selectedDays.add(dayIndex)
        }
    }

    fun selectTimes(timeIndex: String) {
        if (selectedTimes.contains(timeIndex)) {
            selectedTimes.remove(timeIndex)
        } else {
            selectedTimes.add(timeIndex)
        }
    }

    fun checkButtonState(): Boolean {
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val start = LocalDate.parse(scheduleStartDate.value, formatter)
        val end = LocalDate.parse(scheduleEndDate.value, formatter)
        val isInWeek = start.plusDays(7).isBefore(end)
        val daysOfWeek = mutableListOf<Int>()
        var current = start
        while (!current.isAfter(end)) {
            daysOfWeek.add(current.dayOfWeek.value)
            current = current.plusDays(1)
        }
        val adjustedSelectedDays = selectedDays.map { it + 1 }
        val containsAll = daysOfWeek.containsAll(adjustedSelectedDays)
        return when (currentPageIndex.intValue) {
            0 -> selectedMedicine.value != null
            1 -> selectedDays.isNotEmpty()
            2 -> selectedDays.isNotEmpty() && selectedTimes.isNotEmpty()
            3 -> scheduleStartDate.value.isNotEmpty() && scheduleEndDate.value.isNotEmpty()
                && scheduleStartDate.value <= scheduleEndDate.value
                && (isInWeek || containsAll)
            4 -> selectedIndex.intValue > 0 && !currentUsedIndex.contains(selectedIndex.intValue)
            else -> true
        }
    }

    private fun formatDateString(
        dateString: String,
        fromPattern: String,
        toPattern: String
    ): String {
        val fromFormatter = DateTimeFormatter.ofPattern(fromPattern)
        val toFormatter = DateTimeFormatter.ofPattern(toPattern)
        val date = LocalDate.parse(dateString, fromFormatter)
        return date.format(toFormatter)
    }
}