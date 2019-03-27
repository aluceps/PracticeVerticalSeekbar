package me.aluceps.practiceverticalseekbar

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import me.aluceps.practiceverticalseekbar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnChangeListener {


    private val binding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.valueBar.setupListener(this)
    }

    override fun progress(value: Int) {
        debugLog("progress: $value")
    }

    private fun debugLog(message: String) {
        Log.d("MainActivity", message)
    }
}
