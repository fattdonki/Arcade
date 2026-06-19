package com.arthur.arcade.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RadioGroup(
	label: String,
	value: Boolean?,
	onValueChange: (Boolean?) -> Unit
) {
	val options = listOf(
		null to "Don't change",
		true to "Always turn on",
		false to "Always turn off"
	)

	Column(Modifier.fillMaxWidth()) {
		Text(
			text = label,
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold
		)
		Spacer(modifier = Modifier.height(4.dp))
		options.forEach { (optionValue, optionLabel) ->
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { onValueChange(optionValue) }
					.padding(vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				RadioButton(
					selected = value == optionValue,
					onClick = { onValueChange(optionValue) }
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(text = optionLabel, style = MaterialTheme.typography.bodyMedium)
			}
		}
	}
}

