package com.example.pillinTimeAndroid.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pillinTimeAndroid.ui.theme.Gray70
import com.example.pillinTimeAndroid.ui.theme.PillinTimeAndroidTheme
import com.example.pillinTimeAndroid.ui.theme.PillinTimeTheme
import com.example.pillinTimeAndroid.ui.theme.Primary60
import com.example.pillinTimeAndroid.ui.theme.White
import com.example.pillinTimeAndroid.ui.theme.shapes
import java.util.Calendar

@Composable
fun CustomWeekCalendar(
    modifier: Modifier
) {
    val calendar: Calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(
        modifier = modifier
            .background(Color.Transparent)
            .fillMaxWidth()
            .clickable(
                onClick = {  },
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, day ->
            val (dayBackground, dayText) = if (index == today) {
                Pair(Primary60, White)
            } else {
                Pair(Color.Transparent, Gray70)
            }
            CalendarDay(day, dayBackground, dayText, {})
        }
    }
}

@Composable
fun CalendarDay(
    day: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shapes.small)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clickable(
                onClick = onClick,
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Text(
            text = day,
            color = textColor,
            style = PillinTimeTheme.typography.body1Regular
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun CustomWeekCalendarPreview() {
    PillinTimeAndroidTheme {
        CustomWeekCalendar(Modifier)
    }
}