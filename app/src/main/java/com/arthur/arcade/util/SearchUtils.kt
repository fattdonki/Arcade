package com.arthur.arcade.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle


@Composable
fun highlightMatchingText(text: String, query: String): AnnotatedString {
	if (query.isBlank()) return AnnotatedString(text)

	return buildAnnotatedString {
		val lowerText = text.lowercase()
		val lowerQuery = query.lowercase()
		var startIndex = 0

		while (true) {
			val index = lowerText.indexOf(lowerQuery, startIndex)
			if (index == -1) {
				append(text.substring(startIndex))
				break
			}

			append(text.substring(startIndex, index))

			withStyle(
				SpanStyle(
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.tertiary
				)
			) {
				append(text.substring(index, index + query.length))
			}

			startIndex = index + query.length
			if (startIndex >= text.length) break
		}
	}
}