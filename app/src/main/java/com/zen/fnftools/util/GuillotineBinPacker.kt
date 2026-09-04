package com.zen.fnftools.util

/**
 * Simple guillotine-split rectangle bin packer: places each item in the
 * free rectangle that wastes the least area (best-area-fit), then splits
 * the remainder along whichever axis leaves the larger single leftover
 * piece, which tends to keep future placements flexible.
 */
class GuillotineBinPacker(width: Int, height: Int) {

    data class Rect(val x: Int, val y: Int, val w: Int, val h: Int)

    private val freeRects = mutableListOf(Rect(0, 0, width, height))

    fun insert(w: Int, h: Int): Rect? {
        var bestIndex = -1
        var bestLeftoverArea = Int.MAX_VALUE

        for (i in freeRects.indices) {
            val fr = freeRects[i]
            if (fr.w >= w && fr.h >= h) {
                val leftover = fr.w * fr.h - w * h
                if (leftover < bestLeftoverArea) {
                    bestLeftoverArea = leftover
                    bestIndex = i
                }
            }
        }

        if (bestIndex == -1) return null

        val fr = freeRects.removeAt(bestIndex)
        val placed = Rect(fr.x, fr.y, w, h)

        val remainderW = fr.w - w
        val remainderH = fr.h - h

        if (remainderW > remainderH) {
            freeRects.add(Rect(fr.x + w, fr.y, remainderW, fr.h))
            if (remainderH > 0) freeRects.add(Rect(fr.x, fr.y + h, w, remainderH))
        } else {
            freeRects.add(Rect(fr.x, fr.y + h, fr.w, remainderH))
            if (remainderW > 0) freeRects.add(Rect(fr.x + w, fr.y, remainderW, h))
        }

        freeRects.removeAll { it.w <= 0 || it.h <= 0 }
        return placed
    }
}
