package org.sz.drillironc.entities

data class HeaterProfile(val Title: String, val HeaterID: Int, val Adder: Double, val Multiplier:Double,
                         val DefaultTemperatureLow: Int, val DefaultTemperatureHigh: Int, val DualTemperatureEnable: Boolean) {
    override fun toString(): String {
        return Title
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HeaterProfile

        if (Title != other.Title) return false

        return true
    }

    override fun hashCode(): Int {
        return Title.hashCode()
    }
}
