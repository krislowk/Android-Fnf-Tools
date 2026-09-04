package com.zen.fnftools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.zen.fnftools.databinding.ActivitySpritesheetConverterBinding
import com.zen.fnftools.util.NamedBitmap
import com.zen.fnftools.util.TempFrameStore
import com.zen.fnftools.util.setBouncy
import com.zen.fnftools.util.staggerIn

class SpritesheetConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpritesheetConverterBinding
    private var pickedUris: List<Uri> = emptyList()

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            pickedUris = uris
            binding.tvStatus.text = "${uris.size} images selected"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpritesheetConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImages.setBouncy()
        binding.btnConvert.setBouncy()

        staggerIn(listOf(binding.tvHeader, binding.tvBody, binding.btnPickImages))

        binding.btnPickImages.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.btnConvert.setOnClickListener { proceedToRename() }
    }

    private fun proceedToRename() {
        if (pickedUris.isEmpty()) {
            Toast.makeText(this, "Pick some frame images first", Toast.LENGTH_SHORT).show()
            return
        }

        val animName = binding.etFrameName.text.toString().ifBlank { "anim" }
        val outputName = binding.etOutputName.text.toString().ifBlank { "spritesheet" }

        val bitmaps = ArrayList<NamedBitmap>()
        for ((i, uri) in pickedUris.withIndex()) {
            val stream = contentResolver.openInputStream(uri) ?: continue
            val bmp: Bitmap = BitmapFactory.decodeStream(stream) ?: continue
            stream.close()
            val frameName = "$animName${i.toString().padStart(4, '0')}"
            bitmaps.add(NamedBitmap(frameName, bmp))
        }

        if (bitmaps.isEmpty()) {
            Toast.makeText(this, "Couldn't read the selected images", Toast.LENGTH_SHORT).show()
            return
        }

        TempFrameStore.frames = bitmaps

        val intent = Intent(this, FrameManagerActivity::class.java).apply {
            putExtra(FrameManagerActivity.EXTRA_MODE, "shelf")
            putExtra(FrameManagerActivity.EXTRA_OUTPUT_NAME, outputName)
        }
        startActivity(intent)
    }
}
