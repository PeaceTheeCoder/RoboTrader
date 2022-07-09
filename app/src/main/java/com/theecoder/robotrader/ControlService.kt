package com.theecoder.robotrader

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Job
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.SharedPreferences
import android.net.Uri
import android.os.Binder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.theecoder.robotrader.network.db.LicenceDB
import com.theecoder.robotrader.network.module.*
import com.theecoder.robotrader.repository.RTRepository
import com.theecoder.robotrader.ui.HomeActivity
import com.theecoder.robotrader.ui.MainActivity
import com.theecoder.robotrader.ui.home.HomeViewModel
import com.theecoder.robotrader.utils.Resource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import retrofit2.Response

import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.text.TextUtils.concat
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import android.widget.Toast
import android.app.Service.LAYOUT_INFLATER_SERVICE as LAYOUT_INFLATER_SERVICE1
import android.app.PendingIntent
import android.content.Intent.*


class ControlService: Service(), View.OnTouchListener, View.OnClickListener{
    lateinit var job: Job
    private val binder = MyBinder()
    private val rtRepository = RTRepository(LicenceDB(this))
    private val licence = getSelectedLicences()
    private var busy = false
    private val signalData: MutableLiveData<Resource<Signals>> = MutableLiveData()
    private lateinit var symbols: LiveData<List<Symbol>>
    private var useSymbols :List<Symbol> = listOf()
    private lateinit var pendingIntent: PendingIntent
    private lateinit var pendingIntentMT4: PendingIntent
    private lateinit var myPendingIntent: PendingIntent
    private lateinit var myPendingIntent2: PendingIntent
    private lateinit var lic: Sicence
    private lateinit var sharedPref : SharedPreferences
    private  var LOCK = false

    private var trading = false
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("runnable","service is binded")
        return binder
    }


    inner class MyBinder: Binder(){
        fun getService() = this@ControlService
    }

    fun getStatus() : Flow<Boolean> {
        return flow {
            delay(500)
            emit(trading)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel(applicationContext)
        clearSaved()
        resetVariable()
        createOverlay()
        sharedPref = applicationContext.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        saveData("isTrading", false)
        saveData("isTraded", false)

        val intent = applicationContext.packageManager.getLaunchIntentForPackage("net.metaquotes.metatrader5")

        intent!!.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val intentMT4 = applicationContext.packageManager.getLaunchIntentForPackage("net.metaquotes.metatrader4")

        intentMT4!!.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        pendingIntentMT4 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intentMT4,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(this, 0, intentMT4,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }

        trading = true
        Log.d("runnable","service is started")
        licence.observeForever(licenceObserver)
        signalData.observeForever(signalsObserver)


        return START_STICKY
    }

    private fun resetVariable()
    {
        saveData("isTraded", false)
        saveData("isTrading", false)
        ExSignal("none", "none", "none", "0", "0", "0")
        busy =false
        checkCount = 0
        counter2 = 100
    }
    private fun getSavedSymbols(phone_secret_key:String) = rtRepository.getSavedSymbols(phone_secret_key)
    private fun getSelectedLicences() = rtRepository.getSicences()
    private var symbolObserver = Observer<List<Symbol>> { useSymbols = it }
    private var checkCount = 0
    private var counter2 = 100
    private val licenceObserver = Observer<List<Sicence>> { data ->
        if (data.isNotEmpty())
        {
            lic = data[0]
            symbols = getSavedSymbols(lic.phone_secret_key)
            symbols.observeForever(symbolObserver)
            job = MainScope().launch {
                while (true)
                {

                    delay(1000)
                    if(!busy && !sharedPref.getBoolean("isTrading",false) && !sharedPref.getBoolean("inMt5",false))
                    {
                        checkCount =0
                        delay(1000)
                        if (counter2 <= 50) {
                            counter2 += 1
                            if (counter2 > 20)
                            {
                                myView.findViewById<TextView>(R.id.note_text).apply{
                                    visibility = GONE
                                }
                                saveData("error", true)
                            }
                            Log.d("runnable","in the delay")
                        }else {
                            myView.findViewById<TextView>(R.id.note_text).apply{
                                visibility = GONE
                            }
                            getSignals(AuthBody(lic.key,lic.phone_secret_key))
                            counter2 = 100

                        }

                    }
                    if(sharedPref.getBoolean("isTrading",false)){

                        if(sharedPref.getBoolean("isTraded",false)){
                            openRoboTrader()
                            counter2 = 0
                            saveData("isTrading", false)
                            ExSignal("none", "none", "none", "0", "0", "0")
                            myView.findViewById<TextView>(R.id.note_text).apply{

                                text = if(sharedPref.getBoolean("error",true)){
                                    concat("Automated Trading is Completed.")
                                }else{
                                    concat("Did Not Execute the trade because of one or more of the following\n\n" +
                                            "1. The Symbol have an open trade.\n" +
                                            "2. Your Wrote a wrong LotSize For the symbol.\n" +
                                            "3. you did not add the symbol in quotes of your MT5/MT4.\n" +
                                            "4. You do not have enough balance in your account.\n" +
                                            "5. You did not switch on the accessibility settings.\n" +
                                            "PLEASE STOP THE ROBOT AND START IT AGAIN.")
                                }
                                visibility = VISIBLE
                            }
                            busy = false
                            continue
                        }

                        delay(1000)
                        if (checkCount <= 35) {
                            checkCount += 1
                        }else{
                            if(!sharedPref.getBoolean("isTraded",false)){
                                openRoboTrader()
                            }
                            saveData("isTraded", true)
                            saveData("isTrading", false)
                            ExSignal("none", "none", "none", "0", "0", "0")
                            myView.findViewById<TextView>(R.id.note_text).apply{
                                text = concat("Did Not Execute the trade because of one or more of the following\n\n" +
                                            "1. The Symbol have an open trade.\n" +
                                            "2. Your Wrote a wrong LotSize For the symbol.\n" +
                                            "3. you did not add the symbol in quotes of your MT5/MT4.\n" +
                                            "4. You do not have enough balance in your account.\n" +
                                            "5. You did not switch on the accessibility settings.\n" +
                                            "PLEASE STOP THE ROBOT AND START IT AGAIN.")

                                visibility = VISIBLE
                            }
                            busy = false
                        }
                        Log.d("runnable",checkCount.toString())


                    }

                }
            }
        }
    }

    private val signalsObserver = Observer<Resource<Signals>> { data ->
        busy = when(data) {
            is Resource.Loading ->{
                true
            }
            is Resource.Error ->{
                false
            }
            is Resource.Success ->{
                if(data.data != null && data.data.signal !=null){
                    val mySig = data.data.signal
                    val mySymbol = testIfSymbolIsAllowed(mySig.asset,useSymbols)
                    if (mySymbol !=null)
                    {
                        if ((mySig.action == mySymbol.action) || (mySymbol.action =="BOTH") )
                        {

                            ExSignal(
                                mySymbol.name,
                                mySig.action,
                                mySig.price,
                                mySig.tp,
                                mySig.sl,
                                mySymbol.lotSize!!.toString()
                            )
                            Log.d("runnable",mySymbol.name.toString())

                            if(!sharedPref.getBoolean("isTrading",false) ) {

                                Log.d("runnable", "trading")
                                saveData("isTrading", true)
                                saveData("isTraded", false)
                                if(mySymbol.platform == "MT5") {
                                    val intent: Intent =
                                        Intent(this, AccessibilityService::class.java)
                                    startService(intent)
                                    openMT5()
                                    myView.findViewById<TextView>(R.id.note_text).apply{
                                        text = concat("Automated Trading is in Progress, Please Do not Use your phone.")
                                        visibility = VISIBLE
                                    }
                                }
                                else if(mySymbol.platform == "MT4") {
                                    val intent: Intent =
                                        Intent(this, AccessibilityServiceMT4::class.java)
                                    startService(intent)
                                    openMT4()
                                    myView.findViewById<TextView>(R.id.note_text).apply{
                                        text = concat("Automated Trading is in Progress, Please Do not Use your phone.")
                                        visibility = VISIBLE
                                    }
                                }else
                                {
                                    myView.findViewById<TextView>(R.id.note_text).apply{
                                        text = concat("Automated Trading Failed, please add a platform to this symbol.")
                                        visibility = VISIBLE
                                    }
                                }


                                false
                            }else
                            {
                                false
                            }

                        }else
                        {
                            false
                        }

                    }else
                    {
                      false
                    }

                }
                else{
                    false
                }
            }
        }
    }
    private fun openMT5()
    {
        pendingIntent.send()
    }
    private fun openMT4()
    {
        pendingIntentMT4.send()
    }
    private fun openRoboTrader()
    {
        myPendingIntent2.send()
    }


    private fun testIfSymbolIsAllowed(name: String?, symbols : List<Symbol>): Symbol?{

        for(symbol in symbols)
        {
            if(name == symbol.name) return symbol
        }

        return null
    }

    private fun getSignals(authBody: AuthBody) = MainScope().launch {

        signalData.postValue(Resource.Loading())
        try {
            val response = rtRepository.getSignals(authBody)
            signalData.postValue(handleSignalResponse(response))
        } catch (t: Throwable) {
            signalData.postValue(Resource.Error("Oops! Something went wrong"))
        }

    }
    private fun handleSignalResponse(response: Response<Signals>): Resource<Signals> {
        if (response.isSuccessful) {
            response.body()?.let {
                if (it.message == "accept") {
                    return Resource.Success(it)
                }

                return Resource.Error("Unknown Error Occurred!!")
            }
        }
        return Resource.Error(response.message())
    }

    override fun onDestroy() {
        super.onDestroy()
        trading =false
        busy = false
        useSymbols  = listOf()
        licence.removeObserver(licenceObserver)
        signalData.removeObserver(signalsObserver)
        symbols.removeObserver(symbolObserver)
        job.cancel()
        windowManager.removeView(myView)
        clearSaved()
        resetVariable()
        Log.d("runnable","service stopped")
        stopSelf()

    }
    private fun clearSaved()
    {
        val pref1: SharedPreferences =
            getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        pref1.edit().clear().apply()

        val pref2: SharedPreferences =
            getSharedPreferences("ExSignal", MODE_PRIVATE)
        pref2.edit().clear().apply()
    }

    private fun createNotificationChannel(context: Context){
        val channelId = "RoboTrader"
        val channelName = "Robo Trader"

        val channel = NotificationChannel(
            channelId,
            channelName,
            IMPORTANCE_LOW

        ).apply {
            lightColor = Color.BLUE
            importance = IMPORTANCE_LOW
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        myPendingIntent =if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val intent2 = Intent(this, FinishActivity::class.java)
        intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //myPendingIntent2 =PendingIntent.getActivity(this,0,intent2,0)
        myPendingIntent2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(this, 0, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }


        val channelBuilder = NotificationCompat.Builder(this,channelId)
        val notification = channelBuilder
            .setOngoing(true)
            .setSmallIcon(com.theecoder.robotrader.R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentTitle("Automated Trading is Active")
            .setContentText("The Robot Will Execute Trades For you.")
            .build()

        startForeground(1, notification)
    }

    private lateinit var windowManager: WindowManager
    private lateinit var btnParams:WindowManager.LayoutParams

    private lateinit var myView : View
    private fun createOverlay() {

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater : LayoutInflater  =  getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        myView = inflater.inflate(R.layout.info_layout, null)
        myView.setOnTouchListener(this)
        myView.setOnClickListener(this)

        val layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        btnParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        btnParams.gravity = Gravity.TOP or Gravity.START
        btnParams.x = 50
        btnParams.y =50

        windowManager.addView(myView,btnParams)


    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0.0f
    private var initialTouchY = 0.0f
    private var moving = true

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        view!!.performClick()
        when(event?.action)
        {
            MotionEvent.ACTION_DOWN ->{
                initialX = btnParams.x
                initialY = btnParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                moving = true
            }
            MotionEvent.ACTION_UP ->
            {
                moving =false
            }

            MotionEvent.ACTION_MOVE ->{
                btnParams.x = initialX+ (event.rawX - initialTouchX).toInt()
                btnParams.y = initialY+ (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(myView, btnParams)
            }
        }
        return true
    }

    override fun onClick(view: View?) {
        if(!moving) {
            Toast.makeText(this, lic.ea_name+" Is Trading For You.", Toast.LENGTH_SHORT).show()
        }
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
                "inMt5" -> {
                    putBoolean("inMt5",data)
                }
            }

        }.apply()
    }

    private fun ExSignal(
        symbol: String,
        action: String,
        price : String,
        tp : String,
        sl : String,
        lot : String
    ){
        val sharedPref : SharedPreferences = getSharedPreferences("ExSignal", MODE_PRIVATE)
        val editor : SharedPreferences.Editor = sharedPref.edit()
        editor.apply {
            putString("symbol",symbol)
            putString("action",action)
            putString("price",price)
            putString("tp",tp)
            putString("sl",sl)
            putString("lot",lot)

            }.apply()

    }

}

