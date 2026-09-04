package com.zen.fnftools.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.io.ByteArrayOutputStream

/**
 * Minimal GIF89a decoder. android.graphics.Movie is deprecated and, on many
 * modern devices, fails to render transparency correctly (frames come out
 * as solid black) — so we parse the format directly instead of trusting it.
 */
object GifDecoder {

    private class Reader(private val data: ByteArray) {
        var pos = 0
        fun readByte(): Int = data[pos++].toInt() and 0xFF
        fun readShort(): Int {
            val lo = readByte()
            val hi = readByte()
            return lo or (hi shl 8)
        }
        fun readBytes(n: Int): ByteArray {
            val out = data.copyOfRange(pos, pos + n)
            pos += n
            return out
        }
        fun skip(n: Int) { pos += n }
        fun hasMore(): Boolean = pos < data.size
    }

    fun decode(bytes: ByteArray): List<Bitmap> {
        val r = Reader(bytes)

        val sig = String(r.readBytes(6), Charsets.US_ASCII)
        require(sig.startsWith("GIF")) { "Not a GIF file" }

        val screenWidth = r.readShort()
        val screenHeight = r.readShort()
        val packed = r.readByte()
        val gctFlag = (packed and 0x80) != 0
        val gctSize = 2 shl (packed and 0x07)
        r.readByte() // background color index
        r.readByte() // pixel aspect ratio

        val globalColorTable: IntArray? = if (gctFlag) readColorTable(r, gctSize) else null

        val frames = mutableListOf<Bitmap>()
        var canvas = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        var savedForRestore: Bitmap? = null

        var transparentIndex = -1
        var transparencyEnabled = false
        var disposalMethod = 0

        while (r.hasMore()) {
            val blockType = r.readByte()
            when (blockType) {
                0x21 -> { // Extension
                    val label = r.readByte()
                    if (label == 0xF9) {
                        r.readByte() // block size, always 4
                        val gcePacked = r.readByte()
                        disposalMethod = (gcePacked shr 2) and 0x07
                        transparencyEnabled = (gcePacked and 0x01) != 0
                        r.readShort() // delay time (unused — caller controls sampling)
                        transparentIndex = r.readByte()
                        r.readByte() // block terminator
                    } else {
                        skipSubBlocks(r)
                    }
                }
                0x2C -> { // Image Descriptor
                    val imgLeft = r.readShort()
                    val imgTop = r.readShort()
                    val imgWidth = r.readShort()
                    val imgHeight = r.readShort()
                    val imgPacked = r.readByte()
                    val lctFlag = (imgPacked and 0x80) != 0
                    val interlaced = (imgPacked and 0x40) != 0
                    val lctSize = 2 shl (imgPacked and 0x07)

                    val colorTable = if (lctFlag) readColorTable(r, lctSize) else (globalColorTable ?: IntArray(256))

                    val minCodeSize = r.readByte()
                    val lzwData = readSubBlocksConcatenated(r)
                    val indices = lzwDecode(lzwData, minCodeSize, imgWidth * imgHeight)

                    if (disposalMethod == 3) {
                        savedForRestore = canvas.copy(Bitmap.Config.ARGB_8888, true)
                    }

                    val pixels = IntArray(imgWidth * imgHeight)
                    if (interlaced) {
                        val rowOrder = interlacedRowOrder(imgHeight)
                        var srcIndex = 0
                        for (row in rowOrder) {
                            for (col in 0 until imgWidth) {
                                val idx = indices.getOrElse(srcIndex++) { 0 }
                                pixels[row * imgWidth + col] = pixelFor(idx, colorTable, transparencyEnabled, transparentIndex)
                            }
                        }
                    } else {
                        for (i in 0 until imgWidth * imgHeight) {
                            val idx = indices.getOrElse(i) { 0 }
                            pixels[i] = pixelFor(idx, colorTable, transparencyEnabled, transparentIndex)
                        }
                    }

                    val frameBitmap = Bitmap.createBitmap(pixels, imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
                    Canvas(canvas).drawBitmap(frameBitmap, imgLeft.toFloat(), imgTop.toFloat(), null)

                    frames.add(canvas.copy(Bitmap.Config.ARGB_8888, true))

                    when (disposalMethod) {
                        2 -> {
                            val cleared = canvas.copy(Bitmap.Config.ARGB_8888, true)
                            val clearPaint = Paint().apply {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                            }
                            Canvas(cleared).drawRect(
                                imgLeft.toFloat(), imgTop.toFloat(),
                                (imgLeft + imgWidth).toFloat(), (imgTop + imgHeight).toFloat(),
                                clearPaint
                            )
                            canvas = cleared
                        }
                        3 -> {
                            savedForRestore?.let { canvas = it.copy(Bitmap.Config.ARGB_8888, true) }
                        }
                        else -> { /* leave canvas as-is */ }
                    }

                    transparencyEnabled = false
                    transparentIndex = -1
                    disposalMethod = 0
                }
                0x3B -> break // Trailer
                else -> break // Unknown/corrupt block — stop gracefully with what we have
            }
        }

        return frames
    }

    private fun pixelFor(idx: Int, colorTable: IntArray, transparencyEnabled: Boolean, transparentIndex: Int): Int {
        if (transparencyEnabled && idx == transparentIndex) return 0
        val rgb = colorTable.getOrElse(idx) { 0 }
        return rgb or (0xFF shl 24)
    }

    private fun readColorTable(r: Reader, size: Int): IntArray {
        val table = IntArray(size)
        for (i in 0 until size) {
            val red = r.readByte()
            val green = r.readByte()
            val blue = r.readByte()
            table[i] = (red shl 16) or (green shl 8) or blue
        }
        return table
    }

    private fun skipSubBlocks(r: Reader) {
        while (true) {
            val size = r.readByte()
            if (size == 0) break
            r.skip(size)
        }
    }

    private fun readSubBlocksConcatenated(r: Reader): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val size = r.readByte()
            if (size == 0) break
            out.write(r.readBytes(size))
        }
        return out.toByteArray()
    }

    private fun interlacedRowOrder(height: Int): List<Int> {
        val rows = mutableListOf<Int>()
        var row = 0
        while (row < height) { rows.add(row); row += 8 }
        row = 4
        while (row < height) { rows.add(row); row += 8 }
        row = 2
        while (row < height) { rows.add(row); row += 4 }
        row = 1
        while (row < height) { rows.add(row); row += 2 }
        return rows
    }

    /** Standard GIF LZW decompression into a flat array of color-table indices. */
    private fun lzwDecode(data: ByteArray, minCodeSize: Int, pixelCount: Int): IntArray {
        val output = IntArray(pixelCount)
        var outputPos = 0

        val clearCode = 1 shl minCodeSize
        val endCode = clearCode + 1

        fun freshDict(): MutableList<IntArray> {
            val dict = MutableList(clearCode) { intArrayOf(it) }
            dict.add(IntArray(0)) // placeholder for clear code
            dict.add(IntArray(0)) // placeholder for end code
            return dict
        }

        var dict = freshDict()
        var codeSize = minCodeSize + 1
        var prevEntry: IntArray? = null

        var bitBuffer = 0
        var bitCount = 0
        var dataPos = 0

        fun readCode(): Int {
            while (bitCount < codeSize) {
                if (dataPos >= data.size) return endCode
                bitBuffer = bitBuffer or ((data[dataPos].toInt() and 0xFF) shl bitCount)
                dataPos++
                bitCount += 8
            }
            val code = bitBuffer and ((1 shl codeSize) - 1)
            bitBuffer = bitBuffer ushr codeSize
            bitCount -= codeSize
            return code
        }

        while (outputPos < pixelCount) {
            val code = readCode()

            if (code == clearCode) {
                dict = freshDict()
                codeSize = minCodeSize + 1
                prevEntry = null
                continue
            }
            if (code == endCode) break

            val entry: IntArray = when {
                code < dict.size -> dict[code]
                code == dict.size && prevEntry != null -> prevEntry + prevEntry[0]
                else -> break
            }

            for (b in entry) {
                if (outputPos < pixelCount) output[outputPos++] = b else break
            }

            if (prevEntry != null) {
                dict.add(prevEntry + entry[0])
                if (dict.size == (1 shl codeSize) && codeSize < 12) {
                    codeSize++
                }
            }

            prevEntry = entry
        }

        return output
    }
}
