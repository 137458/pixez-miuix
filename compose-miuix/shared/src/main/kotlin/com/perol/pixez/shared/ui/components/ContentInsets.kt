package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Extra bottom space reserved for the main navigation chrome.
 * Secondary routes keep a small breathing room instead of inheriting the main bar height.
 */
val LocalBottomBarContentPadding = compositionLocalOf { 16.dp }
