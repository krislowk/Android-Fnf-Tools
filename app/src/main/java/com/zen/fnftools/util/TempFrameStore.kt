package com.zen.fnftools.util

/**
 * Bitmaps are too large/slow to pass through Intent extras, and this is a
 * single-process app, so we hand them off via a simple in-memory holder
 * between the picker screen and FrameManagerActivity.
 */
object TempFrameStore {
    var frames: MutableList<NamedBitmap> = mutableListOf()

    fun clear() {
        frames = mutableListOf()
    }
}
