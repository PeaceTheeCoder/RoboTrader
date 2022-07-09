package com.theecoder.robotrader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import com.theecoder.robotrader.utils.Constants.Companion.UPDATE_LINK


class FinishActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)

        val update = intent.getBooleanExtra("update",false)
        if(!update)
        {
            findViewById<RelativeLayout>(R.id.loading).visibility = VISIBLE
            findViewById<RelativeLayout>(R.id.update).visibility = GONE

            var maxto = 0
            val timeProgressBar = findViewById<ProgressBar>(R.id.time_progressBar)
            val timetext = findViewById<TextView>(R.id.time_line_2)
            var timeleft = 100
            val timer1 = object: CountDownTimer(100000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeleft -=1
                    timetext.text = timeleft.toString()
                    if(maxto <= 100){
                        timeProgressBar.apply {
                            progress = maxto
                        }
                    }
                    maxto += 1

                }

                override fun onFinish() {
                    timeProgressBar.apply {
                        progress = 100
                        finish()
                    }
                }
            }.start()
        }else{
            findViewById<RelativeLayout>(R.id.loading).visibility = GONE
            findViewById<RelativeLayout>(R.id.update).visibility = VISIBLE
            val updatebtn = findViewById<Button>(R.id.updatebutton).setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_LINK))
                startActivity(browserIntent)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}