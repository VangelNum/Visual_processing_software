import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
    ) {
        App()
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
            var imageMatrix by remember { mutableStateOf<Array<IntArray>?>(null) }
            val bitShiftOptions = listOf(0, 1, 2)
            var selectedBitShift by remember { mutableStateOf(bitShiftOptions[0]) }
            var cursorX by remember { mutableStateOf(0) }
            var cursorY by remember { mutableStateOf(0) }
            var pixelBrightness by remember { mutableStateOf(0) }
            var fileName by remember { mutableStateOf<String?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(stateHorizontal)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column {
                            Text("Загрузка mbv файла:", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(9.dp))
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
                                }) {
                                Text("Загрузить", fontSize = 14.sp)
                            }
                        }
                        Column {
                            Text("Загружено изображение:")
                            Text(fileName ?: "", fontSize = 11.sp)
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false){
                                OutlinedButton(
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.White,
                                        contentColor = Color.Black
                                    ), onClick = {
                                        if (imageMatrix != null) {
                                            imageBitmap = convertMatrixToImageBitmap(imageMatrix!!, selectedBitShift)
                                        }
                                    }) {
                                    Text(
                                        "Отобразить изображение"
                                    )
                                }
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text("Сдвигать коды на:")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bitShiftOptions.forEach { shiftValue ->
                                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                        Row(
                                            modifier = Modifier.clickable { selectedBitShift = shiftValue },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedBitShift == shiftValue,
                                                onClick = { selectedBitShift = shiftValue },
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
                    if (imageBitmap != null) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        ) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = "Loaded Image",
                                modifier = Modifier
                                    .verticalScroll(stateVertical)
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            cursorX = offset.x.toInt()
                                            cursorY = offset.y.toInt()
                                            pixelBrightness =
                                                calculatePixelBrightness(
                                                    cursorX,
                                                    cursorY,
                                                    imageMatrix!!,
                                                    selectedBitShift
                                                )
                                        }
                                    }
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
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState = stateVertical),
                                style = verticalScrollbarStyle
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Координаты курсора:", style = MaterialTheme.typography.body1)
                                Text("X = $cursorX", style = MaterialTheme.typography.subtitle1)
                                Text("Y = $cursorY", style = MaterialTheme.typography.subtitle1)
                                Text("Яркость: $pixelBrightness", style = MaterialTheme.typography.subtitle1)
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


// Функция для конвертации матрицы в ImageBitmap
fun convertMatrixToImageBitmap(matrix: Array<IntArray>, bitShift: Int): ImageBitmap {
    val width = matrix[0].size
    val height = matrix.size
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixelValue = (matrix[y][x] ushr bitShift) and 0x3FF
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
        val pixelData = IntArray(width * height) { buffer.short.toInt() and 0x3FF }
        val imageMatrix = Array(height) { y ->
            IntArray(width) { x ->
                pixelData[y * width + x]
            }
        }
        val fileName = selectedFile.name
        return Pair(imageMatrix, fileName)
    }
    return Pair(null, null)
}

fun calculatePixelBrightness(x: Int, y: Int, pixelData: Array<IntArray>, bitShift: Int): Int {
    if (x >= 0 && x < pixelData[0].size && y >= 0 && y < pixelData.size) {
        return (pixelData[y][x] ushr bitShift) and 0x3FF
    }
    return 0 // Пиксель за границами изображения
}