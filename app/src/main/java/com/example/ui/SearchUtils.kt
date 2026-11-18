package com.example.ui

import com.example.data.PersonRecord
import java.text.Normalizer
import java.util.Locale

private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
private val phoneCompactRegex = Regex("[\\s\\-()./]+")

fun normalizeHungarianSearch(text: String): String {
    val decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
    return combiningMarksRegex.replace(decomposed, "").lowercase(Locale.forLanguageTag("hu"))
}

fun PersonRecord.matchesQuery(rawQuery: String): Boolean {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return true

    val normalizedQuery = normalizeHungarianSearch(trimmed)
    val compactQuery = phoneCompactRegex.replace(normalizedQuery, "").removePrefix("#")

    if (normalizeHungarianSearch(name).contains(normalizedQuery)) return true
    if (note.isNotBlank() && normalizeHungarianSearch(note).contains(normalizedQuery)) return true

    val lockerMatches = lockerNumbers.any { locker ->
        val normLocker = normalizeHungarianSearch(locker)
        normLocker.contains(normalizedQuery) ||
            (compactQuery.isNotEmpty() && normLocker.contains(compactQuery))
    }
    if (lockerMatches) return true

    val phoneMatches = phoneNumbers.any { phone ->
        val normPhone = normalizeHungarianSearch(phone)
        val compactPhone = phoneCompactRegex.replace(normPhone, "")
        normPhone.contains(normalizedQuery) ||
            (compactQuery.isNotEmpty() && compactPhone.contains(compactQuery))
    }
    return phoneMatches
}

fun extractNaturalLockerSortKey(lockerNumber: String): Pair<Int, String> {
    val digits = lockerNumber.takeWhile { it.isDigit() }
    val num = digits.toIntOrNull() ?: Int.MAX_VALUE
    return num to lockerNumber.lowercase(Locale.forLanguageTag("hu"))
}

fun extractInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.forLanguageTag("hu"))
        else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.forLanguageTag("hu"))
    }
}
