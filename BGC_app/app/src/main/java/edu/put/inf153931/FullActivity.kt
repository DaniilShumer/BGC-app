package edu.put.inf153931

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FullActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full)
        val thumbnail_bmp = intent.getParcelableExtra<android.graphics.Bitmap>("thumbnail_bmp")
        findViewById<android.widget.ImageView>(R.id.imageView).setImageBitmap(thumbnail_bmp)
    }
}