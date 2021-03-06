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
class AccessibilityServiceMT4: AccessibilityService() {
    private lateinit var sharedPref : SharedPreferences
    private lateinit var exSignal : SharedPreferences
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        try {
            val accessibilityNodeInfo = rootInActiveWindow
            TRADED = sharedPref.getBoolean("isTraded",false)
            LOCK = sharedPref.getBoolean("isTrading",false) and !TRADED

            Handler(Looper.getMainLooper()).postDelayed(
                {
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
                val list1 = child.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/selected_list")

                if( list1.size > 0 && LOCK)
                {
                    if(list1[0].childCount > 0)
                    {
                        list1[0].getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                }else
                {
                    val popup = child.findAccessibilityNodeInfosByViewId("android:id/parentPanel")
                    if( popup.size > 0 && LOCK)
                    {
                        for(h in 0 until popup[0].childCount)
                        {
                            val neworder = popup[0].getChild(h).findAccessibilityNodeInfosByText("New order")
                            if(neworder.size > 0)
                            {
                                if(neworder[0].isEnabled)
                                {
                                    neworder[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                }else
                                {
                                    val openchart = popup[0].getChild(h).findAccessibilityNodeInfosByText("Open chart")
                                    if(openchart.size > 0)
                                    {
                                        openchart[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    }
                                }

                            }
                        }
                    }
                    val headText = child.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/left_title")
                    if(headText.size>0 && LOCK)
                    {
                        if(headText[0].text == assett)
                        {
                            val ACTION = exSignal.getString("action","none")
                            val lotSize = exSignal.getString("lot","0")
                            val TP = exSignal.getString("tp","0")
                            val SL = exSignal.getString("sl","0")

                            val lotArguments = Bundle()
                            val slArguments = Bundle()
                            val tpArguments = Bundle()

                            val lotClass = innerChild.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/amount_edit")
                            val tpSLClass = innerChild.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/price_edit")

                            if(lotClass.size > 0)
                            {
                                lotArguments.putCharSequence(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                    lotSize
                                )
                                lotClass[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,lotArguments)
                            }
                            if(tpSLClass.size>1)
                            {
                                slArguments.putCharSequence(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                    SL
                                )
                                tpSLClass[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,slArguments)

                                tpArguments.putCharSequence(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                    TP
                                )
                                tpSLClass[1].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,tpArguments)

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
                                headText[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                PRESS_BACK_PRESSING = false
                                saveData("isTraded", true)
                                TRADED = true
                            }

                        }else
                        {
                            val symbolicon = child.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/order_symbol")
                            if(symbolicon.size>0)
                            {
                                symbolicon[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }

                        }

                    }

                    val symbollist = innerChild.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/period_list")
                    if(symbollist.size>0 && LOCK)
                    {
                        val assetsymbol = symbollist[0].findAccessibilityNodeInfosByText(assett)
                        if(assetsymbol.size > 0)
                        {
                            assetsymbol[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }else
                        {
                            symbollist[0].performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        }
                    }

                }

                val quotes = innerChild.findAccessibilityNodeInfosByViewId("net.metaquotes.metatrader4:id/bottom_bar_quotes")
                if (quotes.size > 0 && LOCK)
                {
                    quotes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
            packageNames = arrayOf("net.metaquotes.metatrader4")
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