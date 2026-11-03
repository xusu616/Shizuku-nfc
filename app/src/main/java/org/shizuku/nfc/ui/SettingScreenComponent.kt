package org.shizuku.nfc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 偏好设置项开关组件（完全对齐 shizuku-ftp/SwitchPrefRow）
 */
@Composable
fun SwitchPrefRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colorBag: ColorBag = ColorBag()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.onPrimary else colorBag.darkMuted,
                checkedTrackColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
            )
        )
    }
}

/**
 * 偏好设置滑动条组件（完全对齐 shizuku-ftp/SliderRow）
 */
@Composable
fun SliderRow(
    sliderStep: Int = 7,
    minSlideValue: Float = 5f,
    maxSlideValue: Float = 40f,
    sliderTitle: String,
    sliderDescription: String,
    rememberedSliderPosition: Float,
    onSliderValueChange: (Float) -> Unit,
    colorBag: ColorBag = ColorBag()
) {
    var sliderPosition by remember(rememberedSliderPosition) { mutableFloatStateOf(rememberedSliderPosition) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = sliderTitle, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${sliderPosition.toInt()} dp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = sliderDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    onSliderValueChange(it)
                },
                valueRange = minSlideValue..maxSlideValue,
                steps = sliderStep,
                colors = if (!colorBag.useM3Color) {
                    SliderDefaults.colors(
                        thumbColor = colorBag.vibrant,
                        activeTrackColor = colorBag.vibrant,
                        inactiveTrackColor = colorBag.darkMuted,
                        activeTickColor = colorBag.muted,
                        inactiveTickColor = colorBag.vibrant
                    )
                } else {
                    SliderDefaults.colors()
                }
            )
        }
    }
}
