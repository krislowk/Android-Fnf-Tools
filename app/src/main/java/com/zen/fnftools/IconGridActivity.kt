package com.zen.fnftools

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.zen.fnftools.databinding.ActivityIconGridBinding
import com.zen.fnftools.util.NamedBitmap
import com.zen.fnftools.util.TempFrameStore
import com.zen.fnftools.util.setBouncy
import com.zen.fnftools.util.staggerIn

class IconGridActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIconGridBinding
    private var pickedUris: List<Uri> = emptyList()

    private val pickIcons =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            pickedUris = uris
            binding.tvStatus.text = "${uris.size} icons selected"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIconGridBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickIcons.setBouncy()
        binding.btnConvert.setBouncy()

        staggerIn(listOf(binding.tvHeader, binding.tvBody, binding.btnPickIcons))

        binding.btnPickIcons.setOnClickListener {
            pickIcons.launch("image/*")
        }

        binding.btnConvert.setOnClickListener { proceedToRename() }
    }

    private fun displayNameOf(uri: Uri): String {
        var name = "icon"
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && it.moveToFirst()) {
                name = it.getString(idx)
            }
        }
        return name.substringBeforeLast(".")
    }

    private fun proceedToRename() {
        if (pickedUris.isEmpty()) {
            Toast.makeText(this, "Pick some icon images first", Toast.LENGTH_SHORT).show()
            return
        }

        val cellSize = binding.etCellSize.text.toString().toIntOrNull() ?: 150
        val columns = binding.etColumns.text.toString().toIntOrNull() ?: 0
        val outputName = binding.etOutputName.text.toString().ifBlank { "icon-grid" }

        val bitmaps = ArrayList<NamedBitmap>()
        for (uri in pickedUris) {
            val stream = contentResolver.openInputStream(uri) ?: continue
            val bmp: Bitmap = BitmapFactory.decodeStream(stream) ?: continue
            stream.close()
            bitmaps.add(NamedBitmap(displayNameOf(uri), bmp))
        }

        if (bitmaps.isEmpty()) {
            Toast.makeText(this, "Couldn't read the selected images", Toast.LENGTH_SHORT).show()
            return
        }

        TempFrameStore.frames = bitmaps

        val intent = Intent(this, FrameManagerActivity::class.java).apply {
            putExtra(FrameManagerActivity.EXTRA_MODE, "grid")
            putExtra(FrameManagerActivity.EXTRA_OUTPUT_NAME, outputName)
            putExtra(FrameManagerActivity.EXTRA_CELL_SIZE, cellSize)
            putExtra(FrameManagerActivity.EXTRA_COLUMNS, columns)
        }
        startActivity(intent)
    }
}
