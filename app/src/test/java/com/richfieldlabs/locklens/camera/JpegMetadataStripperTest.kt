package com.richfieldlabs.locklens.camera

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpegMetadataStripperTest {
    @Test
    fun strip_removesAppAndCommentSegmentsButKeepsImagePayload() {
        val jpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x04.toByte(), 0x11.toByte(), 0x22.toByte(),
            0xFF.toByte(), 0xE1.toByte(), 0x00.toByte(), 0x05.toByte(), 0x41.toByte(), 0x42.toByte(), 0x43.toByte(),
            0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x04.toByte(), 0x44.toByte(), 0x45.toByte(),
            0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x04.toByte(), 0x55.toByte(), 0x66.toByte(),
            0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(), 0x04.toByte(), 0x77.toByte(), 0x88.toByte(),
            0x12.toByte(), 0x34.toByte(), 0xFF.toByte(), 0x00.toByte(), 0x56.toByte(),
            0xFF.toByte(), 0xD9.toByte(),
        )

        val stripped = JpegMetadataStripper.strip(jpeg)

        val expected = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x04.toByte(), 0x55.toByte(), 0x66.toByte(),
            0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(), 0x04.toByte(), 0x77.toByte(), 0x88.toByte(),
            0x12.toByte(), 0x34.toByte(), 0xFF.toByte(), 0x00.toByte(), 0x56.toByte(),
            0xFF.toByte(), 0xD9.toByte(),
        )

        assertArrayEquals(expected, stripped)
    }

    @Test
    fun strip_returnsOriginalBytesForNonJpegData() {
        val bytes = "not-a-jpeg".encodeToByteArray()

        assertArrayEquals(bytes, JpegMetadataStripper.strip(bytes))
    }
}
