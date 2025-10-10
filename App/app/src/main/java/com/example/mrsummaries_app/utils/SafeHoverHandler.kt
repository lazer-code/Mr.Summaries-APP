package com.example.mrsummaries_app.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A safer alternative to PointerInteropFilter for handling S Pen hover events
 * Prevents the "LayoutCoordinate operations are only valid when isAttached is true" crash
 */
@Composable
fun SafeHoverHandler(
    onHoverEnter: () -> Unit = {},
    onHoverExit: () -> Unit = {},
    onHoverMove: (PointerInputChange) -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    // Safe hover event handling with proper exception handling
                    try {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> onHoverEnter()
                            PointerEventType.Exit -> onHoverExit()
                            PointerEventType.Move -> {
                                // Only process hover moves if we have changes
                                if (event.changes.isNotEmpty()) {
                                    onHoverMove(event.changes[0])
                                }
                            }
                            else -> { /* Ignore other event types */ }
                        }
                    } catch (e: IllegalStateException) {
                        // Safely catch and ignore the specific exception that causes the crash
                        // This happens when the view is detached during hover event processing
                    } catch (e: Exception) {
                        // Log other exceptions but don't crash
                        println("Error in hover handler: ${e.message}")
                    }
                }
            }
        }
    ) {
        content()
    }
}