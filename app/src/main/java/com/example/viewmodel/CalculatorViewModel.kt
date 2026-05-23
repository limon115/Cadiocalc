package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.db.CalculatorDatabase
import com.example.db.HistoryEntity
import com.example.db.VariableEntity
import com.example.logic.ComplexNumber
import com.example.logic.MathEngine
import com.example.logic.formatDouble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Screen modes for the ClassWiz Simulator.
 */
enum class ActiveMode {
    HOME,          // Main menu selection
    CALCULATE,     // Scientific Calculation
    COMPLEX,       // Complex math calculations
    BASE_N,        // Decimal, Hex, Binary, Octal converter
    TABLE,         // Function f(x)/g(x) table creation
    EQUATION,      // Solver (Simultaneous or Polynomial)
    MATH_BOX       // Dice rolls and Coin tosses
}

/**
 * Base-N active displays.
 */
enum class BaseNMode {
    DEC, HEX, BIN, OCT
}

/**
 * Equation Sub-modes.
 */
enum class EquationSubMode {
    CHOOSE,
    SIMUL_2,
    SIMUL_3,
    POLY_2,
    POLY_3
}

/**
 * Math Box Sub-modes.
 */
enum class MathBoxSubMode {
    CHOOSE,
    DICE_CONFIG,
    DICE_RESULTS,
    COIN_CONFIG,
    COIN_RESULTS
}

/**
 * Calculator UI State Data class.
 */
data class CalculatorUiState(
    val activeMode: ActiveMode = ActiveMode.CALCULATE,
    val homeSelectedAppIndex: Int = 0, // 0 to 5 indexes in HOME grid
    val shiftActive: Boolean = false,
    val alphaActive: Boolean = false,
    val angleUnit: String = "D", // "D" (Degree) or "R" (Radian)
    val batteryLevel: Int = 3, // 1=Low, 2=Medium, 3=Full
    
    // Calculate Mode States
    val formulaInput: String = "",
    val cursorIndex: Int = 0,
    val resultOutput: String = "0",
    val isErrorState: Boolean = false,
    val errorMsg: String = "",
    val evaluationHistory: List<Pair<String, String>> = emptyList(),

    // Shared Variables A-F, X, Y, Z
    val variableMap: Map<String, Double> = mapOf(
        "A" to 0.0, "B" to 0.0, "C" to 0.0, "D" to 0.0, "E" to 0.0, "F" to 0.0,
        "X" to 0.0, "Y" to 0.0, "Z" to 0.0
    ),
    val showVariableModal: Boolean = false,

    // Catalog functions dropdown triggers
    val showCatalogModal: Boolean = false,
    val showFormatModal: Boolean = false,

    // Base-N Applet States
    val baseNActiveMode: BaseNMode = BaseNMode.DEC,
    val baseNInputVal: String = "0",

    // Table Mode States
    val tableFx: String = "X^2 + 0.5",
    val tableGx: String = "X^2 - 0.5",
    val isEditingTableFx: Boolean = true, // true=editing f(x), false=g(x)
    val tableStart: Double = -2.0,
    val tableEnd: Double = 2.0,
    val tableStep: Double = 0.5,
    val showTableConfigScreen: Boolean = true,
    val tableGeneratedRows: List<List<String>> = emptyList(), // list of [index, x, f(x), g(x)]

    // Equation Mode States
    val equationSubMode: EquationSubMode = EquationSubMode.CHOOSE,
    // Store 3x4 matrix coefficients: array of row cells
    // Simul-2 needs [a1, b1, c1] and [a2, b2, c2]
    // Polynomial-2 needs a,b,c
    val simulCoefficients: List<DoubleArray> = listOf(
        doubleArrayOf(0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    ),
    val currentSelectedCellRow: Int = 0,
    val currentSelectedCellCol: Int = 0,
    val equationSolutions: List<String> = emptyList(),

    // Math Box Mode States
    val mathBoxSubMode: MathBoxSubMode = MathBoxSubMode.CHOOSE,
    val diceCount: Int = 2,
    val coinCount: Int = 3,
    val attemptsCount: Int = 100,
    val diceTrialsResultList: List<MathEngine.DiceTrial> = emptyList(),
    val coinTrialsResultList: List<MathEngine.CoinTrial> = emptyList(),
    val showRelativeFrequencyGraph: Boolean = true
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        CalculatorDatabase::class.java,
        "classwiz_calc_db"
    ).build()

    private val mathEngine = MathEngine()

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState

    init {
        // Load initial persistent variables and history log
        viewModelScope.launch {
            try {
                val persistentVars = db.dao().getAllVariables()
                if (persistentVars.isNotEmpty()) {
                    val updatedMap = _uiState.value.variableMap.toMutableMap()
                    persistentVars.forEach {
                        updatedMap[it.name] = it.value
                    }
                    _uiState.update { it.copy(variableMap = updatedMap) }
                }

                val dbHistory = db.dao().getHistory()
                if (dbHistory.isNotEmpty()) {
                    val list = dbHistory.map { Pair(it.input, it.output) }
                    _uiState.update { it.copy(evaluationHistory = list) }
                }
            } catch (e: Exception) {
                // Safe default fallback on SQLite load issue
            }
        }
    }

    /**
     * Home grid item selection directional offset update
     */
    fun moveHomeSelection(dx: Int, dy: Int) {
        val currentIndex = _uiState.value.homeSelectedAppIndex
        // Grid is 3 wide:
        // Index mapping:
        // [0: Calculate] [1: Complex] [2: Base-N]
        // [3: Table]     [4: Equation] [5: Math Box]
        val col = currentIndex % 3
        val row = currentIndex / 3

        val nextCol = (col + dx + 3) % 3
        val nextRow = (row + dy + 2) % 2
        val nextIndex = nextRow * 3 + nextCol

        _uiState.update { it.copy(homeSelectedAppIndex = nextIndex) }
    }

    /**
     * Select outstanding App inside HOME
     */
    fun selectHomeApp() {
        val selectedAppIndex = _uiState.value.homeSelectedAppIndex
        val chosenMode = when (selectedAppIndex) {
            0 -> ActiveMode.CALCULATE
            1 -> ActiveMode.COMPLEX
            2 -> ActiveMode.BASE_N
            3 -> ActiveMode.TABLE
            4 -> ActiveMode.EQUATION
            5 -> ActiveMode.MATH_BOX
            else -> ActiveMode.CALCULATE
        }
        _uiState.update { it.copy(
            activeMode = chosenMode,
            formulaInput = "",
            isErrorState = false,
            resultOutput = "0"
        ) }
    }

    fun setAppMode(mode: ActiveMode) {
        _uiState.update { it.copy(activeMode = mode, isErrorState = false) }
    }

    /**
     * Numpad and operators input processing.
     */
    fun onKeyPress(key: String) {
        val state = _uiState.value
        val shiftActive = state.shiftActive
        val alphaActive = state.alphaActive

        // Reset toggles after key consumption (except helper checks)
        _uiState.update { it.copy(shiftActive = false, alphaActive = false) }

        when (key) {
            "HOME" -> {
                _uiState.update { it.copy(activeMode = ActiveMode.HOME) }
            }
            "BACK" -> {
                if (state.activeMode == ActiveMode.HOME) {
                    _uiState.update { it.copy(activeMode = ActiveMode.CALCULATE) }
                } else if (state.activeMode == ActiveMode.TABLE && !state.showTableConfigScreen) {
                    _uiState.update { it.copy(showTableConfigScreen = true) }
                } else if (state.activeMode == ActiveMode.EQUATION && state.equationSubMode != EquationSubMode.CHOOSE) {
                    _uiState.update { it.copy(equationSubMode = EquationSubMode.CHOOSE) }
                } else if (state.activeMode == ActiveMode.MATH_BOX && state.mathBoxSubMode != MathBoxSubMode.CHOOSE) {
                    _uiState.update { it.copy(mathBoxSubMode = MathBoxSubMode.CHOOSE) }
                } else {
                    _uiState.update { it.copy(activeMode = ActiveMode.HOME) }
                }
            }
            "SHIFT" -> {
                _uiState.update { it.copy(shiftActive = !shiftActive, alphaActive = false) }
            }
            "ALPHA" -> {
                _uiState.update { it.copy(alphaActive = !alphaActive, shiftActive = false) }
            }
            "VARIABLE" -> {
                _uiState.update { it.copy(showVariableModal = !state.showVariableModal) }
            }
            "FUNCTION" -> {
                if (state.activeMode == ActiveMode.TABLE) {
                    _uiState.update { it.copy(isEditingTableFx = !state.isEditingTableFx) }
                } else {
                    // Quick inject x
                    appendInput("X")
                }
            }
            "CATALOG" -> {
                _uiState.update { it.copy(showCatalogModal = true) }
            }
            "TOOLS" -> {
                // Initialize default reset
                resetAllSettings()
            }
            "DEL" -> {
                deleteLastChar()
            }
            "AC" -> {
                clearScreen()
            }
            "FORMAT" -> {
                if (state.activeMode == ActiveMode.BASE_N) {
                    // Cycles through Dec -> Hex -> Bin -> Oct
                    toggleBaseNFormat()
                } else {
                    _uiState.update { it.copy(showFormatModal = true) }
                }
            }
            "EXE" -> {
                evaluateCurrentExpression()
            }
            "D_PAD_OK" -> {
                if (state.activeMode == ActiveMode.HOME) {
                    selectHomeApp()
                } else {
                    evaluateCurrentExpression()
                }
            }
            else -> {
                // Normal text inputs
                handleNormalTextInput(key, shiftActive, alphaActive)
            }
        }
    }

    private fun handleNormalTextInput(key: String, shiftActive: Boolean, alphaActive: Boolean) {
        val state = _uiState.value

        // If inside Base-N Applet
        if (state.activeMode == ActiveMode.BASE_N) {
            handleBaseNInput(key)
            return
        }

        // Variable Recall / Store checking
        if (alphaActive) {
            val validVars = listOf("A", "B", "C", "D", "E", "F", "X", "Y", "Z")
            val targetVar = key.uppercase()
            if (validVars.contains(targetVar)) {
                // Injects the variable
                appendInput(targetVar)
                return
            }
        }

        // Shift modifier actions
        if (shiftActive) {
            val shiftAction = when (key) {
                "sin" -> "sin⁻¹("
                "cos" -> "cos⁻¹("
                "tan" -> "tan⁻¹("
                "ln" -> "e^("
                "log" -> "10^("
                "Ans" -> "π"
                "x" -> "X"
                "AC" -> {
                    // Turn "OFF" the LCD display, trigger error state simulator
                    _uiState.update { it.copy(isErrorState = true, errorMsg = "SYSTEM SHUTDOWN - TAPPING ESC/AC POWER ON") }
                    return
                }
                else -> ""
            }
            if (shiftAction.isNotEmpty()) {
                appendInput(shiftAction)
                return
            }
        }

        // Process standard keys
        val input = when (key) {
            "sin" -> "sin("
            "cos" -> "cos("
            "tan" -> "tan("
            "ln" -> "ln("
            "log" -> "log("
            "√" -> "√("
            "x²" -> "^2"
            "x^y" -> "^"
            "*" -> " * "
            "/" -> " / "
            "+" -> " + "
            "-" -> " - "
            "Ans" -> "Ans"
            "x10^x" -> " * 10^"
            else -> key
        }
        appendInput(input)
    }

    private fun handleBaseNInput(key: String) {
        val state = _uiState.value
        val allowedDigits = when (state.baseNActiveMode) {
            BaseNMode.DEC -> "0123456789+-"
            BaseNMode.HEX -> "0123456789ABCDEFabcdef+-"
            BaseNMode.BIN -> "01+-"
            BaseNMode.OCT -> "01234567+- "
        }

        // Check clear
        if (key == "AC") {
            _uiState.update { it.copy(baseNInputVal = "0") }
            return
        }

        if (key == "." || key == "sin" || key == "cos" || key == "tan" || key == "√") {
            // Not allowed in integer Base-N
            return
        }

        var incoming = key
        // Map letters to Hex inputs if Alpha triggers are clicked
        if (incoming == "7" && _uiState.value.alphaActive) incoming = "A"
        if (incoming == "8" && _uiState.value.alphaActive) incoming = "B"
        if (incoming == "9" && _uiState.value.alphaActive) incoming = "C"

        if (allowedDigits.contains(incoming)) {
            val current = if (state.baseNInputVal == "0") "" else state.baseNInputVal
            _uiState.update { it.copy(baseNInputVal = current + incoming) }
        }
    }

    private fun toggleBaseNFormat() {
        val state = _uiState.value
        val currentBase = state.baseNActiveMode
        val nextBase = when (currentBase) {
            BaseNMode.DEC -> BaseNMode.HEX
            BaseNMode.HEX -> BaseNMode.BIN
            BaseNMode.BIN -> BaseNMode.OCT
            BaseNMode.OCT -> BaseNMode.DEC
        }
        _uiState.update { it.copy(baseNActiveMode = nextBase) }
    }

    fun injectFormatCatalogAction(pattern: String) {
        _uiState.update { it.copy(showCatalogModal = false, showFormatModal = false) }
        appendInput(pattern)
    }

    private fun appendInput(str: String) {
        _uiState.update {
            val updatedInput = it.formulaInput + str
            it.copy(
                formulaInput = updatedInput,
                isErrorState = false
            )
        }
    }

    private fun deleteLastChar() {
        _uiState.update {
            if (it.activeMode == ActiveMode.BASE_N) {
                val input = it.baseNInputVal
                val next = if (input.length <= 1) "0" else input.dropLast(1)
                it.copy(baseNInputVal = next)
            } else {
                val input = it.formulaInput
                it.copy(formulaInput = if (input.isNotEmpty()) input.dropLast(1) else "")
            }
        }
    }

    private fun clearScreen() {
        _uiState.update {
            if (it.activeMode == ActiveMode.BASE_N) {
                it.copy(baseNInputVal = "0")
            } else {
                it.copy(
                    formulaInput = "",
                    resultOutput = "0",
                    isErrorState = false,
                    errorMsg = ""
                )
            }
        }
    }

    /**
     * Variable store utility. Call on UI to assign variables. Is persistent.
     */
    fun saveValueToVariable(varName: String, value: Double) {
        val updatedMap = _uiState.value.variableMap.toMutableMap()
        updatedMap[varName] = value
        _uiState.update { it.copy(variableMap = updatedMap, showVariableModal = false) }

        viewModelScope.launch {
            try {
                db.dao().saveVariable(VariableEntity(varName, value))
            } catch (e: Exception) {
                // Safeguard against DB write error
            }
        }
    }

    /**
     * Formula Evaluator engine processor
     */
    private fun evaluateCurrentExpression() {
        val state = _uiState.value

        // Direct Solver checking
        if (state.activeMode == ActiveMode.EQUATION) {
            solveEquations()
            return
        }

        // Direct Table config checking
        if (state.activeMode == ActiveMode.TABLE && state.showTableConfigScreen) {
            generateTableData()
            return
        }

        // Dice/Coin simulation executions
        if (state.activeMode == ActiveMode.MATH_BOX) {
            if (state.mathBoxSubMode == MathBoxSubMode.DICE_CONFIG) {
                runDiceSimulation()
            } else if (state.mathBoxSubMode == MathBoxSubMode.COIN_CONFIG) {
                runCoinSimulation()
            }
            return
        }

        if (state.activeMode == ActiveMode.BASE_N) {
            evaluateBaseNMath()
            return
        }

        val rawInput = state.formulaInput
        if (rawInput.isBlank()) return

        // Replace custom text with parsed operations: e.g. Ans, e, π
        var parsedFormula = rawInput
            .replace("Ans", state.resultOutput)

        // Extract raw variables map
        val doubleVars = state.variableMap

        try {
            val rawResult = mathEngine.evaluateExpression(
                parsedFormula,
                doubleVars,
                state.angleUnit
            )
            val resultText = formatDouble(rawResult)

            val updatedHistory = listOf(Pair(rawInput, resultText)) + state.evaluationHistory

            _uiState.update {
                it.copy(
                    resultOutput = resultText,
                    isErrorState = false,
                    evaluationHistory = updatedHistory.take(20)
                )
            }

            // Save to room db
            viewModelScope.launch {
                try {
                    db.dao().insertHistory(HistoryEntity(input = rawInput, output = resultText))
                } catch (e: Exception) {
                    // Safe logic
                }
            }

        } catch (e: ArithmeticException) {
            _uiState.update { it.copy(isErrorState = true, errorMsg = e.message ?: "Syntax ERROR") }
        } catch (e: Exception) {
            _uiState.update { it.copy(isErrorState = true, errorMsg = "Syntax ERROR") }
        }
    }

    /**
     * Base-N calculation utility.
     */
    private fun evaluateBaseNMath() {
        val state = _uiState.value
        val input = state.baseNInputVal
        if (input.isBlank()) return
        
        try {
            val radix = when (state.baseNActiveMode) {
                BaseNMode.DEC -> 10
                BaseNMode.HEX -> 16
                BaseNMode.BIN -> 2
                BaseNMode.OCT -> 8
            }
            val cleanStr = input.replace(" ", "")
            val parsedVal = cleanStr.toLong(radix)
            
            // Format base value across systems
            val output = parsedVal.toString() // default dec decimal display
            _uiState.update { it.copy(baseNInputVal = output) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isErrorState = true, errorMsg = "Syntax ERROR") }
        }
    }

    /**
     * Generates f(x) and g(x) Table grid list rows
     */
    private fun generateTableData() {
        val state = _uiState.value
        val start = state.tableStart
        val end = state.tableEnd
        val step = state.tableStep

        if (step <= 0 || start >= end) {
            _uiState.update { it.copy(isErrorState = true, errorMsg = "Range ERROR") }
            return
        }

        val rows = mutableListOf<List<String>>()
        var currX = start
        var idx = 1

        val doubleVars = state.variableMap

        while (currX <= end + 1e-9) {
            val xStr = formatDouble(currX)
            val fxVal = try {
                val r = mathEngine.evaluateFunc(state.tableFx, currX, doubleVars, state.angleUnit)
                formatDouble(r)
            } catch (e: Exception) {
                "ERROR"
            }
            val gxVal = try {
                val r = mathEngine.evaluateFunc(state.tableGx, currX, doubleVars, state.angleUnit)
                formatDouble(r)
            } catch (e: Exception) {
                "ERROR"
            }
            rows.add(listOf(idx.toString(), xStr, fxVal, gxVal))
            currX += step
            idx++
        }

        _uiState.update {
            it.copy(
                showTableConfigScreen = false,
                tableGeneratedRows = rows,
                isErrorState = false
            )
        }
    }

    /**
     * Update table range coefficients directly from configuration.
     */
    fun updateTableConfiguration(fx: String, gx: String, start: Double, end: Double, step: Double) {
        _uiState.update {
            it.copy(
                tableFx = fx,
                tableGx = gx,
                tableStart = start,
                tableEnd = end,
                tableStep = step
            )
        }
    }

    /**
     * Equation Matrix Cell selector updates.
     */
    fun selectEquationCell(row: Int, col: Int) {
        _uiState.update {
            it.copy(
                currentSelectedCellRow = row,
                currentSelectedCellCol = col
            )
        }
    }

    fun updateEquationCellCoefficient(value: Double) {
        val state = _uiState.value
        val r = state.currentSelectedCellRow
        val c = state.currentSelectedCellCol
        val updatedMatrix = state.simulCoefficients.mapIndexed { idx, arr ->
            if (idx == r) {
                val copy = arr.clone()
                if (c < copy.size) {
                    copy[c] = value
                }
                copy
            } else {
                arr
            }
        }
        _uiState.update { it.copy(simulCoefficients = updatedMatrix) }
    }

    fun selectEquationSubmode(subMode: EquationSubMode) {
        _uiState.update {
            it.copy(
                equationSubMode = subMode,
                equationSolutions = emptyList(),
                isErrorState = false
            )
        }
    }

    private fun solveEquations() {
        val state = _uiState.value
        val subMode = state.equationSubMode
        
        try {
            when (subMode) {
                EquationSubMode.SIMUL_2 -> {
                    val coeffs = state.simulCoefficients
                    val solutions = mathEngine.solveSimultaneous2(
                        arrayOf(coeffs[0].take(3).toDoubleArray(), coeffs[1].take(3).toDoubleArray())
                    )
                    _uiState.update {
                        it.copy(
                            equationSolutions = listOf(
                                "x = ${formatDouble(solutions[0])}",
                                "y = ${formatDouble(solutions[1])}"
                            ),
                            isErrorState = false
                        )
                    }
                }
                EquationSubMode.SIMUL_3 -> {
                    val coeffs = state.simulCoefficients
                    val solutions = mathEngine.solveSimultaneous3(
                        arrayOf(
                            coeffs[0].take(4).toDoubleArray(),
                            coeffs[1].take(4).toDoubleArray(),
                            coeffs[2].take(4).toDoubleArray()
                        )
                    )
                    _uiState.update {
                        it.copy(
                            equationSolutions = listOf(
                                "x = ${formatDouble(solutions[0])}",
                                "y = ${formatDouble(solutions[1])}",
                                "z = ${formatDouble(solutions[2])}"
                            ),
                            isErrorState = false
                        )
                    }
                }
                EquationSubMode.POLY_2 -> {
                    // Polynomial ax^2 + bx + c = 0
                    // Uses cells of first matrix row
                    val a = state.simulCoefficients[0][0]
                    val b = state.simulCoefficients[0][1]
                    val c = state.simulCoefficients[0][2]
                    
                    val res = mathEngine.solveQuadratic(a, b, c)
                    val solutionsList = mutableListOf(
                        "x1 = ${res.x1}",
                        "x2 = ${res.x2}",
                        "Min/Max x = ${formatDouble(res.minMaxX)}",
                        "Min/Max y = ${formatDouble(res.minMaxY)}"
                    )
                    _uiState.update {
                        it.copy(
                            equationSolutions = solutionsList,
                            isErrorState = false
                        )
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isErrorState = true,
                    errorMsg = "Math ERROR"
                )
            }
        }
    }

    /**
     * Math Box dice simulation trigger.
     */
    fun setMathBoxSubmode(subMode: MathBoxSubMode) {
        _uiState.update { it.copy(mathBoxSubMode = subMode, isErrorState = false) }
    }

    fun configureMathBox(dice: Int, coins: Int, attempts: Int) {
        _uiState.update {
            it.copy(
                diceCount = dice,
                coinCount = coins,
                attemptsCount = attempts
            )
        }
    }

    private fun runDiceSimulation() {
        val state = _uiState.value
        val res = mathEngine.simulateDice(state.diceCount, state.attemptsCount)
        _uiState.update {
            it.copy(
                diceTrialsResultList = res,
                mathBoxSubMode = MathBoxSubMode.DICE_RESULTS,
                isErrorState = false
            )
        }
    }

    private fun runCoinSimulation() {
        val state = _uiState.value
        val res = mathEngine.simulateCoins(state.coinCount, state.attemptsCount)
        _uiState.update {
            it.copy(
                coinTrialsResultList = res,
                mathBoxSubMode = MathBoxSubMode.COIN_RESULTS,
                isErrorState = false
            )
        }
    }

    fun toggleRelativeFrequencyGraph() {
        _uiState.update { it.copy(showRelativeFrequencyGraph = !it.showRelativeFrequencyGraph) }
    }

    /**
     * Settings reset operation.
     */
    fun resetAllSettings() {
        _uiState.update {
            CalculatorUiState(
                activeMode = ActiveMode.CALCULATE,
                angleUnit = "D"
            )
        }
        viewModelScope.launch {
            try {
                db.dao().clearHistory()
            } catch (e: Exception) {
                // Ignore safe errors
            }
        }
    }

    fun toggleAngleUnit() {
        val unit = if (_uiState.value.angleUnit == "D") "R" else "D"
        _uiState.update { it.copy(angleUnit = unit) }
    }

    fun animateBatteryState() {
        val currentLevel = _uiState.value.batteryLevel
        // Cycle 1 -> 2 -> 3 -> 1
        val nextLevel = if (currentLevel >= 3) 1 else currentLevel + 1
        _uiState.update { it.copy(batteryLevel = nextLevel) }
    }
}
