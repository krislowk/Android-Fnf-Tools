package com.zen.fnftools.util

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class NamedBitmap(val name: String, val bitmap: Bitmap)

object SpritesheetPacker {

    /**
     * Packs bitmaps left-to-right into shelves (rows), wrapping once a row
     * would exceed maxWidth. Simple and predictable — good enough for the
     * frame counts a single FNF character/animation uses (rarely more than
     * a couple hundred frames).
     *
     * Returns the packed Bitmap plus the AtlasFrame list (in the same order
     * the bitmaps were given) describing where each ended up.
     */
    fun pack(bitmaps: List<NamedBitmap>, maxWidth: Int = 2048, padding: Int = 1): Pair<Bitmap, List<AtlasFrame>> {
        require(bitmaps.isNotEmpty()) { "Nothing to pack" }

        // If everything is wider than maxWidth on its own, grow maxWidth to fit the widest item.
        val widest = bitmaps.maxOf { it.bitmap.width }
        val effectiveMaxWidth = max(maxWidth, widest + padding * 2)

        var cursorX = 0
        var cursorY = 0
        var rowHeight = 0
        var canvasWidth = 0

        data class Placement(val name: String, val x: Int, val y: Int, val w: Int, val h: Int)
        val placements = ArrayList<Placement>(bitmaps.size)

        for (nb in bitmaps) {
            val w = nb.bitmap.width
            val h = nb.bitmap.height

            if (cursorX != 0 && cursorX + w + padding > effectiveMaxWidth) {
                // wrap to next row
                cursorX = 0
                cursorY += rowHeight + padding
                rowHeight = 0
            }

            placements.add(Placement(nb.name, cursorX, cursorY, w, h))
            canvasWidth = max(canvasWidth, cursorX + w)
            rowHeight = max(rowHeight, h)
            cursorX += w + padding
        }

        val canvasHeight = cursorY + rowHeight

        val packed = Bitmap.createBitmap(max(canvasWidth, 1), max(canvasHeight, 1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(packed)
        for ((i, p) in placements.withIndex()) {
            canvas.drawBitmap(bitmaps[i].bitmap, p.x.toFloat(), p.y.toFloat(), null)
        }

        val frames = placements.map { p ->
            AtlasFrame(name = p.name, x = p.x, y = p.y, width = p.w, height = p.h)
        }

        return packed to frames
    }

    /**
     * Packs bitmaps into a uniform grid of [cellSize] x [cellSize] cells — the layout
     * Psych Engine's icon-grid system expects. Each source bitmap is scaled to fill
     * its cell. [columns] of 0 auto-picks a roughly-square layout.
     */
    fun packGrid(bitmaps: List<NamedBitmap>, cellSize: Int, columns: Int = 0): Pair<Bitmap, List<AtlasFrame>> {
        require(bitmaps.isNotEmpty()) { "Nothing to pack" }
        require(cellSize > 0) { "cellSize must be > 0" }

        val cols = if (columns > 0) columns else max(1, sqrt(bitmaps.size.toDouble()).let { Math.ceil(it) }.toInt())
        val rows = Math.ceil(bitmaps.size.toDouble() / cols).toInt()

        val canvasWidth = cols * cellSize
        val canvasHeight = rows * cellSize

        val packed = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(packed)

        val frames = ArrayList<AtlasFrame>(bitmaps.size)
        for ((i, nb) in bitmaps.withIndex()) {
            val col = i % cols
            val row = i / cols
            val x = col * cellSize
            val y = row * cellSize

            val scaled = Bitmap.createScaledBitmap(nb.bitmap, cellSize, cellSize, true)
            canvas.drawBitmap(scaled, x.toFloat(), y.toFloat(), null)

            frames.add(AtlasFrame(name = nb.name, x = x, y = y, width = cellSize, height = cellSize))
        }

        return packed to frames
    }

    /**
     * Packs bitmaps as tightly as possible using a guillotine bin-packing
     * algorithm (best-area-fit placement, shorter-leftover-axis split) —
     * much less wasted space than the row-based shelf packer when frame
     * sizes vary a lot. Grows the working bin automatically until every
     * frame fits, then crops the result down to the actual used area.
     */
    fun packTight(bitmaps: List<NamedBitmap>, padding: Int = 1): Pair<Bitmap, List<AtlasFrame>> {
        require(bitmaps.isNotEmpty()) { "Nothing to pack" }

        val sorted = bitmaps.withIndex().sortedWith(
            compareByDescending<IndexedValue<NamedBitmap>> { it.value.bitmap.height }
                .thenByDescending { it.value.bitmap.width }
        )

        val totalArea = bitmaps.sumOf { (it.bitmap.width + padding).toLong() * (it.bitmap.height + padding) }
        val maxItemW = bitmaps.maxOf { it.bitmap.width } + padding
        val maxItemH = bitmaps.maxOf { it.bitmap.height } + padding
        var binSize = max(sqrt(totalArea.toDouble()).toInt() + 1, max(maxItemW, maxItemH))

        var placements: List<Pair<Int, GuillotineBinPacker.Rect>>? = null
        var attempts = 0
        while (placements == null && attempts < 8) {
            val packer = GuillotineBinPacker(binSize, binSize)
            val result = mutableListOf<Pair<Int, GuillotineBinPacker.Rect>>()
            var failed = false
            for (iv in sorted) {
                val w = iv.value.bitmap.width + padding
                val h = iv.value.bitmap.height + padding
                val rect = packer.insert(w, h)
                if (rect == null) { failed = true; break }
                result.add(iv.index to rect)
            }
            if (!failed) {
                placements = result
            } else {
                binSize = (binSize * 1.4).toInt() + 1
                attempts++
            }
        }

        val finalPlacements = placements ?: throw IllegalStateException("Could not pack frames — try fewer/smaller images")

        val usedWidth = finalPlacements.maxOf { it.second.x + it.second.w } - padding
        val usedHeight = finalPlacements.maxOf { it.second.y + it.second.h } - padding

        val packed = Bitmap.createBitmap(max(usedWidth, 1), max(usedHeight, 1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(packed)
        val frames = arrayOfNulls<AtlasFrame>(bitmaps.size)

        for ((originalIndex, rect) in finalPlacements) {
            val nb = bitmaps[originalIndex]
            canvas.drawBitmap(nb.bitmap, rect.x.toFloat(), rect.y.toFloat(), null)
            frames[originalIndex] = AtlasFrame(
                name = nb.name, x = rect.x, y = rect.y,
                width = nb.bitmap.width, height = nb.bitmap.height
            )
        }

        return packed to frames.filterNotNull()
    }

    /** Picks a roughly-square starting width based on total pixel area, capped at [cap]. */
    fun suggestMaxWidth(bitmaps: List<NamedBitmap>, cap: Int = 2048): Int {
        val totalArea = bitmaps.sumOf { it.bitmap.width.toLong() * it.bitmap.height.toLong() }
        val side = sqrt(totalArea.toDouble()).toInt()
        return min(max(side, bitmaps.maxOf { it.bitmap.width }), cap)
    }
}
