package com.zen.fnftools.util

import kotlin.math.max
import kotlin.math.min

object GifFrameExtractor {

    /**
     * Decodes every real frame from the GIF via GifDecoder, then samples
     * [frameCount] of them evenly. If [frameCount] is 0 or covers every
     * frame already, all real frames are used as-is (no duplicate sampling).
     *
     * Returns named bitmaps ("frame0000", "frame0001", ...) ready to hand
     * straight to SpritesheetPacker.
     */
    fun extract(gifBytes: ByteArray, frameCount: Int, namePrefix: String = "frame"): List<NamedBitmap> {
        val allFrames = GifDecoder.decode(gifBytes)
        if (allFrames.isEmpty()) {
            throw IllegalArgumentException("Could not decode GIF — file may be corrupt or not a GIF89a.")
        }

        val indices: List<Int> = if (frameCount <= 0 || frameCount >= allFrames.size) {
            allFrames.indices.toList()
        } else {
            val count = max(min(frameCount, allFrames.size), 1)
            (0 until count).map { i -> (i * allFrames.size) / count }
        }

        return indices.mapIndexed { i, idx ->
            NamedBitmap("$namePrefix${i.toString().padStart(4, '0')}", allFrames[idx])
        }
    }
}
