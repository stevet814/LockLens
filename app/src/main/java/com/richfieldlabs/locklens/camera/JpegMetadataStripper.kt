package com.richfieldlabs.locklens.camera

import java.io.ByteArrayOutputStream
import java.io.File

internal object JpegMetadataStripper {
    private const val MARKER_PREFIX = 0xFF
    private const val MARKER_SOI = 0xD8
    private const val MARKER_EOI = 0xD9
    private const val MARKER_SOS = 0xDA
    private const val MARKER_TEM = 0x01
    private const val MARKER_COM = 0xFE
    private const val MARKER_RST0 = 0xD0
    private const val MARKER_RST7 = 0xD7
    private const val MARKER_APP0 = 0xE0
    private const val MARKER_APP15 = 0xEF

    fun strip(file: File) {
        val original = file.readBytes()
        val sanitized = strip(original)
        if (!sanitized.contentEquals(original)) {
            file.writeBytes(sanitized)
        }
    }

    fun strip(bytes: ByteArray): ByteArray {
        if (!bytes.isJpeg()) return bytes

        val output = ByteArrayOutputStream(bytes.size)
        output.write(MARKER_PREFIX)
        output.write(MARKER_SOI)

        var index = 2
        loop@ while (index < bytes.size) {
            if (bytes[index].unsigned() != MARKER_PREFIX) {
                output.write(bytes, index, bytes.size - index)
                break
            }

            while (index < bytes.size && bytes[index].unsigned() == MARKER_PREFIX) {
                index++
            }
            if (index >= bytes.size) return bytes

            val marker = bytes[index].unsigned()
            index++

            when {
                marker == MARKER_EOI -> {
                    output.write(MARKER_PREFIX)
                    output.write(marker)
                    break@loop
                }
                marker == MARKER_TEM || marker in MARKER_RST0..MARKER_RST7 -> {
                    output.write(MARKER_PREFIX)
                    output.write(marker)
                }
                else -> {
                    if (index + 1 >= bytes.size) return bytes
                    val segmentLength = readUnsignedShort(bytes, index)
                    if (segmentLength < 2 || index + segmentLength > bytes.size) return bytes

                    if (!marker.shouldStrip()) {
                        output.write(MARKER_PREFIX)
                        output.write(marker)
                        output.write(bytes, index, segmentLength)
                    }
                    index += segmentLength

                    if (marker == MARKER_SOS) {
                        output.write(bytes, index, bytes.size - index)
                        break@loop
                    }
                }
            }
        }

        return output.toByteArray()
    }

    private fun Int.shouldStrip(): Boolean =
        this == MARKER_COM || this in MARKER_APP0..MARKER_APP15

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].unsigned() shl 8) or bytes[offset + 1].unsigned()

    private fun ByteArray.isJpeg(): Boolean =
        size >= 2 && this[0].unsigned() == MARKER_PREFIX && this[1].unsigned() == MARKER_SOI

    private fun Byte.unsigned(): Int = toInt() and 0xFF
}
