/*
 * Copyright (c) 2023. Esto pertenece a codigo de Libreria Compose con objetivo a desarrollos de Arrastre y Soltar.
 * Los derechos de comercialización son reservados en discución con el equipo colaborador.
 * By BrunoNavarro
 */

package com.nsoft.comunityapp.draganddrop.ui

import android.content.Context
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsoft.comunityapp.draganddrop.ui.entities.COLUMN
import com.nsoft.comunityapp.draganddrop.ui.entities.DragItem
import com.nsoft.comunityapp.draganddrop.ui.library.DragTarget

@Composable
fun CustomDragCard(
    data: DragItem,
    params: Params.CustomParams
) {
    val vibrator =
        params.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    data.columnPosition.from = params.idColumn
    params.onStart?.let {
        params.onEnd?.let {
            DragTarget<DragItem, COLUMN>(
                rowIndex = data.rowPosition.from ?: 0,
                columnIndex = data.columnPosition.from as COLUMN,
                dataToDrop = data,
                vibrator = vibrator,
                onStart = params.onStart,
                onEnd = params.onEnd
            ) {
                Card(
                    backgroundColor = data.backgroundColor,
                    modifier = Modifier
                        .width(Dp((params.screenWidth ?: 0) / 2.1f))
                        .height(Dp((params.screenHeight ?: 0) / 6f))
                        .padding(8.dp)
                        .shadow(params.elevation.dp, RoundedCornerShape(15.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = data.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider()
                        Spacer(params.modifier)
                        Text(
                            text = data.column.name,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}