package com.cunffy.launcher.data.search.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Offline unit conversions for queries like `10 km to miles` or `72 f to c`. Covers length,
 * mass, and temperature; currency is intentionally excluded (needs live rates).
 */
class UnitConversionSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        val match = PATTERN.find(query.trim().lowercase()) ?: return emptyList()
        val (amountStr, fromRaw, toRaw) = match.destructured
        val amount = amountStr.toDoubleOrNull() ?: return emptyList()
        val from = UNIT_ALIASES[fromRaw] ?: return emptyList()
        val to = UNIT_ALIASES[toRaw] ?: return emptyList()
        if (from.dimension != to.dimension) return emptyList()

        val result = convert(amount, from, to) ?: return emptyList()
        val formatted = format(result)
        return listOf(
            SearchResult(
                id = "conv:$query",
                title = "$formatted ${to.display}",
                subtitle = "$amount ${from.display}",
                type = SearchResultType.ANSWER,
                icon = ResultIcon.OfVector(Icons.Rounded.SwapHoriz),
                score = 95,
                onActivate = { /* informational */ },
            ),
        )
    }

    private fun convert(amount: Double, from: UnitDef, to: UnitDef): Double? = when (from.dimension) {
        Dimension.TEMPERATURE -> {
            val celsius = when (from.display) {
                "°F" -> (amount - 32) * 5 / 9
                "K" -> amount - 273.15
                else -> amount
            }
            when (to.display) {
                "°F" -> celsius * 9 / 5 + 32
                "K" -> celsius + 273.15
                else -> celsius
            }
        }
        else -> amount * from.factor / to.factor
    }

    private fun format(value: Double): String =
        if (kotlin.math.abs(value - value.roundToLong()) < 1e-9) value.roundToLong().toString()
        else String.format("%.4f", value).trimEnd('0').trimEnd('.')

    private enum class Dimension { LENGTH, MASS, TEMPERATURE }
    private data class UnitDef(val display: String, val dimension: Dimension, val factor: Double)

    private companion object {
        val PATTERN = Regex("""^([\d.]+)\s*([a-z°]+)\s+(?:to|in)\s+([a-z°]+)$""")

        // factor = value of one unit in the dimension's base (metre / kilogram).
        val UNITS = listOf(
            UnitDef("m", Dimension.LENGTH, 1.0), UnitDef("km", Dimension.LENGTH, 1000.0),
            UnitDef("cm", Dimension.LENGTH, 0.01), UnitDef("mm", Dimension.LENGTH, 0.001),
            UnitDef("mi", Dimension.LENGTH, 1609.344), UnitDef("yd", Dimension.LENGTH, 0.9144),
            UnitDef("ft", Dimension.LENGTH, 0.3048), UnitDef("in", Dimension.LENGTH, 0.0254),
            UnitDef("kg", Dimension.MASS, 1.0), UnitDef("g", Dimension.MASS, 0.001),
            UnitDef("mg", Dimension.MASS, 1e-6), UnitDef("lb", Dimension.MASS, 0.45359237),
            UnitDef("oz", Dimension.MASS, 0.028349523),
            UnitDef("°C", Dimension.TEMPERATURE, 1.0), UnitDef("°F", Dimension.TEMPERATURE, 1.0),
            UnitDef("K", Dimension.TEMPERATURE, 1.0),
        )

        val UNIT_ALIASES: Map<String, UnitDef> = buildMap {
            fun bind(unit: UnitDef, vararg names: String) = names.forEach { put(it, unit) }
            val by = UNITS.associateBy { it.display.removePrefix("°").lowercase() }
            bind(by.getValue("m"), "m", "meter", "meters", "metre", "metres")
            bind(by.getValue("km"), "km", "kilometer", "kilometers", "kilometre", "kilometres")
            bind(by.getValue("cm"), "cm", "centimeter", "centimeters")
            bind(by.getValue("mm"), "mm", "millimeter", "millimeters")
            bind(by.getValue("mi"), "mi", "mile", "miles")
            bind(by.getValue("yd"), "yd", "yard", "yards")
            bind(by.getValue("ft"), "ft", "foot", "feet")
            bind(by.getValue("in"), "in", "inch", "inches")
            bind(by.getValue("kg"), "kg", "kilogram", "kilograms")
            bind(by.getValue("g"), "g", "gram", "grams")
            bind(by.getValue("mg"), "mg", "milligram", "milligrams")
            bind(by.getValue("lb"), "lb", "lbs", "pound", "pounds")
            bind(by.getValue("oz"), "oz", "ounce", "ounces")
            bind(by.getValue("c"), "c", "°c", "celsius", "centigrade")
            bind(by.getValue("f"), "f", "°f", "fahrenheit")
            bind(by.getValue("k"), "k", "kelvin")
        }
    }
}
