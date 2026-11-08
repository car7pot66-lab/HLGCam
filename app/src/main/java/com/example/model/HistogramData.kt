package com.example.model

data class HistogramData(
    val red: FloatArray = FloatArray(BIN_COUNT),
    val green: FloatArray = FloatArray(BIN_COUNT),
    val blue: FloatArray = FloatArray(BIN_COUNT),
    val luma: FloatArray = FloatArray(BIN_COUNT)
) {
    companion object {
        const val BIN_COUNT = 64
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HistogramData
        return red.contentEquals(other.red) &&
                green.contentEquals(other.green) &&
                blue.contentEquals(other.blue) &&
                luma.contentEquals(other.luma)
    }

    override fun hashCode(): Int {
        var result = red.contentHashCode()
        result = 31 * result + green.contentHashCode()
        result = 31 * result + blue.contentHashCode()
        result = 31 * result + luma.contentHashCode()
        return result
    }
}
