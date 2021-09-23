package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.ui.viewstate.MapFilterViewState
import at.specure.data.repository.MapRepository
import java.text.DateFormatSymbols
import java.util.Calendar
import javax.inject.Inject

class MapFiltersViewModel @Inject constructor(private val repository: MapRepository) : BaseViewModel() {

    val state = MapFilterViewState()
    val calendar = Calendar.getInstance()
    val currentMonthNumber = calendar.get(Calendar.MONTH)
    val yearList: List<Int>
    val yearDisplayNames: List<String>
    val monthNumbersForYearHashMap: HashMap<Int, List<Int>> = HashMap()
    val monthDisplayForYearHashMap: HashMap<Int, List<String>> = HashMap()

    init {
        addStateSaveHandler(state)

        yearList = if (currentMonthNumber == Calendar.DECEMBER) {
            listOf(calendar.get(Calendar.YEAR) - 1, calendar.get(Calendar.YEAR))
        } else {
            listOf(calendar.get(Calendar.YEAR) - 2, calendar.get(Calendar.YEAR) - 1, calendar.get(Calendar.YEAR))
        }

        yearDisplayNames = yearList.map {
            it.toString()
        }

        yearList.forEachIndexed { index, it ->
            if (index == 0 && (currentMonthNumber != Calendar.DECEMBER)) {
                monthNumbersForYearHashMap[it] = (currentMonthNumber..Calendar.DECEMBER).toList()
                monthDisplayForYearHashMap[it] = monthNumbersForYearHashMap[it]?.map { DateFormatSymbols().months[it] }!!
            } else if (index == yearList.size - 1 && (currentMonthNumber != Calendar.DECEMBER)) {
                monthNumbersForYearHashMap[it] = (Calendar.JANUARY..currentMonthNumber).toList()
                monthDisplayForYearHashMap[it] = monthNumbersForYearHashMap[it]?.map { DateFormatSymbols().months[it] }!!
            } else {
                monthNumbersForYearHashMap[it] = (Calendar.JANUARY..Calendar.DECEMBER).toList()
                monthDisplayForYearHashMap[it] = monthNumbersForYearHashMap[it]?.map { DateFormatSymbols().months[it] }!!
            }
        }
    }
}