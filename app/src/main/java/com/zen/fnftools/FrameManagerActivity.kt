package com.zen.fnftools

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zen.fnftools.databinding.ActivityFrameManagerBinding
import com.zen.fnftools.util.OutputSaver
import com.zen.fnftools.util.SpritesheetPacker
import com.zen.fnftools.util.TempFrameStore
import com.zen.fnftools.util.TextureAtlasXml
import com.zen.fnftools.util.setBouncy

class FrameManagerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"           // "shelf" or "grid"
        const val EXTRA_OUTPUT_NAME = "outputName"
        const val EXTRA_CELL_SIZE = "cellSize"   // grid mode only
        const val EXTRA_COLUMNS = "columns"      // grid mode only
    }

    private lateinit var binding: ActivityFrameManagerBinding
    private lateinit var adapter: FrameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFrameManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frames = TempFrameStore.frames
        if (frames.isEmpty()) {
            Toast.makeText(this, "No frames to rename — pick some first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = FrameAdapter(frames)
        binding.rvFrames.layoutManager = LinearLayoutManager(this)
        binding.rvFrames.adapter = adapter
        binding.rvFrames.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(this, R.anim.zen_list_layout_anim)

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) adapter.selectAll() else adapter.clearSelection()
        }

        binding.btnApplyBulk.setBouncy()
        binding.btnApplyBulk.setOnClickListener {
            val newPrefix = binding.etBulkPrefix.text.toString()
            if (adapter.selectedCount() == 0) {
                Toast.makeText(this, "Check the frames you want to rename first", Toast.LENGTH_SHORT).show()
            } else {
                adapter.applyPrefixToSelected(newPrefix)
                Toast.makeText(this, "Renamed ${adapter.selectedCount()} frames", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPackSave.setBouncy()
        binding.btnPackSave.setOnClickListener { packAndSave() }
    }

    private fun packAndSave() {
        if (!com.zen.fnftools.util.SaveAccess.isGranted()) {
            com.zen.fnftools.util.SaveAccess.requestAccess(this)
            return
        }

        binding.loadingOverlay.visibility = android.view.View.VISIBLE

        val mode = intent.getStringExtra(EXTRA_MODE) ?: "shelf"
        val outputName = intent.getStringExtra(EXTRA_OUTPUT_NAME)?.ifBlank { "spritesheet" } ?: "spritesheet"
        val frames = TempFrameStore.frames

        Thread {
            try {
                val (packed, atlasFrames) = if (mode == "grid") {
                    val cellSize = intent.getIntExtra(EXTRA_CELL_SIZE, 150)
                    val columns = intent.getIntExtra(EXTRA_COLUMNS, 0)
                    SpritesheetPacker.packGrid(frames, cellSize = cellSize, columns = columns)
                } else {
                    SpritesheetPacker.packTight(frames)
                }

                val xml = TextureAtlasXml.build("$outputName.png", atlasFrames)

                val pngOk = OutputSaver.savePng(this, packed, "$outputName.png")
                val xmlOk = OutputSaver.saveXml(this, xml, "$outputName.xml")

                runOnUiThread {
                    binding.loadingOverlay.visibility = android.view.View.GONE
                    if (pngOk && xmlOk) {
                        showPreview(packed, outputName)
                    } else {
                        Toast.makeText(this, "Something went wrong saving the output", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.loadingOverlay.visibility = android.view.View.GONE
                    Toast.makeText(this, "Packing failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showPreview(packed: android.graphics.Bitmap, outputName: String) {
        binding.rvFrames.visibility = android.view.View.GONE
        binding.cbSelectAll.visibility = android.view.View.GONE
        binding.etBulkPrefix.visibility = android.view.View.GONE
        binding.btnApplyBulk.visibility = android.view.View.GONE
        binding.btnPackSave.visibility = android.view.View.GONE

        binding.ivPreview.setImageBitmap(packed)
        binding.ivPreview.visibility = android.view.View.VISIBLE

        binding.tvSavedInfo.text = "Saved to /ZFNFTSaves/$outputName.png + $outputName.xml"
        binding.tvSavedInfo.visibility = android.view.View.VISIBLE

        binding.btnDone.visibility = android.view.View.VISIBLE
        binding.btnDone.setBouncy()
        binding.btnDone.setOnClickListener {
            TempFrameStore.clear()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure a cancelled rename session doesn't leak bitmaps into the next screen.
        if (isFinishing) TempFrameStore.clear()
    }
}
