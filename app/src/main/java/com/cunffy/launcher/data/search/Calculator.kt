package com.cunffy.launcher.data.search

/**
 * Tiny recursive-descent evaluator for arithmetic search queries
 * (`+ - * / % ^`, parentheses, decimals). Returns null for anything that isn't a
 * well-formed expression so non-math queries fall through to other providers.
 */
object Calculator {

    fun evaluate(input: String): Double? {
        val expr = input.trim()
        // Require it to actually look like math (a digit and an operator) to avoid hijacking words.
        if (!expr.any { it.isDigit() } || !expr.any { it in "+-*/%^" }) return null
        return runCatching { Parser(expr).parse() }.getOrNull()
    }

    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val value = parseExpression()
            skipSpaces()
            if (pos != s.length) throw IllegalArgumentException("trailing input")
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { pos++; value += parseTerm() }
                    '-' -> { pos++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { pos++; value *= parseFactor() }
                    '/' -> { pos++; value /= parseFactor() }
                    '%' -> { pos++; value %= parseFactor() }
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            skipSpaces()
            val base = parsePrimary()
            skipSpaces()
            return if (peek() == '^') {
                pos++
                Math.pow(base, parseFactor())
            } else {
                base
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            when (peek()) {
                '(' -> {
                    pos++
                    val value = parseExpression()
                    skipSpaces()
                    require(peek() == ')') { "expected )" }
                    pos++
                    return value
                }
                '-' -> { pos++; return -parsePrimary() }
                '+' -> { pos++; return parsePrimary() }
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "expected number" }
            return s.substring(start, pos).toDouble()
        }

        private fun peek(): Char = if (pos < s.length) s[pos] else ' '
        private fun skipSpaces() { while (pos < s.length && s[pos] == ' ') pos++ }
    }
}
