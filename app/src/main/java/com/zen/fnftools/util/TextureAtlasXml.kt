package com.zen.fnftools.util

/**
 * A single packed frame's position/size info, matching the fields Psych Engine
 * (and Adobe Animate texture atlas exports) expect in the XML.
 */
data class AtlasFrame(
    val name: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    // frameX/frameY/frameWidth/frameHeight describe trimming relative to the
    // original (untrimmed) frame. We don't trim, so these just mirror width/height.
    val frameX: Int = 0,
    val frameY: Int = 0,
    val frameWidth: Int = -1,
    val frameHeight: Int = -1,
    val rotated: Boolean = false
)

object TextureAtlasXml {

    /**
     * Builds an Adobe Animate style TextureAtlas XML string, the same format
     * Psych Engine / vanilla FNF reads for character and stage sprites.
     */
    fun build(imagePath: String, frames: List<AtlasFrame>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<TextureAtlas imagePath=\"").append(escape(imagePath)).append("\">\n")
        for (f in frames) {
            val fw = if (f.frameWidth == -1) f.width else f.frameWidth
            val fh = if (f.frameHeight == -1) f.height else f.frameHeight
            sb.append("    <SubTexture name=\"").append(escape(f.name)).append("\"")
            sb.append(" x=\"").append(f.x).append("\"")
            sb.append(" y=\"").append(f.y).append("\"")
            sb.append(" width=\"").append(f.width).append("\"")
            sb.append(" height=\"").append(f.height).append("\"")
            sb.append(" frameX=\"").append(f.frameX).append("\"")
            sb.append(" frameY=\"").append(f.frameY).append("\"")
            sb.append(" frameWidth=\"").append(fw).append("\"")
            sb.append(" frameHeight=\"").append(fh).append("\"")
            if (f.rotated) sb.append(" rotated=\"true\"")
            sb.append("/>\n")
        }
        sb.append("</TextureAtlas>\n")
        return sb.toString()
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
}
