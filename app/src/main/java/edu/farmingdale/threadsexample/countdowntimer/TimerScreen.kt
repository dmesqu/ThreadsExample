package edu.farmingdale.threadsexample.countdowntimer

import TimerViewModel
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.NumberPicker
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DecimalFormat
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    timerViewModel: TimerViewModel = viewModel()
) {
    val totalMillis = timerViewModel.totalMillis
    val remainingMillis = timerViewModel.remainingMillis

    // progress value for the circular indicator
    val targetProgress =
        if (timerViewModel.isRunning && totalMillis > 0L) {
            remainingMillis.toFloat() / totalMillis.toFloat()
        } else {
            0f
        }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "timerProgress"
    )
    //my solution todo5

    val isLastTenSeconds =
        timerViewModel.isRunning && remainingMillis in 1_000L..10_000L
    //my solution todo8

    var hasPlayedSound by remember { mutableStateOf(false) }

    LaunchedEffect(remainingMillis, totalMillis, timerViewModel.isRunning) {
        if (remainingMillis == 0L && totalMillis > 0L && !hasPlayedSound) {
            hasPlayedSound = true

            Log.d("TimerSound", "Playing timer finished sound (foreground)")
            //my solution todo7

            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            toneGen.release()
        }

        if (timerViewModel.isRunning) {
            // Allow beep again next time
            hasPlayedSound = false
        }
    }
    //my solution todo7

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .padding(20.dp)
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            if (timerViewModel.isRunning && totalMillis > 0L) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(240.dp)
                )
                //my solution todo5
            }

            Text(
                text = timerText(timerViewModel.remainingMillis),
                fontSize = 56.sp,                // still larger than original 40.sp
                //my solution todo4
                color = if (isLastTenSeconds) Color.Red else Color.Unspecified,
                fontWeight = if (isLastTenSeconds) FontWeight.Bold else FontWeight.Normal,
                //my solution todo8
                maxLines = 1,
                softWrap = false
                //my solution todo4
            )

        }
        TimePicker(
            hour = timerViewModel.selectedHour,
            min = timerViewModel.selectedMinute,
            sec = timerViewModel.selectedSecond,
            onTimePick = timerViewModel::selectTime
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = modifier.padding(top = 50.dp)
        ) {
            if (timerViewModel.isRunning) {
                Button(
                    onClick = timerViewModel::cancelTimer,
                    modifier = modifier
                ) {
                    Text("Cancel")
                }
            } else {
                Button(
                    enabled = timerViewModel.selectedHour +
                            timerViewModel.selectedMinute +
                            timerViewModel.selectedSecond > 0,
                    onClick = timerViewModel::startTimer,
                    modifier = modifier
                ) {
                    Text("Start")
                }
            }

            Button(
                onClick = timerViewModel::resetTimer,
                enabled = timerViewModel.totalMillis > 0L,
                modifier = modifier.padding(start = 16.dp)
            ) {
                Text("Reset")
            }
            //my solution todo6
        }
    }
}

fun timerText(timeInMillis: Long): String {
    val duration: Duration = timeInMillis.milliseconds
    return String.format(
        Locale.getDefault(), "%02d:%02d:%02d",
        duration.inWholeHours, duration.inWholeMinutes % 60, duration.inWholeSeconds % 60
    )
}

@Composable
fun TimePicker(
    hour: Int = 0,
    min: Int = 0,
    sec: Int = 0,
    onTimePick: (Int, Int, Int) -> Unit = { _: Int, _: Int, _: Int -> }
) {
    // Values must be remembered for calls to onPick()
    var hourVal by remember { mutableIntStateOf(hour) }
    var minVal by remember { mutableIntStateOf(min) }
    var secVal by remember { mutableIntStateOf(sec) }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hours")
            NumberPickerWrapper(
                initVal = hourVal,
                maxVal = 99,
                onNumPick = {
                    hourVal = it
                    onTimePick(hourVal, minVal, secVal)
                }
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        ) {
            Text("Minutes")
            NumberPickerWrapper(
                initVal = minVal,
                onNumPick = {
                    minVal = it
                    onTimePick(hourVal, minVal, secVal)
                }
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Seconds")
            NumberPickerWrapper(
                initVal = secVal,
                onNumPick = {
                    secVal = it
                    onTimePick(hourVal, minVal, secVal)
                }
            )
        }
    }
}

@Composable
fun NumberPickerWrapper(
    initVal: Int = 0,
    minVal: Int = 0,
    maxVal: Int = 59,
    onNumPick: (Int) -> Unit = {}
) {
    val numFormat = NumberPicker.Formatter { i: Int ->
        DecimalFormat("00").format(i)
    }

    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                setOnValueChangedListener { _, _, newVal -> onNumPick(newVal) }
                minValue = minVal
                maxValue = maxVal
                value = initVal
                setFormatter(numFormat)
            }
        }
    )
}
