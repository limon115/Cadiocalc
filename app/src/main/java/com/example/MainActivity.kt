package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.ComplexNumber
import com.example.logic.formatDouble
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.*

val LocalLcdTextColor = compositionLocalOf { Color.Black }

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { innerPadding ->
                    ClassWizApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Main Host container with True-Black background and 3D mock bezel calculator frame.
 */
@Composable
fun ClassWizApp(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // LCD backlight color customizer: Sleek Charcoal Dark, Solar Gray, retro Amber, high-res Backlight White
    var backlightIndex by remember { mutableStateOf(0) }
    val backlightColor = when (backlightIndex) {
        0 -> Color(0xFF1A1A1A) // Sleek Premium Charcoal Dark
        1 -> Color(0xFFC7D3C6) // Classic Solar Green-Gray
        2 -> Color(0xFFFFD899) // Warm Amber Retro Backlight
        3 -> Color(0xFFE3EDF7) // Clear White Backlight Mode
        else -> Color(0xFF1A1A1A)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp) // Limits excessive stretching on tablets
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF282C2E), Color(0xFF141617))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calculator Header Brand Print
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CASIO",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp
                )
                
                // Backlight selector button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { backlightIndex = (backlightIndex + 1) % 4 }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LIGHT",
                        color = Color.Yellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "fx-991CW",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The retro High-Res LCD Screen simulator box
            LcdScreenBox(
                uiState = uiState,
                backlightColor = backlightColor,
                onVariableSelected = { varName, value ->
                    viewModel.saveValueToVariable(varName, value)
                },
                onVariableModalDismiss = {
                    viewModel.onKeyPress("VARIABLE") // Toggles trigger
                },
                onFormatClose = {
                    viewModel.injectFormatCatalogAction("")
                },
                onFormatInject = { pattern ->
                    viewModel.injectFormatCatalogAction(pattern)
                },
                onSetTableConfig = { fx, gx, start, end, step ->
                    viewModel.updateTableConfiguration(fx, gx, start, end, step)
                },
                onDpadNavigate = { dx, dy ->
                    if (uiState.activeMode == ActiveMode.HOME) {
                        viewModel.moveHomeSelection(dx, dy)
                    }
                },
                onBatteryClick = {
                    viewModel.animateBatteryState()
                }
            )

            // Beneath the LCD Screen - CLASSWIZ chrome text logo print
            Text(
                text = "C L A S S W I Z",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // The Navigation Ring (Circular D-pad)
            NavigationRing(
                onUp = {
                    if (uiState.activeMode == ActiveMode.HOME) {
                        viewModel.moveHomeSelection(0, -1)
                    } else if (uiState.activeMode == ActiveMode.EQUATION) {
                        // Change selected cell up
                        val nextRow = if (uiState.currentSelectedCellRow > 0) uiState.currentSelectedCellRow - 1 else 2
                        viewModel.selectEquationCell(nextRow, uiState.currentSelectedCellCol)
                    }
                },
                onDown = {
                    if (uiState.activeMode == ActiveMode.HOME) {
                        viewModel.moveHomeSelection(0, 1)
                    } else if (uiState.activeMode == ActiveMode.EQUATION) {
                        // Change selected cell down
                        val nextRow = if (uiState.currentSelectedCellRow < 2) uiState.currentSelectedCellRow + 1 else 0
                        viewModel.selectEquationCell(nextRow, uiState.currentSelectedCellCol)
                    }
                },
                onLeft = {
                    if (uiState.activeMode == ActiveMode.HOME) {
                        viewModel.moveHomeSelection(-1, 0)
                    } else if (uiState.activeMode == ActiveMode.EQUATION) {
                        val nextCol = if (uiState.currentSelectedCellCol > 0) uiState.currentSelectedCellCol - 1 else 3
                        viewModel.selectEquationCell(uiState.currentSelectedCellRow, nextCol)
                    }
                },
                onRight = {
                    if (uiState.activeMode == ActiveMode.HOME) {
                        viewModel.moveHomeSelection(1, 0)
                    } else if (uiState.activeMode == ActiveMode.EQUATION) {
                        val nextCol = if (uiState.currentSelectedCellCol < 3) uiState.currentSelectedCellCol + 1 else 0
                        viewModel.selectEquationCell(uiState.currentSelectedCellRow, nextCol)
                    }
                },
                onOk = {
                    viewModel.onKeyPress("D_PAD_OK")
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // The Full glassmorphic action and numeric keyboard pad
            KeyboardPad(
                uiState = uiState,
                onKey = { k -> viewModel.onKeyPress(k) }
            )
        }
    }
}

/**
 * High-Res Liquid LCD Glass simulator viewport
 */
@Composable
fun LcdScreenBox(
    uiState: CalculatorUiState,
    backlightColor: Color,
    onVariableSelected: (String, Double) -> Unit,
    onVariableModalDismiss: () -> Unit,
    onFormatClose: () -> Unit,
    onFormatInject: (String) -> Unit,
    onSetTableConfig: (String, String, Double, Double, Double) -> Unit,
    onDpadNavigate: (Int, Int) -> Unit,
    onBatteryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backlightColor)
            .border(3.dp, Color(0xFF1B1B1B), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        val textClr = if (backlightColor == Color(0xFF1A1A1A)) Color.White else Color.Black
        CompositionLocalProvider(LocalLcdTextColor provides textClr) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Status bar row
                LcdStatusBar(uiState, onBatteryClick)

                Spacer(modifier = Modifier.height(6.dp))

                // Screen content switcher based on state
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isErrorState) {
                        LcdErrorView(uiState.errorMsg)
                    } else {
                        when (uiState.activeMode) {
                            ActiveMode.HOME -> LcdHomeMenuView(uiState, onDpadNavigate)
                            ActiveMode.CALCULATE -> LcdCalculateView(uiState)
                            ActiveMode.COMPLEX -> LcdComplexView(uiState)
                            ActiveMode.BASE_N -> LcdBaseNView(uiState)
                            ActiveMode.TABLE -> LcdTableView(uiState, onSetTableConfig)
                            ActiveMode.EQUATION -> LcdEquationView(uiState)
                            ActiveMode.MATH_BOX -> LcdMathBoxView(uiState)
                        }
                    }
                }
            }
        }

        // Overlay Variable List selection modal representing high-res dialogs
        if (uiState.showVariableModal) {
            VariableSelectDialog(
                variableMap = uiState.variableMap,
                currentValue = uiState.resultOutput.toDoubleOrNull() ?: 0.0,
                onValueSelected = onVariableSelected,
                onDismiss = onVariableModalDismiss
            )
        }

        // Overlay Format operations dialog
        if (uiState.showFormatModal) {
            FormatSelectDialog(
                currentResult = uiState.resultOutput,
                onDismiss = onFormatClose,
                onSelectPattern = onFormatInject
            )
        }

        // Action catalog injections dialog
        if (uiState.showCatalogModal) {
            CatalogInjectDialog(
                onDismiss = onFormatClose,
                onSelectPattern = onFormatInject
            )
        }
    }
}

/**
 * LCD Status Bar rendering with icons
 */
@Composable
fun LcdStatusBar(uiState: CalculatorUiState, onBatteryClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Shift active tag S
            StatusTextTag("S", uiState.shiftActive, Color(0xFFF27D26))
            // Alpha active tag A
            StatusTextTag("A", uiState.alphaActive, Color(0xFFE01E5A))
            // Math active status indicator
            StatusTextTag("Math", true, LocalLcdTextColor.current)
            // Degree or Radian indicator
            StatusTextTag(uiState.angleUnit, true, LocalLcdTextColor.current)
        }

        // App title and battery level
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = uiState.activeMode.name,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = LocalLcdTextColor.current.copy(alpha = 0.7f)
            )

            // Interactable Battery level tag representing solar indicator
            val batText = when (uiState.batteryLevel) {
                1 -> "[|  ]"
                2 -> "[|| ]"
                else -> "[|||]"
            }
            Text(
                text = batText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = if (uiState.batteryLevel == 1) Color(0xFFE01E5A) else LocalLcdTextColor.current,
                modifier = Modifier
                    .clickable { onBatteryClick() }
                    .testTag("battery_indicator")
            )
        }
    }
}

@Composable
fun StatusTextTag(text: String, active: Boolean, activeColor: Color) {
    if (active) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = activeColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/**
 * LCD Screen: Casio App Selector Grid (Home Mode)
 */
@Composable
fun LcdHomeMenuView(uiState: CalculatorUiState, onMove: (Int, Int) -> Unit) {
    val apps = listOf(
        "Calculate", "Complex", "Base-N",
        "Table", "Equation", "Math Box"
    )

    val mainColor = LocalLcdTextColor.current
    val oppositeColor = if (mainColor == Color.White) Color.Black else Color.White

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "HOME SELECT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = mainColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..2) {
                            val idx = row * 3 + col
                            val appName = apps[idx]
                            val isSelected = uiState.homeSelectedAppIndex == idx

                            val itemBg = if (isSelected) mainColor else mainColor.copy(alpha = 0.08f)
                            val itemBorder = if (isSelected) mainColor else mainColor.copy(alpha = 0.2f)
                            val itemText = if (isSelected) oppositeColor else mainColor

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(itemBg)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        itemBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = appName,
                                    color = itemText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * LCD Screen: Calculation view layout
 */
@Composable
fun LcdCalculateView(uiState: CalculatorUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Scrollable history list or active inputs
        Column(modifier = Modifier.fillMaxWidth()) {
            if (uiState.evaluationHistory.isNotEmpty()) {
                val prev = uiState.evaluationHistory.first()
                Text(
                    text = "${prev.first} = ${prev.second}",
                    fontSize = 11.sp,
                    color = LocalLcdTextColor.current.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            // Formula rendering on screen in Serif textbook display
            Text(
                text = if (uiState.formulaInput.isEmpty()) "0" else formatFormulaForScreen(uiState.formulaInput),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalLcdTextColor.current,
                fontFamily = FontFamily.Serif,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Active output results
        Text(
            text = uiState.resultOutput,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LocalLcdTextColor.current,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 6.dp)
        )
    }
}

@Composable
fun LcdComplexView(uiState: CalculatorUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "COMPLEX MODE [i]",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = LocalLcdTextColor.current.copy(alpha = 0.6f)
            )

            Text(
                text = if (uiState.formulaInput.isEmpty()) "0" else formatFormulaForScreen(uiState.formulaInput),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalLcdTextColor.current,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Evaluates dynamically
        Text(
            text = uiState.resultOutput,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LocalLcdTextColor.current,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/**
 * LCD Screen: Base-N converter
 */
@Composable
fun LcdBaseNView(uiState: CalculatorUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "BASE-N: ${uiState.baseNActiveMode.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LocalLcdTextColor.current
            )

            Text(
                text = uiState.baseNInputVal,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalLcdTextColor.current,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Hex & Bin inline displays
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val num = uiState.baseNInputVal.toLongOrNull() ?: 0L
            Text(
                text = "HEX: " + num.toString(16).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LocalLcdTextColor.current.copy(alpha = 0.6f)
            )
            Text(
                text = "BIN: " + num.toString(2).takeLast(16),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LocalLcdTextColor.current.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * LCD Screen: Function Table Maker
 */
@Composable
fun LcdTableView(
    uiState: CalculatorUiState,
    onSetConfig: (String, String, Double, Double, Double) -> Unit
) {
    if (uiState.showTableConfigScreen) {
        // Table parameters setup
        var fxInput by remember { mutableStateOf(uiState.tableFx) }
        var gxInput by remember { mutableStateOf(uiState.tableGx) }
        var startVal by remember { mutableStateOf(uiState.tableStart.toString()) }
        var endVal by remember { mutableStateOf(uiState.tableEnd.toString()) }
        var stepVal by remember { mutableStateOf(uiState.tableStep.toString()) }

        val textClr = LocalLcdTextColor.current

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "TABLE FUNCTION SET",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textClr
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("f(x) = ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                TextField(
                    value = fxInput,
                    onValueChange = { fxInput = it },
                    modifier = Modifier.weight(1f).height(24.dp).testTag("fx_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = textClr),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = textClr,
                        unfocusedIndicatorColor = textClr.copy(alpha = 0.3f)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("g(x) = ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                TextField(
                    value = gxInput,
                    onValueChange = { gxInput = it },
                    modifier = Modifier.weight(1f).height(24.dp).testTag("gx_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = textClr),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = textClr,
                        unfocusedIndicatorColor = textClr.copy(alpha = 0.3f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = startVal,
                    onValueChange = { startVal = it },
                    label = { Text("Start", fontSize = 8.sp) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textClr),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = textClr,
                        unfocusedBorderColor = textClr.copy(alpha = 0.3f),
                        focusedLabelColor = textClr,
                        unfocusedLabelColor = textClr.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = endVal,
                    onValueChange = { endVal = it },
                    label = { Text("End", fontSize = 8.sp) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textClr),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = textClr,
                        unfocusedBorderColor = textClr.copy(alpha = 0.3f),
                        focusedLabelColor = textClr,
                        unfocusedLabelColor = textClr.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = stepVal,
                    onValueChange = { stepVal = it },
                    label = { Text("Step", fontSize = 8.sp) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textClr),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = textClr,
                        unfocusedBorderColor = textClr.copy(alpha = 0.3f),
                        focusedLabelColor = textClr,
                        unfocusedLabelColor = textClr.copy(alpha = 0.5f)
                    )
                )
            }

            val okBg = if (textClr == Color.White) Color.White else Color.Black
            val okText = if (textClr == Color.White) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(okBg)
                    .clickable {
                        onSetConfig(
                            fxInput,
                            gxInput,
                            startVal.toDoubleOrNull() ?: -2.0,
                            endVal.toDoubleOrNull() ?: 2.0,
                            stepVal.toDoubleOrNull() ?: 0.5
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("[ OK & GENERATE ]", color = okText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // Output Generated rows table
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalLcdTextColor.current.copy(alpha = 0.2f))
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(1f))
                Text("x", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(2f))
                Text("f(x)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(3f))
                Text("g(x)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(3f))
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.tableGeneratedRows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, LocalLcdTextColor.current.copy(alpha = 0.1f)))
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row[0], fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(1f))
                        Text(row[1], fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(2f))
                        Text(row[2], fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                        Text(row[3], fontSize = 11.sp, color = LocalLcdTextColor.current, modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * LCD Screen: Linear & Polynomial Matrix solver View
 */
@Composable
fun LcdEquationView(uiState: CalculatorUiState) {
    val matrix = uiState.simulCoefficients
    val textClr = LocalLcdTextColor.current
    val oppositeClr = if (textClr == Color.White) Color.Black else Color.White

    when (uiState.equationSubMode) {
        EquationSubMode.CHOOSE -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("EQUATION SOLVER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                Text("- Tap key [1] for Simultaneous (2 unknowns)", fontSize = 11.sp, color = textClr)
                Text("- Tap key [2] for Simultaneous (3 unknowns)", fontSize = 11.sp, color = textClr)
                Text("- Tap key [3] for Polynomial (Degree 2)", fontSize = 11.sp, color = textClr)
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.equationSubMode.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = textClr
                    )
                }

                // Grid view of entered matrix values
                Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val numRows = if (uiState.equationSubMode == EquationSubMode.SIMUL_3) 3 else if (uiState.equationSubMode == EquationSubMode.POLY_2) 1 else 2
                        val numCols = if (uiState.equationSubMode == EquationSubMode.SIMUL_3) 4 else 3

                        for (r in 0 until numRows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0 until numCols) {
                                    val valCell = matrix[r][c]
                                    val isPicked = uiState.currentSelectedCellRow == r && uiState.currentSelectedCellCol == c

                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isPicked) textClr else textClr.copy(alpha = 0.08f))
                                            .border(1.dp, textClr.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatDouble(valCell),
                                            color = if (isPicked) oppositeClr else textClr,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Solutions block scroll
                if (uiState.equationSolutions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(textClr)
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.equationSolutions.take(3).forEach { sol ->
                            Text(sol, color = oppositeClr, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Use D-Ring to select grid cells. Tap basic keys to enter coefficients, then click EXE to solve roots.",
                        fontSize = 8.sp,
                        color = textClr.copy(alpha = 0.7f),
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * LCD Screen: Probability Math Box
 */
@Composable
fun LcdMathBoxView(uiState: CalculatorUiState) {
    val textClr = LocalLcdTextColor.current

    when (uiState.mathBoxSubMode) {
        MathBoxSubMode.CHOOSE -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("MATH BOX PROBABILITY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                Text("- Tap key [1] for Dice Roll Simulation", fontSize = 11.sp, color = textClr)
                Text("- Tap key [2] for Coin Toss Simulation", fontSize = 11.sp, color = textClr)
            }
        }
        MathBoxSubMode.DICE_CONFIG -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("DICE ROLL PARAMETERS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                Text("- Dice Count configured: ${uiState.diceCount}", fontSize = 11.sp, color = textClr)
                Text("- Attempts runs: ${uiState.attemptsCount}", fontSize = 11.sp, color = textClr)
                Text("Click EXE key to run the trial rolls!", fontWeight = FontWeight.Bold, color = Color(0xFFE01E5A), fontSize = 11.sp)
            }
        }
        MathBoxSubMode.DICE_RESULTS -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DICE RESULTS [1..${uiState.attemptsCount}]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textClr)
                    Text(
                        text = "[TOGGLE GRAPH]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF27D26),
                        modifier = Modifier.clickable { /* Toggle relative frequency graph view */ }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.diceTrialsResultList.take(20)) { trial ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trial #${trial.id}", fontSize = 10.sp, color = textClr)
                            Text("Dices: ${trial.dice.joinToString()}", fontSize = 10.sp, color = textClr)
                            Text("Sum = ${trial.sum}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textClr)
                        }
                    }
                }
            }
        }
        MathBoxSubMode.COIN_CONFIG -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("COIN TOSS PARAMETERS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textClr)
                Text("- Coin Count configured: ${uiState.coinCount}", fontSize = 11.sp, color = textClr)
                Text("- Attempts runs: ${uiState.attemptsCount}", fontSize = 11.sp, color = textClr)
                Text("Click EXE key to cast the virtual coin!", fontWeight = FontWeight.Bold, color = Color(0xFFE01E5A), fontSize = 11.sp)
            }
        }
        MathBoxSubMode.COIN_RESULTS -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("COIN TOSS: HP Heads (●) / Tails (○)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textClr)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.coinTrialsResultList.take(20)) { trial ->
                        val states = trial.tosses.map { if (it) "●" else "○" }.joinToString(" ")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Toss #${trial.id}", fontSize = 10.sp, color = textClr)
                            Text(states, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = textClr)
                            Text("Heads = ${trial.headsCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textClr)
                        }
                    }
                }
            }
        }
    }
}

/**
 * LCD Screen: Error/Warning displays
 */
@Composable
fun LcdErrorView(errorMsg: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8B0000).copy(alpha = 0.15f))
            .border(1.5.dp, Color(0xFF8B0000), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ERROR DETECTED",
            color = Color(0xFF8B0000),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = errorMsg,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "[Tap escape AC to recover]",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Format normal strings for appropriate mathematical layouts (using indices etc.)
 */
fun formatFormulaForScreen(formula: String): String {
    return formula
        .replace("*", "×")
        .replace("/", "÷")
        .replace("^2", "²")
        .replace("^", "^")
}

/**
 * 3D D-pad key controller
 */
@Composable
fun NavigationRing(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3E4346), Color(0xFF1C1E1F))
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .testTag("d_pad_container")
    ) {
        // Ok central clicker
        Box(
            modifier = Modifier
                .size(46.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color(0xFF0F1011))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .clickable { onOk() }
                .testTag("ok_button"),
            contentAlignment = Alignment.Center
        ) {
            Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // Up arrow click
        IconButton(
            onClick = onUp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(32.dp)
                .testTag("d_pad_up")
        ) {
            Text("▲", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }

        // Down arrow click
        IconButton(
            onClick = onDown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(32.dp)
                .testTag("d_pad_down")
        ) {
            Text("▼", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }

        // Left arrow click
        IconButton(
            onClick = onLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
                .testTag("d_pad_left")
        ) {
            Text("◀", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }

        // Right arrow click
        IconButton(
            onClick = onRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(32.dp)
                .testTag("d_pad_right")
        ) {
            Text("▶", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

/**
 * Floating glass scientific grid keypad layout
 */
@Composable
fun KeyboardPad(
    uiState: CalculatorUiState,
    onKey: (String) -> Unit
) {
    val rows = listOf(
        // Row 1 - Shift and major setup pads
        listOf("SHIFT", "ALPHA", "HOME", "BACK", "DEL", "AC"),
        // Row 2 - Primary system modules
        listOf("VARIABLE", "FUNCTION", "CATALOG", "FORMAT", "(", ")"),
        // Row 3 - Calculus and Trigonometrics
        listOf("sin", "cos", "tan", "ln", "log", "√"),
        // Row 4 - Powers & Exponent
        listOf("x²", "x^y", "7", "8", "9", "/"),
        // Row 5 - High volume numpad multiplication
        listOf("4", "5", "6", "*", "1", "2"),
        // Row 6 - Addition subtraction & execution trigger
        listOf("3", "0", ".", "x10^x", "EXE")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { rCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rCells.forEach { cell ->
                    val weight = when (cell) {
                        "EXE" -> 1.5f
                        else -> 1f
                    }
                    KeyWrapper(
                        cell = cell,
                        modifier = Modifier.weight(weight),
                        onClick = { onKey(cell) }
                    )
                }
            }
        }
    }
}

/**
 * Standard glassmorphic functional key container
 */
@Composable
fun KeyWrapper(
    cell: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isYellowShift = cell == "SHIFT"
    val isRedAlpha = cell == "ALPHA"
    val isExe = cell == "EXE"
    val isDelAc = cell == "DEL" || cell == "AC"
    val isAction = cell in listOf("HOME", "BACK", "VARIABLE", "FUNCTION", "CATALOG", "FORMAT")
    val isNumpad = cell in listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "x10^n", "x10^x", "Ans")
    val isOperator = cell in listOf("+", "−", "×", "÷", "*", "/")

    val btnBgColor = when {
        isDelAc -> Color(0xFF333333)
        isExe -> Color.White.copy(alpha = 0.20f)
        isYellowShift || isRedAlpha || isAction || isNumpad -> Color.White.copy(alpha = 0.10f)
        isOperator -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    val btnBorderColor = when {
        isDelAc -> Color.White.copy(alpha = 0.10f)
        isExe -> Color.White.copy(alpha = 0.40f)
        isYellowShift || isRedAlpha || isAction || isNumpad -> Color.White.copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = 0.10f)
    }

    val txtColor = when {
        isYellowShift -> Color(0xFFF27D26)
        isRedAlpha -> Color(0xFFE01E5A)
        isDelAc || isExe -> Color.White
        else -> Color.White.copy(alpha = 0.95f)
    }

    val shape = when {
        isYellowShift || isRedAlpha || isAction -> RoundedCornerShape(8.dp)
        isDelAc || isExe || isNumpad -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(6.dp)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(btnBgColor)
            .border(1.dp, btnBorderColor, shape)
            .clickable { onClick() }
            .padding(vertical = 11.dp)
            .testTag("key_${cell.lowercase().replace(" ","_")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val shiftPrintText = when (cell) {
            "sin" -> "sin⁻¹"
            "cos" -> "cos⁻¹"
            "tan" -> "tan⁻¹"
            "ln" -> "e^x"
            "log" -> "10^x"
            "Ans" -> "π"
            else -> ""
        }
        if (shiftPrintText.isNotEmpty()) {
            Text(
                text = shiftPrintText,
                color = Color(0xFFF27D26),
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }

        Text(
            text = cell,
            color = txtColor,
            fontSize = if (cell.length > 5) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * LCD Dialog Viewports: Variable map selector list
 */
@Composable
fun VariableSelectDialog(
    variableMap: Map<String, Double>,
    currentValue: Double,
    onValueSelected: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFFEFEFEF), RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORE VALUE: ${formatDouble(currentValue)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "[X]",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                variableMap.keys.chunked(3).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { varName ->
                            val currentVal = variableMap[varName] ?: 0.0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .clickable { onValueSelected(varName, currentValue) }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = varName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                    Text(text = formatDouble(currentVal), fontSize = 10.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * LCD Dialog Viewports: Output conversion Selectors
 */
@Composable
fun FormatSelectDialog(
    currentResult: String,
    onDismiss: () -> Unit,
    onSelectPattern: (String) -> Unit
) {
    val resultsValue = currentResult.toDoubleOrNull() ?: 1.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFFEFEFEF), RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FORMAT CONVERTER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                Text("[Dismiss]", color = Color.Red, fontSize = 11.sp, modifier = Modifier.clickable { onDismiss() })
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Formats list options
            val list = listOf(
                "Standard Fraction" to " / 1",
                "Decimal Value" to " * 1.0",
                "Engineering Notation (10³)" to " * 10^3",
                "Reciprocal Form (x⁻¹)" to "^-1"
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                list.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .clickable { onSelectPattern(item.second) }
                            .padding(8.dp)
                    ) {
                        Text(item.first, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * LCD Dialog Viewports: Catalog injection dropdowns
 */
@Composable
fun CatalogInjectDialog(
    onDismiss: () -> Unit,
    onSelectPattern: (String) -> Unit
) {
    val functionsCategories = listOf(
        "Absolute Value" to "Abs(",
        "Square Root" to "√(",
        "Summation (Σ)" to " + 0 * ",
        "Custom Constant (Pi)" to "π",
        "Natural Constant (e)" to "e"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFFEFEFEF), RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ADVANCED CATALOG", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                Text("[Dismiss]", color = Color.Red, fontSize = 11.sp, modifier = Modifier.clickable { onDismiss() })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                functionsCategories.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .clickable { onSelectPattern(item.second) }
                            .padding(8.dp)
                    ) {
                        Text(item.first, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
