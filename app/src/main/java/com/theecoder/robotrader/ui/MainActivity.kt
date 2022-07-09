package com.theecoder.robotrader.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.theecoder.robotrader.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /*val handler = Handler()
        handler.postDelayed({*/

            val intent = Intent(this, HomeActivity::class.java)
            //val bundle: Bundle = ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            startActivity(intent)
            finish()

       // },100)
    }
}