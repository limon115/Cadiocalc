package com.example.logic

import kotlin.math.*
import java.util.Locale
import java.util.Stack
import java.util.Random

/**
 * Custom complex number representations.
 */
data class ComplexNumber(val real: Double, val imag: Double) {
    operator fun plus(o: ComplexNumber) = ComplexNumber(real + o.real, imag + o.imag)
    operator fun minus(o: ComplexNumber) = ComplexNumber(real - o.real, imag - o.imag)
    operator fun times(o: ComplexNumber) = ComplexNumber(
        real * o.real - imag * o.imag,
        real * o.imag + imag * o.real
    )
    operator fun div(o: ComplexNumber): ComplexNumber {
        val denom = o.real * o.real + o.imag * o.imag
        if (denom == 0.0) return ComplexNumber(Double.NaN, Double.NaN)
        return ComplexNumber(
            (real * o.real + imag * o.imag) / denom,
            (imag * o.real - real * o.imag) / denom
        )
    }

    override fun toString(): String {
        if (imag.isNaN() || real.isNaN()) return "ERROR"
        val cleanReal = formatDouble(real)
        val cleanImag = formatDouble(abs(imag))
        return when {
            imag == 0.0 -> cleanReal
            real == 0.0 -> if (imag < 0) "-${cleanImag}i" else "${cleanImag}i"
            imag < 0 -> "$cleanReal - ${cleanImag}i"
            else -> "$cleanReal + ${cleanImag}i"
        }
    }
}

/**
 * Utility to format double results nicely, removing trailing .0 where appropriate.
 */
fun formatDouble(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val rounded = (value * 1e10).roundToLong() / 1e10
    if (rounded == rounded.toLong().toDouble()) {
        return rounded.toLong().toString()
    }
    return String.format(Locale.US, "%.8f", rounded).trimEnd('0').trimEnd('.')
}

class MathEngine {

    /**
     * Parse and evaluate basic real-number math expressions.
     */
    fun evaluateExpression(
        expression: String,
        variables: Map<String, Double>,
        angleMode: String // "D" for Degree, "R" for Radian
    ): Double {
        if (expression.isBlank()) return 0.0

        try {
            val tokens = tokenize(expression, variables)
            val rpn = infixToPostfix(tokens)
            return evaluatePostfix(rpn, angleMode)
        } catch (e: Exception) {
            throw ArithmeticException("Syntax ERROR")
        }
    }

    private fun tokenize(expr: String, variables: Map<String, Double>): List<String> {
        val clean = expr.replace(" ", "")
        val result = mutableListOf<String>()
        var i = 0

        while (i < clean.length) {
            val c = clean[i]

            // Numbers
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                    sb.append(clean[i])
                    i++
                }
                result.add(sb.toString())
                continue
            }

            // Constants
            if (c == 'π' || c == 'p' && i + 1 < clean.length && clean[i+1] == 'i') {
                result.add("π")
                i += if (c == 'p') 2 else 1
                continue
            }
            if (c == 'e' && (i + 1 == clean.length || !clean[i+1].isLetter())) {
                result.add("e")
                i++
                continue
            }

            // Word-based functions
            if (c.isLetter()) {
                val sb = StringBuilder()
                while (i < clean.length && (clean[i].isLetter() || clean[i].isDigit() || clean[i] == '⁻' || clean[i] == '¹')) {
                    sb.append(clean[i])
                    i++
                }
                val word = sb.toString()

                // Check if it is a registered variable in the map
                if (variables.containsKey(word.uppercase())) {
                    result.add(word.uppercase())
                } else {
                    result.add(word)
                }
                continue
            }

            // Operators and Brackets
            result.add(c.toString())
            i++
        }

        // Insert implicit multiplication: e.g. 2π -> 2 * π, (3+1)5 -> (3+1) * 5, 2sin(30) -> 2 * sin(30)
        val expanded = mutableListOf<String>()
        for (j in 0 until result.size) {
            val current = result[j]
            expanded.add(current)
            if (j + 1 < result.size) {
                val next = result[j + 1]
                if (isOperandOrFuncOpener(current) && isOperandOrFuncCloser(next)) {
                    expanded.add("*")
                }
            }
        }

        return expanded
    }

    private fun isOperandOrFuncOpener(tok: String): Boolean {
        // Can a multiplier precede this? e.g. closing brackets, variables, numbers, constants
        if (tok == ")" || tok == "π" || tok == "e") return true
        val firstChar = tok.firstOrNull() ?: return false
        if (firstChar.isDigit() || firstChar == '.') return true
        // If it's a variable
        if (tok.length == 1 && tok[0] in 'A'..'Z') return true
        return false
    }

    private fun isOperandOrFuncCloser(tok: String): Boolean {
        // Can an implicit multiplication start with this? e.g. opening bracket, function name, variables, numbers
        if (tok == "(" || tok == "π" || tok == "e" || tok == "√") return true
        if (tok.length > 1 && tok.first().isLetter()) return true // function call e.g. sin, cos
        val firstChar = tok.firstOrNull() ?: return false
        if (firstChar.isDigit() || firstChar == '.') return true
        if (tok.length == 1 && tok[0] in 'A'..'Z') return true
        return false
    }

    private fun infixToPostfix(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operators = Stack<String>()

        var expectUnary = true

        for (token in tokens) {
            when {
                // If it is a number or variable or constant, send it to output
                isNumber(token) || token == "π" || token == "e" || (token.length == 1 && token[0] in 'A'..'Z') -> {
                    output.add(token)
                    expectUnary = false
                }

                // If it is a function name, push it to stack
                isFunction(token) || token == "√" -> {
                    operators.push(token)
                    expectUnary = true
                }

                token == "(" -> {
                    operators.push(token)
                    expectUnary = true
                }

                token == ")" -> {
                    while (operators.isNotEmpty() && operators.peek() != "(") {
                        output.add(operators.pop())
                    }
                    if (operators.isNotEmpty() && operators.peek() == "(") {
                        operators.pop() // remove '('
                    }
                    if (operators.isNotEmpty() && (isFunction(operators.peek()) || operators.peek() == "√")) {
                        output.add(operators.pop())
                    }
                    expectUnary = false
                }

                isOperator(token) -> {
                    var op = token
                    // Handle unary minus / plus
                    if (expectUnary && op == "-") {
                        op = "u-"
                    } else if (expectUnary && op == "+") {
                        op = "u+"
                    }

                    while (operators.isNotEmpty() && shouldPopOperator(operators.peek(), op)) {
                        output.add(operators.pop())
                    }
                    operators.push(op)
                    expectUnary = true
                }
            }
        }

        while (operators.isNotEmpty()) {
            val op = operators.pop()
            if (op != "(" && op != ")") {
                output.add(op)
            }
        }

        return output
    }

    private fun shouldPopOperator(top: String, next: String): Boolean {
        if (top == "(") return false
        val p1 = getPrecedence(top)
        val p2 = getPrecedence(next)
        if (p1 == p2 && isRightAssociative(next)) return false
        return p1 >= p2
    }

    private fun getPrecedence(op: String): Int {
        return when (op) {
            "u-", "u+" -> 6
            "^", "√" -> 5
            "*", "/" -> 4
            "+", "-" -> 3
            else -> 0
        }
    }

    private fun isRightAssociative(op: String): Boolean {
        return op == "^" || op == "√" || op == "u-" || op == "u+"
    }

    private fun isNumber(s: String): Boolean {
        val first = s.firstOrNull() ?: return false
        return first.isDigit() || first == '.'
    }

    private fun isOperator(s: String): Boolean {
        return s == "+" || s == "-" || s == "*" || s == "/" || s == "^"
    }

    private fun isFunction(s: String): Boolean {
        return s == "sin" || s == "cos" || s == "tan" ||
               s == "sin⁻¹" || s == "cos⁻¹" || s == "tan⁻¹" ||
               s == "ln" || s == "log" || s == "Abs"
    }

    private fun evaluatePostfix(postfix: List<String>, angleMode: String): Double {
        val stack = Stack<Double>()

        for (token in postfix) {
            if (isNumber(token)) {
                stack.push(token.toDouble())
            } else if (token == "π") {
                stack.push(PI)
            } else if (token == "e") {
                stack.push(E)
            } else if (token.length == 1 && token[0] in 'A'..'Z') {
                // If a variable remains un-substituted (could be default zero)
                stack.push(0.0)
            } else if (token == "u-") {
                val a = stack.pop()
                stack.push(-a)
            } else if (token == "u+") {
                // do nothing
            } else if (token == "√") {
                val a = stack.pop()
                stack.push(sqrt(a))
            } else if (isFunction(token)) {
                val a = stack.pop()
                val res = when (token) {
                    "sin" -> {
                        val rad = if (angleMode == "D") Math.toRadians(a) else a
                        sin(rad)
                    }
                    "cos" -> {
                        val rad = if (angleMode == "D") Math.toRadians(a) else a
                        cos(rad)
                    }
                    "tan" -> {
                        val rad = if (angleMode == "D") Math.toRadians(a) else a
                        tan(rad)
                    }
                    "sin⁻¹" -> {
                        val r = asin(a)
                        if (angleMode == "D") Math.toDegrees(r) else r
                    }
                    "cos⁻¹" -> {
                        val r = acos(a)
                        if (angleMode == "D") Math.toDegrees(r) else r
                    }
                    "tan⁻¹" -> {
                        val r = atan(a)
                        if (angleMode == "D") Math.toDegrees(r) else r
                    }
                    "ln" -> ln(a)
                    "log" -> log10(a)
                    "Abs" -> abs(a)
                    else -> 0.0
                }
                stack.push(res)
            } else {
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Math ERROR")
                        a / b
                    }
                    "^" -> a.pow(b)
                    else -> 0.0
                }
                stack.push(res)
            }
        }

        if (stack.size != 1) throw Exception("Evaluate error")
        return stack.pop()
    }

    /**
     * Variable Substitution helper for math templates.
     */
    fun evaluateFunc(
        expr: String,
        xValue: Double,
        variables: Map<String, Double>,
        angleMode: String
    ): Double {
        val vars = variables.toMutableMap()
        vars["X"] = xValue
        vars["x"] = xValue
        return evaluateExpression(expr, vars, angleMode)
    }

    /**
     * Simultaneous Equation Solver
     * Solves determinants dynamically.
     */
    fun solveSimultaneous2(coefficients: Array<DoubleArray>): DoubleArray {
        // [a1, b1, c1]
        // [a2, b2, c2]
        val a1 = coefficients[0][0]
        val b1 = coefficients[0][1]
        val c1 = coefficients[0][2]

        val a2 = coefficients[1][0]
        val b2 = coefficients[1][1]
        val c2 = coefficients[1][2]

        val d = a1 * b2 - b1 * a2
        if (d == 0.0) throw ArithmeticException("No unique solution")

        val dx = c1 * b2 - b1 * c2
        val dy = a1 * c2 - c1 * a2

        return doubleArrayOf(dx / d, dy / d)
    }

    fun solveSimultaneous3(coefficients: Array<DoubleArray>): DoubleArray {
        val a1 = coefficients[0][0]; val b1 = coefficients[0][1]; val c1 = coefficients[0][2]; val d1 = coefficients[0][3]
        val a2 = coefficients[1][0]; val b2 = coefficients[1][1]; val c2 = coefficients[1][2]; val d2 = coefficients[1][3]
        val a3 = coefficients[2][0]; val b3 = coefficients[2][1]; val c3 = coefficients[2][2]; val d3 = coefficients[2][3]

        // Determinant Helper
        val d = det3x3(
            a1, b1, c1,
            a2, b2, c2,
            a3, b3, c3
        )

        if (d == 0.0) throw ArithmeticException("No unique solution")

        val dx = det3x3(
            d1, b1, c1,
            d2, b2, c2,
            d3, b3, c3
        )
        val dy = det3x3(
            a1, d1, c1,
            a2, d2, c2,
            a3, d3, c3
        )
        val dz = det3x3(
            a1, b1, d1,
            a2, b2, d2,
            a3, b3, d3
        )

        return doubleArrayOf(dx / d, dy / d, dz / d)
    }

    private fun det3x3(
        a: Double, b: Double, c: Double,
        d: Double, e: Double, f: Double,
        g: Double, h: Double, i: Double
    ): Double {
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    }

    /**
     * Polynomial Solver: Real and Complex roots
     */
    fun solveQuadratic(a: Double, b: Double, c: Double): QuadraticResult {
        if (a == 0.0) throw ArithmeticException("Leading coefficient cannot be zero")
        val d = b * b - 4 * a * c
        val xMinMaxX = -b / (2 * a)
        val xMinMaxY = d / (-4 * a)
        val isMin = a > 0

        return if (d >= 0.0) {
            val r1 = (-b + sqrt(d)) / (2 * a)
            val r2 = (-b - sqrt(d)) / (2 * a)
            QuadraticResult(
                ComplexNumber(r1, 0.0),
                ComplexNumber(r2, 0.0),
                xMinMaxX,
                xMinMaxY,
                isMin,
                quadraticRootsReal = true
            )
        } else {
            val real = -b / (2 * a)
            val imag = sqrt(-d) / (2 * a)
            QuadraticResult(
                ComplexNumber(real, imag),
                ComplexNumber(real, -imag),
                xMinMaxX,
                xMinMaxY,
                isMin,
                quadraticRootsReal = false
            )
        }
    }

    data class QuadraticResult(
        val x1: ComplexNumber,
        val x2: ComplexNumber,
        val minMaxX: Double,
        val minMaxY: Double,
        val isMinimum: Boolean,
        val quadraticRootsReal: Boolean
    )

    /**
     * Math Box Simulator: rolls/tosses
     */
    data class DiceTrial(val id: Int, val dice: List<Int>, val sum: Int, val diff: Int)

    fun simulateDice(diceCount: Int, attempts: Int): List<DiceTrial> {
        val rand = Random()
        val trials = mutableListOf<DiceTrial>()
        for (i in 1..attempts) {
            val diceValues = List(diceCount) { rand.nextInt(6) + 1 }
            val sum = diceValues.sum()
            val diff = if (diceCount >= 2) abs(diceValues[0] - diceValues[1]) else 0
            trials.add(DiceTrial(i, diceValues, sum, diff))
        }
        return trials
    }

    data class CoinTrial(val id: Int, val tosses: List<Boolean>, val headsCount: Int)

    fun simulateCoins(coinsCount: Int, attempts: Int): List<CoinTrial> {
        val rand = Random()
        val trials = mutableListOf<CoinTrial>()
        for (i in 1..attempts) {
            val tosses = List(coinsCount) { rand.nextBoolean() } // true = Heads, false = Tails
            val heads = tosses.count { it }
            trials.add(CoinTrial(i, tosses, heads))
        }
        return trials
    }
}
