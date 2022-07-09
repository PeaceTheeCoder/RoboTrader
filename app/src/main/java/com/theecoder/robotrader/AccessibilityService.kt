package com.theecoder.robotrader
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TimeUtils
import kotlin.system.exitProcess


private var LOCK = false
private var TRADED =false
private var PRESS_BUY_OR_SELL= false
private var PRESS_BACK_PRESSING = false
class AccessibilityService: AccessibilityService() {
    private lateinit var sharedPref : SharedPreferences
    private lateinit var exSignal : SharedPreferences
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        try {
            val accessibilityNodeInfo = rootInActiveWindow
            TRADED = sharedPref.getBoolean("isTraded",false)
            LOCK = sharedPref.getBoolean("isTrading",false) and !TRADED

            Handler(Looper.getMainLooper()).postDelayed(
                {
                    Log.d("asset","clicked")
                    deepChild(accessibilityNodeInfo,event)
                },
                50
            )

        }catch (e :PackageManager.NameNotFoundException ){
            e.printStackTrace();
        }


    }
    private fun deepChild(child: AccessibilityNodeInfo, event: AccessibilityEvent) {
        val assett = exSignal.getString("symbol","none")
        for (i in 0 until child.childCount) {
            val innerChild = child.getChild(i)
            if (innerChild !=null)
            {
                deepChild(innerChild, event)
                if(child.isScrollable && child.className =="android.widget.ListView" && LOCK)
                {
                    for (x in 0 until child.childCount)
                    {
                        val asset = child.findAccessibilityNodeInfosByText(assett)

                        if (asset.size > 0)
                        {
                            if(asset[0].parent.getChild(0).text.toString() != "0") {
                                val tradeActive =
                                    asset[0].parent.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader5:id/indicator")
                                if (tradeActive.size > 0) {
                                    saveData("error", false)
                                    saveData("isTraded", true)
                                    TRADED = true
                                    return
                                }else{
                                    asset[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                }
                            }

                        }else{
                            child.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        }

                    }

                }else
                {
                    if(child.className =="android.widget.LinearLayout" && LOCK){

                        val openOrder = child.findAccessibilityNodeInfosByText("New Order")
                        if (openOrder.size > 0)
                        {
                            openOrder[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                    if (child.className == "android.view.ViewGroup" && LOCK) {
                        child.getChild(0)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    val ACTION = exSignal.getString("action","none")
                    if (innerChild.className == "android.widget.EditText" && LOCK)
                    {
                        val lotSize = exSignal.getString("lot","0")
                        val TP = exSignal.getString("tp","0")
                        val SL = exSignal.getString("sl","0")


                        val arguments = Bundle()

                        if (innerChild.hintText == "volume")
                        {
                            arguments.putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                lotSize
                            )
                            innerChild.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments)
                        }
                        if (innerChild.hintText == "SL")
                        {
                            arguments.putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                SL
                            )
                            innerChild.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments)
                        }
                        if (innerChild.hintText == "TP")
                        {
                            arguments.putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                TP
                            )
                            innerChild.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments)
                        }
                        PRESS_BUY_OR_SELL = true
                    }

                    if(PRESS_BUY_OR_SELL)
                    {
                        val button = innerChild.findAccessibilityNodeInfosByText(ACTION)
                        if(button.size >0) {
                            button[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)

                            PRESS_BUY_OR_SELL = false
                            TRADED = true
                            saveData("isTraded", true)
                            PRESS_BACK_PRESSING = true
                            return
                        }
                    }
                    if (PRESS_BACK_PRESSING)
                    {
                        if(innerChild.className == "android.view.ViewGroup")
                        {
                            val backBtn = innerChild.findAccessibilityNodeInfosByText(assett)

                            if(backBtn.size > 0)
                            {
                                backBtn[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                PRESS_BACK_PRESSING = false
                                saveData("isTraded", true)
                                TRADED = true
                            }

                        }
                        saveData("isTraded", true)
                        TRADED = true

                    }

                }
            }

        }
    }



    override fun onInterrupt() {

        Log.e("error","Something went wrong.")
    }

    override fun onServiceConnected() {
        LOCK = true
        TRADED = true
        PRESS_BUY_OR_SELL= false
        PRESS_BACK_PRESSING = false
        sharedPref = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        exSignal = getSharedPreferences("ExSignal", MODE_PRIVATE)
        Toast.makeText(this, "RoboTrader Activated.  ",Toast.LENGTH_SHORT).show()
        val info = AccessibilityServiceInfo()
        info.apply {

            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = arrayOf("net.metaquotes.metatrader5")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
            notificationTimeout = 100
        }

        this.serviceInfo = info
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TRADED = false
        LOCK = true
        TRADED = true
        PRESS_BUY_OR_SELL= false
        PRESS_BACK_PRESSING = false
        sharedPref = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        exSignal = getSharedPreferences("ExSignal", MODE_PRIVATE)

        saveData("isTraded", false)
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "RoboTrader Stopped.  ",Toast.LENGTH_SHORT).show()
    }

    private fun saveData(name : String, data :Boolean){
        val sharedPref : SharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        val editor : SharedPreferences.Editor = sharedPref.edit()
        editor.apply {
            when(name) {
                "isTrading" -> {
                    putBoolean("isTrading",data)
                }
                "isTraded" -> {
                    putBoolean("isTraded",data)
                }
                "error" ->{
                    putBoolean("error",data)
                }
            }

        }.apply()
    }
}