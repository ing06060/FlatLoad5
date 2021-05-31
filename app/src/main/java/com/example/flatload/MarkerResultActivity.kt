package com.example.flatload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_marker_result.*

class MarkerResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_result)
        init()
    }
    private fun init() {
        val i = intent
        val markerLoc = i.getStringExtra("markerLocation")
        textView6.text = markerLoc
    }
}