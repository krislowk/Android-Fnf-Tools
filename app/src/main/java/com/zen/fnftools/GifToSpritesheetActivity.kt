package com.zen.fnftools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.zen.fnftools.databinding.ActivityGifToSpritesheetBinding
import com.zen.fnftools.util.GifFrameExtractor
import com.zen.fnftools.util.TempFrameStore
import com.zen.fnftools.util.setBouncy
import com.zen.fnftools.util.staggerIn

class GifToSpritesheetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGifToSpritesheetBinding
    private var gifUri: Uri? = null

    private val pickGif =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            gifUri = uri
            binding.tvStatus.text = if (uri != null) "GIF selected" else "No GIF selected"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGifToSpritesheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickGif.setBouncy()
        binding.btnConvert.setBouncy()

        staggerIn(listOf(binding.tvHeader, binding.tvBody, binding.btnPickGif))

        binding.btnPickGif.setOnClickListener {
            pickGif.launch("image/gif")
        }

        binding.btnConvert.setOnClickListener { proceedToRename() }
    }

    private fun proceedToRename() {
        val uri = gifUri
        if (uri == null) {
            Toast.makeText(this, "Pick a GIF first", Toast.LENGTH_SHORT).show()
            return
        }

        val frameCount = binding.etFrameCount.text.toString().toIntOrNull() ?: 24
        val animName = binding.etFrameName.text.toString().ifBlank { "anim" }
        val outputName = binding.etOutputName.text.toString().ifBlank { "spritesheet" }

        val gifBytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }

        if (gifBytes == null) {
            Toast.makeText(this, "Couldn't read the GIF", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmaps = try {
            GifFrameExtractor.extract(gifBytes, frameCount, animName)
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Failed to decode GIF", Toast.LENGTH_LONG).show()
            return
        }

        TempFrameStore.frames = bitmaps.toMutableList()

        val intent = Intent(this, FrameManagerActivity::class.java).apply {
            putExtra(FrameManagerActivity.EXTRA_MODE, "shelf")
            putExtra(FrameManagerActivity.EXTRA_OUTPUT_NAME, outputName)
        }
        startActivity(intent)
    }
}
