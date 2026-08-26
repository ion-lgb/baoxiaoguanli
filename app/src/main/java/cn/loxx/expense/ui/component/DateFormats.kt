package cn.loxx.expense.ui.component

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormats {
    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayMonth = SimpleDateFormat("M月d日", Locale.getDefault())

    fun day(millis: Long): String = day.format(Date(millis))

    fun dayMonth(millis: Long): String = dayMonth.format(Date(millis))
}
