package com.nsoft.comunityapp.draganddrop.ui.library

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

val LocalDragTargetInfo = localDragTargetInfo<Any, Any>()

inline fun <reified T, reified K> localDragTargetInfo(): ProvidableCompositionLocal<DragTargetInfo<T, K>> {
    return compositionLocalOf { DragTargetInfo() }
}

/**
 * Clase exclusiva de Libreria
 * * Construye el DropComponent
 * * Construye el DragComponent
 * * Animation Drag and Drop Component
 * * Generic Entity Data Class
 * **/

/**Movimiento de componente**/
@RequiresApi(Build.VERSION_CODES.M)
@Composable
inline fun <reified T : CustomerPerson, reified K> DragTarget(
    modifier: Modifier = Modifier,
    rowIndex: Int,
    columnIndex: K,
    dataToDrop: T,
    vibrator: Vibrator?,
    crossinline onStart: (item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit,
    crossinline onEnd: (item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit,
    noinline content: @Composable ((isDrag: Boolean, data: Any?) -> Unit)
) {
    var currentPosition by remember {
        mutableStateOf(Offset.Zero)
    }

    val currentState = LocalDragTargetInfo.current

    Box(
        modifier = modifier
            .onGloballyPositioned {
                currentPosition = it.localToWindow(Offset.Zero)
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        if (vibrator != null) {
                            val vibrationEffect1: VibrationEffect =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    VibrationEffect.createOneShot(
                                        200,
                                        VibrationEffect.CONTENTS_FILE_DESCRIPTOR
                                    )
                                } else {
                                    Log.e("TAG", "Cannot vibrate device..")
                                    TODO("VERSION.SDK_INT < O")
                                }

                            // it is safe to cancel other
                            // vibrations currently taking place
                            vibrator.cancel()
                            vibrator.vibrate(vibrationEffect1)
                        }

                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it

                        currentState.columnPosition.from = columnIndex
                        currentState.rowPosition.from = rowIndex

                        currentState.draggableComposable = content

                        onStart(
                            dataToDrop,
                            currentState.rowPosition,
                            currentState.columnPosition as ColumnPosition<K>
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)

                        currentState.columnPosition.from = columnIndex
                        currentState.rowPosition.from = rowIndex
                    },
                    onDragEnd = {
                        currentState.dragOffset = Offset.Zero
                        currentState.columnPosition.from = columnIndex
                        currentState.rowPosition.from = rowIndex
                        currentState.isDragging = false

                        onEnd(
                            dataToDrop,
                            currentState.rowPosition,
                            currentState.columnPosition as ColumnPosition<K>
                        )
                    },
                    onDragCancel = {
                        currentState.dragOffset = Offset.Zero
                        currentState.columnPosition.from = columnIndex
                        currentState.rowPosition.from = rowIndex
                        currentState.isDragging = false

                        onEnd(
                            dataToDrop,
                            currentState.rowPosition,
                            currentState.columnPosition as ColumnPosition<K>
                        )
                    }
                )
            }
    ) {
        content(currentState.isDragging, currentState.dataToDrop)
    }
}


/**ITEM QUE SOPORTA EL SOLTAR ITEM EN SU INTERIOR**/
@Composable
inline fun <reified T, reified K> DropItem(
    modifier: Modifier,
    rowIndex: Int,
    columnIndex: K,
    content: @Composable() (BoxScope.(isInBound: Boolean, data: T?, rows: RowPosition, column: ColumnPosition<K>) -> Unit)
) {
    val dragInfo = LocalDragTargetInfo.current
    val dragPosition = dragInfo.dragPosition
    val dragOffset = dragInfo.dragOffset

    var isCurrentDropTarget by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .then(Modifier.onGloballyPositioned {
                it.boundsInWindow().let { rect ->
                    isCurrentDropTarget = if (dragInfo.isDragging) {
                        rect.contains(dragPosition + dragOffset)
                    } else false
                }
            })
    ) {
        val data = if (isCurrentDropTarget && !dragInfo.isDragging) {
            dragInfo.rowPosition.to = rowIndex
            dragInfo.columnPosition.to = columnIndex
            dragInfo.dataToDrop as T?
        } else {
            null
        }
        content(
            isCurrentDropTarget && dragInfo.columnPosition.canAdd() && data != null,
            data, dragInfo.rowPosition, dragInfo.columnPosition as ColumnPosition<K>
        )
    }
}

/**DragableScreen no acepta <T,K>
 * para remember en su lugar se deja como Any
 * para acercarlo lo mas posible a generico: only cast to T or K
 * */
@Composable
fun DraggableScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = remember {
        /** Unicamente funciona con Any */
        DragTargetInfo<Any, Any>()
    }

    CompositionLocalProvider(
        LocalDragTargetInfo provides state
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            content()
            if (state.isDragging) {
                var targetSize by remember {
                    mutableStateOf(IntSize.Zero)
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val offset = (state.dragPosition + state.dragOffset)
                            scaleX = 1.3f
                            scaleY = 1.3f
                            alpha = if (targetSize == IntSize.Zero) {
                                0f
                            } else .9f
                            translationX = offset.x.minus(targetSize.width / 3)
                            translationY = offset.y.minus(targetSize.height / 3)
                        }
                        .onGloballyPositioned {
                            targetSize = it.size
                        }
                ) {
                    state.draggableComposable?.invoke(false, null)
                }
            }
        }
    }
}

class DragTargetInfo<T, K> {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggableComposable by mutableStateOf<((@Composable (isDrag: Boolean, data: Any?) -> Unit)?)>(
        null
    )
    var dataToDrop by mutableStateOf<T?>(null)

    var columnPosition by mutableStateOf(ColumnPosition<K>())
    var rowPosition by mutableStateOf(RowPosition())

}

data class ColumnPosition<K>(
    var from: K? = null,
    var to: K? = null
) {
    fun canAdd() = from != to
}

data class RowPosition(
    var from: Int? = 0,
    var to: Int? = 0
) {
    fun canAdd() = from != to
}

open class ItemPosition<K>(
    var rowPosition: RowPosition,
    var columnPosition: ColumnPosition<K>
) {
    fun canAdd() = columnPosition.canAdd() //&& rowPosition.canAdd()
}


interface CustomerPerson {
    val backgroundColor: Color
}


interface CustomComposableParams<T : CustomerPerson, K> {
    val context: Context
    val screenWidth: Int?
    val screenHeight: Int?
    val elevation: Int
    val modifier: Modifier
    val idColumn: K?
    val rowList: List<T>?

    val onStart: (item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit
    val onEnd: (item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit

    fun getName(): String

    fun rowPosition(it: T): Int

    fun nameRow(it: T): String
    fun nameColumn(it: T): String

    fun getBackgroundColor(it: T): Color


    fun updateColumn(it: T, id: K?)

    fun getColumn(it: T): K



}

data class CustomComposableParamsImpl<T : CustomerPerson, K>(
    override val context: Context,
    override val screenWidth: Int? = null,
    override val screenHeight: Int? = null,
    override val elevation: Int = 0,
    override val modifier: Modifier = Modifier,
    override val idColumn: K? = null,
    override val rowList: List<T>? = null,
    override val onStart: ((item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit),
    override val onEnd: ((item: T, rowPosition: RowPosition, columnPosition: ColumnPosition<K>) -> Unit)
) : CustomComposableParams<T, K> {
    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun nameRow(it: T): String {
        TODO("Not yet implemented")
    }

    override fun nameColumn(it: T): String {
        TODO("Not yet implemented")
    }


    override fun rowPosition(it: T): Int {
        TODO("Not yet implemented")
    }

    override fun getBackgroundColor(it: T): Color {
        return it.backgroundColor
    }

    override fun updateColumn(it: T, id: K?) {
        TODO("Not yet implemented")
    }

    override fun getColumn(it: T): K {
        TODO("Not yet implemented")
    }
}