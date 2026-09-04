package com.zen.fnftools

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zen.fnftools.databinding.ActivityMainBinding
import com.zen.fnftools.util.setBouncy
import com.zen.fnftools.util.staggerIn

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttons = listOf(binding.btnSpritesheetConverter, binding.btnIconGrid, binding.btnGifToSpritesheet)
        buttons.forEach { it.setBouncy() }

        staggerIn(
            listOf(binding.tvLogo, binding.tvSubtitle) + buttons,
            startDelay = 80L,
            step = 60L
        )

        binding.btnSpritesheetConverter.setOnClickListener {
            try {
                startActivity(Intent(this, SpritesheetConverterActivity::class.java))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Crash: ${e.javaClass.simpleName}: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        binding.btnIconGrid.setOnClickListener {
            try {
                startActivity(Intent(this, IconGridActivity::class.java))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Crash: ${e.javaClass.simpleName}: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        binding.btnGifToSpritesheet.setOnClickListener {
            try {
                startActivity(Intent(this, GifToSpritesheetActivity::class.java))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Crash: ${e.javaClass.simpleName}: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
