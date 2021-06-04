package com.example.flatload

import android.graphics.BitmapFactory
import android.util.Base64
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

        val decodedBytes = i.getByteArrayExtra("image")
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        imageView3.setImageBitmap(bitmap)

//        val imagestr = i.getStringExtra("imageString")
//        val decodedBytes = Base64.decode(imagestr, 0)
//        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        //imageView3.setImageBitmap(bitmap)
    }
}