import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.lightColors
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 800.dp)
    Window(
        title = "Программное обеспечение обработки визуальных данных лабораторная работа 3",
        state = windowState,
        onCloseRequest = ::exitApplication,
    ) {
        App()
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme(colors = lightColors()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
        ) {
            val stateVertical = rememberScrollState(0)
            val stateHorizontal = rememberScrollState(0)
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            var imageBitmapForHistogram by remember { mutableStateOf<ImageBitmap?>(null) }
            var imageMatrix by remember { mutableStateOf<Array<IntArray>?>(null) }
            val bitShiftOptions = listOf(0, 1, 2)
            var selectedBitShift by remember { mutableStateOf(bitShiftOptions[0]) }
            var cursorX by remember { mutableStateOf(0) }
            var cursorY by remember { mutableStateOf(0) }
            var pixelBrightness by remember { mutableStateOf(0) }
            var fileName by remember { mutableStateOf<String?>(null) }
            var isPreviewImageVisible by remember { mutableStateOf(false) }
            var isInterpolationEnabled by remember { mutableStateOf(false) }
            var isNormalizationEnabled by remember { mutableStateOf(false) }
            var zoomPosition by remember { mutableStateOf(1f) }
            var verticalScrollbarHeight by remember {
                mutableStateOf(0)
            }
            var blackPoint by remember { mutableStateOf(0) }
            var whitePoint by remember { mutableStateOf(255) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(stateHorizontal)
            ) {
                Column() {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column {
                            Text("Загрузка mbv файла:")
                            OutlinedButton(
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.White,
                                    contentColor = Color.Black
                                ), onClick = {
                                    val (loadedImageMatrix, loadedFileName) = loadFile()
                                    imageMatrix = loadedImageMatrix
                                    loadedFileName?.let {
                                        fileName =
                                            "$it [${imageMatrix?.getOrNull(0)?.size ?: 0} x ${imageMatrix?.size ?: 0}]"
                                    }
                                    if (imageMatrix != null) {
                                        imageBitmap = convertMatrixToImageBitmap(
                                            imageMatrix!!,
                                            selectedBitShift
                                        )
                                        imageBitmapForHistogram =
                                            convertExampleForMainMatrixToImageBitmap(
                                                imageMatrix!!,
                                                selectedBitShift,
                                                blackPoint,
                                                whitePoint
                                            )
                                    }
                                }) {
                                Text("Загрузить")
                            }
                        }
                        Column {
                            Text("Загружено изображение:")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(fileName ?: "")
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Сдвигать коды на:")
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bitShiftOptions.forEach { shiftValue ->
                                        Row(
                                            modifier = Modifier.clickable {
                                                selectedBitShift = shiftValue
                                                if (imageMatrix != null) {
                                                    imageBitmap = convertMatrixToImageBitmap(
                                                        imageMatrix!!,
                                                        selectedBitShift
                                                    )
                                                }
                                            },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedBitShift == shiftValue,
                                                onClick = {
                                                    selectedBitShift = shiftValue
                                                    if (imageMatrix != null && imageBitmapForHistogram != null) {
                                                        imageBitmap = convertMatrixToImageBitmap(
                                                            imageMatrix!!,
                                                            selectedBitShift
                                                        )
                                                        imageBitmapForHistogram =
                                                            convertExampleForMainMatrixToImageBitmap(
                                                                imageMatrix!!,
                                                                selectedBitShift,
                                                                blackPoint,
                                                                whitePoint
                                                            )
                                                    }
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color.Black,
                                                    unselectedColor = Color.Black,
                                                    disabledColor = Color.Black
                                                ),
                                            )
                                            Text(
                                                text = shiftValue.toString(),
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                    if (imageBitmap != null && imageBitmapForHistogram != null) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        ) {
                            if (isPreviewImageVisible) {
                                OverviewWindow(
                                    imageBitmap!!,
                                    stateVertical,
                                    verticalScrollbarHeight,
                                )
                            }

                            Image(
                                bitmap = imageBitmapForHistogram!!,
                                contentDescription = "Loaded Image",
                                modifier = Modifier
                                    .verticalScroll(stateVertical)
                                    .pointerMoveFilter(
                                        onMove = { offset ->
                                            cursorX = offset.x.toInt()
                                            cursorY = offset.y.toInt()
                                            pixelBrightness =
                                                calculatePixelBrightness(
                                                    cursorX,
                                                    cursorY,
                                                    imageMatrix!!,
                                                    selectedBitShift
                                                )
                                            true
                                        }
                                    )
                            )
                            val verticalScrollbarStyle = ScrollbarStyle(
                                minimalHeight = 0.dp,
                                thickness = 12.dp,
                                shape = RectangleShape,
                                hoverDurationMillis = 100,
                                unhoverColor = Color.Gray,
                                hoverColor = Color.DarkGray
                            )
                            VerticalScrollbar(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .onGloballyPositioned { layoutCoordinates ->
                                        verticalScrollbarHeight = layoutCoordinates.size.height
                                    },
                                adapter = rememberScrollbarAdapter(scrollState = stateVertical),
                                style = verticalScrollbarStyle
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                    renderMagnifier(
                                        imageMatrix = imageMatrix!!,
                                        cursorX = cursorX,
                                        cursorY = cursorY,
                                        bitShift = selectedBitShift,
                                        isInterpolationEnabled = isInterpolationEnabled,
                                        isNormalizationEnabled = isNormalizationEnabled,
                                        zoomPosition = zoomPosition
                                    )
                                    Slider(
                                        modifier = Modifier.width(300.dp),
                                        value = zoomPosition,
                                        onValueChange = {
                                            zoomPosition = it
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.secondary,
                                            activeTrackColor = MaterialTheme.colors.onBackground,
                                            inactiveTrackColor = MaterialTheme.colors.primary,
                                        ),

                                        steps = 3,
                                        valueRange = 1f..5f
                                    )

                                    Row(
                                        modifier = Modifier.width(300.dp)
                                            .padding(start = 5.dp, end = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("1")
                                        Text("2")
                                        Text("3")
                                        Text("4")
                                        Text("5")
                                    }
                                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {

                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isInterpolationEnabled,
                                                onCheckedChange = { isInterpolationEnabled = it }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Интерполяция")
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isNormalizationEnabled,
                                                onCheckedChange = { isNormalizationEnabled = it }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Нормализация")
                                        }
                                    }
                                }
                                Text("Координаты курсора:", style = MaterialTheme.typography.body1)
                                Text("X = $cursorX", style = MaterialTheme.typography.subtitle1)
                                Text("Y = $cursorY", style = MaterialTheme.typography.subtitle1)
                                Text(
                                    "Яркость: $pixelBrightness",
                                    style = MaterialTheme.typography.subtitle1
                                )

                                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isPreviewImageVisible,
                                            onCheckedChange = {
                                                isPreviewImageVisible = it
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Обзорное изображение")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                DrawHistogram(
                                    imageMatrix = imageMatrix!!,
                                    bitShift = selectedBitShift,
                                    imageBitmap!!,
                                    stateVertical,
                                    verticalScrollbarHeight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .height(40.dp)
                                        .border(1.dp, Color.Black)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Black, Color.White),
                                                startX = 0f,
                                                endX = 300f
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DrawHistogramSliders(
                                    blackPoint,
                                    whitePoint,
                                    onBlackPointChange = {
                                        blackPoint = it.toInt()
                                        imageBitmapForHistogram =
                                            convertExampleForMainMatrixToImageBitmap(
                                                imageMatrix!!,
                                                selectedBitShift,
                                                blackPoint,
                                                whitePoint
                                            )
                                    },
                                    onWhitePointChange = {
                                        whitePoint = it.toInt()
                                        imageBitmapForHistogram =
                                            convertExampleForMainMatrixToImageBitmap(
                                                imageMatrix!!,
                                                selectedBitShift,
                                                blackPoint,
                                                whitePoint
                                            )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            val horizontalScrollbarStyle = ScrollbarStyle(
                minimalHeight = 0.dp,
                thickness = 12.dp,
                shape = RectangleShape,
                hoverDurationMillis = 100,
                unhoverColor = Color.Gray,
                hoverColor = Color.DarkGray
            )
            HorizontalScrollbar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                adapter = rememberScrollbarAdapter(stateHorizontal),
                style = horizontalScrollbarStyle
            )
        }
    }
}

// Функция для выполнения метода ближайшего соседа при увеличении
fun nearestNeighborZoom(imageMatrix: Array<IntArray>, x: Float, y: Float): Int {
    val width = imageMatrix[0].size
    val height = imageMatrix.size

    // Определение ближайших координат
    val xNearest = x.roundToInt().coerceIn(0, width - 1)
    val yNearest = y.roundToInt().coerceIn(0, height - 1)

    // Возврат значения яркости из ближайшего пикселя
    return imageMatrix[yNearest][xNearest]
}

fun nearestNeighborZoomNew(
    imageMatrix: Array<IntArray>,
    scaleSize: Float,
    cursorX: Float,
    cursorY: Float
): Array<IntArray> {
    val scaleFactor = scaleSize
    val copyImageMatrix = imageMatrix.map { it.clone() }.toTypedArray()
    val height = copyImageMatrix.size
    val width = copyImageMatrix[0].size
    val newHeight = (height / scaleFactor).toInt()
    val newWidth = (width / scaleFactor).toInt()

    val xL = (0 until newWidth).map { cursorX + (it / scaleFactor) }
    val yL = (0 until newHeight).map { cursorY + (it / scaleFactor) }

    val x = Array(newHeight) { FloatArray(newWidth) }
    val y = Array(newHeight) { FloatArray(newWidth) }

    for (i in 0 until newHeight) {
        for (j in 0 until newWidth) {
            x[i][j] = xL[j]
            y[i][j] = yL[i]
        }
    }

    val xNearest =
        x.map { it.map { it.roundToInt().coerceIn(0, width - 1) }.toIntArray() }.toTypedArray()
    val yNearest =
        y.map { it.map { it.roundToInt().coerceIn(0, height - 1) }.toIntArray() }.toTypedArray()

    val scaledImage = Array(newHeight) { IntArray(newWidth) }

    for (i in 0 until newHeight) {
        for (j in 0 until newWidth) {
            scaledImage[i][j] = copyImageMatrix[yNearest[i][j]][xNearest[i][j]]
        }
    }

    return scaledImage
}


fun bilinearInterpolationZoom(
    imageMatrix: Array<IntArray>,
    x: Float,
    y: Float,
): Int {
    val x0 = x.toInt()
    val y0 = y.toInt()
    val x1 = (x0 + 1)
    val y1 = (y0 + 1)

    val dx = x - x0
    val dy = y - y0

    val interpolatedValue = (1 - dx) * (1 - dy) * imageMatrix[y0][x0] +
            dx * (1 - dy) * imageMatrix[y0][x1] +
            (1 - dx) * dy * imageMatrix[y1][x0] +
            dx * dy * imageMatrix[y1][x1]

    return interpolatedValue.toInt().coerceIn(0, 1023)
}

fun  normalizeBrightness(value: Int, minValue: Int, maxValue: Int): Int {
    val normalizedValue = ((value - minValue) * (255.0 / (maxValue - minValue))).toInt()
    return normalizedValue.coerceIn(0, 255)
}

@Composable
fun renderMagnifier(
    isInterpolationEnabled: Boolean,
    isNormalizationEnabled: Boolean,
    imageMatrix: Array<IntArray>,
    zoomPosition: Float,
    cursorX: Int,
    cursorY: Int,
    bitShift: Int
) {
    val imageSizeDp = 300.dp
    val imageSizePx = with(LocalDensity.current) { imageSizeDp.toPx() }

    val previewSize = imageSizePx.toInt()

    // Вычисление начальных координат предварительного просмотра
    val startX = (cursorX - previewSize / (zoomPosition)).coerceIn(
        0F,
        (imageMatrix[0].size - previewSize / zoomPosition)
    )
    val startY = (cursorY - previewSize / (2 * zoomPosition)).coerceIn(
        0F,
        (imageMatrix.size - previewSize / zoomPosition)
    )

    // Создание матрицы для предварительного просмотра
    var previewImageMatrix = Array(previewSize) { y ->
        IntArray(previewSize) { x ->
            // Вычисление оригинальных координат с учетом увеличения
            val originalX = (startX + x / zoomPosition).coerceIn(
                0F,
                (imageMatrix[0].size - 1).toFloat()
            )
            val originalY = (startY + y / zoomPosition).coerceIn(
                0F,
                (imageMatrix.size - 1).toFloat()
            )

            // Выбор метода интерполяции в зависимости от флага
            if (isInterpolationEnabled) {
                bilinearInterpolationZoom(imageMatrix, originalX, originalY)
            } else {
                nearestNeighborZoom(imageMatrix, originalX, originalY)
            }
        }
    }

    val minValue = previewImageMatrix.flatMap { it.asIterable() }.minOrNull() ?: 0
    val maxValue = previewImageMatrix.flatMap { it.asIterable() }.maxOrNull() ?: 255

    // Нормализация яркости, если флаг нормализации включен
    val normalizedPreviewImageMatrix = if (isNormalizationEnabled) {
        Array(previewSize) { y ->
            IntArray(previewSize) { x ->
                normalizeBrightness(previewImageMatrix[y][x], minValue, maxValue)
            }
        }
    } else {
        previewImageMatrix
    }

    val previewImageBitmap = convertMatrixToImageBitmap(
        normalizedPreviewImageMatrix,
        bitShift
    )

    Image(
        bitmap = previewImageBitmap,
        contentDescription = "Zoom Image",
        modifier = Modifier.size(imageSizeDp)
    )
}


@Composable
fun OverviewWindow(
    overviewImageBitmap: ImageBitmap,
    stateVertical: ScrollState,
    verticalScrollbarHeight: Int
) {
    val visibleStartX = 0
    val visibleStartY = stateVertical.value
    val visibleWidth = overviewImageBitmap.width
    var visibleHeight by remember { mutableStateOf(0) }
    visibleHeight = verticalScrollbarHeight
    val scope = rememberCoroutineScope()
    Box() {
        Image(
            bitmap = overviewImageBitmap,
            contentDescription = "Overview Image",
            modifier = Modifier
                .border(1.dp, Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        scope.launch {
                            val clickedPercentage = offset.y / size.height
                            val totalImageHeight = overviewImageBitmap.height
                            val scrollToY =
                                (totalImageHeight * clickedPercentage - size.height / 2).toInt()
                            stateVertical.animateScrollTo(
                                scrollToY.coerceIn(
                                    0,
                                    (totalImageHeight - size.height)
                                )
                            )
                        }
                    }
                }
        )
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            // Draw a rectangle for the visible region of the histogram
            val rectLeft = visibleStartX * size.width / overviewImageBitmap.width
            val rectTop = visibleStartY * size.height / overviewImageBitmap.height
            val rectRight = (visibleStartX + visibleWidth) * size.width / overviewImageBitmap.width
            val rectBottom =
                (visibleStartY + visibleHeight) * size.height / overviewImageBitmap.height

            drawRect(
                color = Color.Red,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectRight - rectLeft, rectBottom - rectTop),
                style = Stroke(width = 2.dp.toPx())
            )

        }
    }
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
fun DrawHistogramSliders(
    blackPoint: Int,
    whitePoint: Int,
    onBlackPointChange: (Float) -> Unit,
    onWhitePointChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.width(300.dp),
            value = blackPoint.toFloat(),
            onValueChange = {
                if (it <= whitePoint.toFloat()) {
                    onBlackPointChange(it)
                }
            },
            valueRange = 0f..255f,
            steps = 256
        )
        Text("Ч")
        Spacer(modifier = Modifier.width(4.dp))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.width(300.dp),
            value = whitePoint.toFloat(),
            onValueChange = {
                if (it >= blackPoint.toFloat()) {
                    onWhitePointChange(it)
                }
            },
            valueRange = 0f..255f,
            steps = 256
        )
        Text("Б")
        Spacer(modifier = Modifier.width(4.dp))
    }
}


@Composable
fun DrawHistogram(
    imageMatrix: Array<IntArray>,
    bitShift: Int,
    imageBitmap: ImageBitmap,
    stateVertical: ScrollState,
    verticalScrollbarHeight: Int
) {
    Spacer(modifier = Modifier.width(8.dp))
    val visibleStartX = 0 // Starting X coordinate is always 0
    val visibleStartY = stateVertical.value // Use the current vertical scroll position
    val visibleWidth = imageBitmap.width
    val visibleHeight = visibleStartY + verticalScrollbarHeight

    val brightnessValues = IntArray(256) { 0 }

    // Calculate brightness values for the visible region
    for (y in visibleStartY until (visibleHeight)) {
        for (x in visibleStartX until (visibleStartX + visibleWidth)) {
            val brightness = calculatePixelBrightness(x, y, imageMatrix, bitShift)
            brightnessValues[brightness]++
        }
    }

    Canvas(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
            .border(1.dp, Color.Black)
    ) {
        val maxValue = brightnessValues.maxOrNull() ?: 1
        val barWidth = size.width / brightnessValues.size

        brightnessValues.forEachIndexed { index, value ->
            val barHeight = if (maxValue > 0) {
                (value.toFloat() / maxValue) * size.height
            } else {
                0f
            }

            drawRect(
                color = Color.Black,
                topLeft = Offset(index * barWidth, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}


fun convertMatrixToImageBitmap(
    matrix: Array<IntArray>,
    bitShift: Int
): ImageBitmap {
    val width = matrix[0].size
    val height = matrix.size
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            // Получаем значение пикселя сдвигая его на указанное количество бит и маскируем только 10 младших бит
            val originalPixelValue = (matrix[y][x] ushr bitShift)

            // Формируем ARGB значение пикселя и устанавливаем его в BufferedImage
            val argb =
                0xFF shl 24 or (originalPixelValue shl 16) or (originalPixelValue shl 8) or originalPixelValue
            bufferedImage.setRGB(x, y, argb)
        }
    }
    return bufferedImage.toComposeImageBitmap()
}


fun convertExampleForMainMatrixToImageBitmap(
    matrix: Array<IntArray>,
    bitShift: Int,
    blackThreshold: Int,
    whiteThreshold: Int
): ImageBitmap {
    val width = matrix[0].size
    val height = matrix.size
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)


    for (y in 0 until height) {
        for (x in 0 until width) {
            // Получаем значение пикселя сдвигая его на указанное количество бит и маскируем только 10 младших бит
            val originalPixelValue = (matrix[y][x] ushr bitShift)
            // Применяем уровни черного и белого
            // Apply black and white adjustments
//            val adjustedPixelValue = when {
//                originalPixelValue < blackThreshold -> 0
//                originalPixelValue > whiteThreshold-> 256
//                else -> ((originalPixelValue - blackThreshold) * 256) / (whiteThreshold - blackThreshold)
//            }
//            // Формируем ARGB значение пикселя и устанавливаем его в BufferedImage
//            val argb = if (originalPixelValue < blackThreshold || originalPixelValue > whiteThreshold) {
//                // Colorize pixels outside the specified range (e.g., red)
//                0xFF shl 24 or (255 shl 16) or ( 255 shl 8) or 255
//            } else {
//                // Apply the adjusted grayscale value to pixels within the specified range
//                0xFF shl 24 or (adjustedPixelValue shl 16) or (adjustedPixelValue shl 8) or adjustedPixelValue
//            }
            var pixelValue = (matrix[y][x] ushr bitShift) and 0xFF
            pixelValue = (pixelValue - blackThreshold).coerceIn(0, 255)
            pixelValue = (pixelValue * (255f / (whiteThreshold - blackThreshold))).toInt().coerceIn(0, 255)

            val argb = 0xFF shl 24 or (pixelValue shl 16) or (pixelValue shl 8) or pixelValue
            bufferedImage.setRGB(x, y, argb)
        }
    }
    return bufferedImage.toComposeImageBitmap()
}


fun loadFile(): Pair<Array<IntArray>?, String?> {
    val fileChooser = JFileChooser()
    fileChooser.fileFilter = FileNameExtensionFilter("MBV files", "mbv")
    val result = fileChooser.showOpenDialog(null)

    if (result == JFileChooser.APPROVE_OPTION) {
        val selectedFile = fileChooser.selectedFile
        val fileBytes = selectedFile.readBytes()
        val buffer = ByteBuffer.wrap(fileBytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val width = buffer.short.toInt()
        val height = buffer.short.toInt()
        val imageMatrix = Array(height) { y ->
            IntArray(width) { x ->
                buffer.short.toInt() and 0x3FF
            }
        }
        val fileName = selectedFile.name
        return Pair(imageMatrix, fileName)
    }
    return Pair(null, null)
}

fun calculatePixelBrightness(x: Int, y: Int, pixelData: Array<IntArray>, bitShift: Int): Int {
    // Проверяем, находятся ли указанные координаты в пределах изображения
    if (x >= 0 && x < pixelData[0].size && y >= 0 && y < pixelData.size) {
        // Получаем значение пикселя сдвигая и маскируем только 10 младших бит
        return (pixelData[y][x] ushr bitShift) and 0xFF
    }
    // Если координаты находятся за пределами изображения, возвращаем 0
    return 0 // Пиксель за границами изображения
}


@Preview
@Composable
fun mainPreview() {
    App()
}