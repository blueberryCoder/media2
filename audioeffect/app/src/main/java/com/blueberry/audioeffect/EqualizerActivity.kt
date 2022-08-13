package com.blueberry.audioeffect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import java.io.File
import kotlin.concurrent.thread

class EqualizerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EqualizerActivity"
    }

    private lateinit var etInput: EditText
    private lateinit var etOutput: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            thread {
                startEqualizer()
            }
        }

        etInput = findViewById(R.id.et_input_path)
        etOutput = findViewById(R.id.et_out_path)

        etInput.setText(ResourceUtil.getInputAudio(this))
        etOutput.setText(
            File(
                ResourceUtil.getOutputParentDir(this),
                "equalizer-audio.wav"
            ).absolutePath
        )
    }

    fun startEqualizer() {
        Log.i(TAG, "startEqualizer: ")
        val inputPath = etInput.text.toString()
        val outputPath = etOutput.text.toString()
        val equalizerLib = EqualizerJniLib()
        equalizerLib.initialize()
        equalizerLib.setInput(inputPath)
        equalizerLib.setOutput(outputPath)
        equalizerLib.start()
        Log.i(TAG, "startEqualizer: end")
    }
}