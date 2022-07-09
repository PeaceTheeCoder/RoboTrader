package com.theecoder.robotrader.ui.home

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils.concat
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.theecoder.robotrader.BuildConfig
import com.theecoder.robotrader.ControlService
import com.theecoder.robotrader.FinishActivity
import com.theecoder.robotrader.R
import com.theecoder.robotrader.adapters.RobotsAdapter
import com.theecoder.robotrader.network.module.AuthBody
import com.theecoder.robotrader.network.module.Licence
import com.theecoder.robotrader.ui.HomeActivity
import com.theecoder.robotrader.ui.home.addrobot.AddRobotViewModel
import com.theecoder.robotrader.ui.home.assets.AssetsViewModel
import com.theecoder.robotrader.utils.Constants.Companion.CURRENT_VERSION
import com.theecoder.robotrader.utils.Constants.Companion.LOGO_BASE_URL
import com.theecoder.robotrader.utils.Resource
import com.theecoder.robotrader.utils.convert
import kotlinx.coroutines.coroutineScope

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var robotsAdapter: RobotsAdapter
    private lateinit var addRobotViewModel : AddRobotViewModel
    private lateinit var assetsViewModel: AssetsViewModel
    private lateinit var homeViewModel: HomeViewModel

    private var authenticated = false
    var sLicence: Licence? = null

    //for service
    private var trading = false
    private var mBound : MutableLiveData<Boolean> = MutableLiveData()
    private lateinit var mService: ControlService
    private val mConnection = object  : ServiceConnection{
        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            val service = binder as ControlService.MyBinder
            mService = service.getService()
            mBound.postValue(true)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound.postValue(false)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler(view)
        addRobotViewModel = (activity as HomeActivity).addRobotViewModel
        assetsViewModel = (activity as HomeActivity).assetsViewModel
        homeViewModel = (activity as HomeActivity).homeViewModel

        val navController = findNavController()

        mBound.observe(this,{
            if(it)
                homeViewModel.getTradingStatus(mService)
        })

        homeViewModel.tradingInProgress.observe(viewLifecycleOwner,{
            trading = it
            when(it)
            {
                true ->{
                    view.findViewById<LinearLayout>(R.id.stopButton).visibility = VISIBLE
                    view.findViewById<LinearLayout>(R.id.startButton).visibility = GONE
                }
                false -> {
                    view.findViewById<LinearLayout>(R.id.stopButton).visibility = GONE
                    if (sLicence?.status != "Expired")
                        view.findViewById<LinearLayout>(R.id.startButton).visibility = VISIBLE
                    homeViewModel.getApp()
                }
            }
        })

        homeViewModel.app.observe(viewLifecycleOwner,{
            when(it)
            {
                is Resource.Success ->{

                    if(it.data !=null)
                    if(CURRENT_VERSION != it.data.version) {
                        val intent: Intent = Intent((activity as HomeActivity), FinishActivity::class.java)
                        intent.putExtra("update",true)
                        startActivity(intent)
                    }
                }
            }
        })
        view.findViewById<CardView>(R.id.add_bot_btn).setOnClickListener {
            navController.navigate(R.id.action_navigation_home_to_addRobotFragment)
        }
        view.findViewById<LinearLayout>(R.id.startButton).setOnClickListener {
            val intent: Intent = Intent((activity as HomeActivity),ControlService::class.java)
            (activity as HomeActivity).startForegroundService(intent).also {
                (activity as HomeActivity).bindService(intent,mConnection,Context.BIND_AUTO_CREATE)
            }
            homeViewModel.getTradingStatus(mService)
        }
        view.findViewById<LinearLayout>(R.id.stopButton).setOnClickListener {
            val intent: Intent = Intent((activity as HomeActivity), ControlService::class.java)
            (activity as HomeActivity).unbindService(mConnection)
            (activity as HomeActivity).stopService(intent)
            homeViewModel.tradingInProgress.postValue(false)
        }
        view.findViewById<LinearLayout>(R.id.gotoasset).setOnClickListener {
            navController.navigate(R.id.action_navigation_home_to_assetsFragment)
        }
        view.findViewById<LinearLayout>(R.id.delete_ea).setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Warning !!")
            builder.setMessage(concat("Are you sure you want to delete ",sLicence?.ea_name," ?. Note that you will no longer be able to use the licence key ~ ",sLicence?.key))
            builder.setPositiveButton("DELETE"){
                    dialog,which->
                if(trading)
                {
                    val intent: Intent = Intent((activity as HomeActivity), ControlService::class.java)
                    (activity as HomeActivity).unbindService(mConnection)
                    (activity as HomeActivity).stopService(intent)
                    homeViewModel.tradingInProgress.postValue(false)
                }
                assetsViewModel.deleteAllSymbol(sLicence!!.phone_secret_key)
                addRobotViewModel.deleteSicence(sLicence!!)
                addRobotViewModel.deleteLicence(sLicence!!)
                dialog.dismiss()
            }
            builder.setNegativeButton("CANCEL"){
                    dialog,which->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }

        view.findViewById<ImageView>(R.id.bot_info).setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Robot Information")
            builder.setMessage(concat(
                "Status : ",sLicence?.status,
                "\nRobot Name : ",sLicence?.ea_name,
                "\nLicence Key : ",sLicence?.key,
                "\nActive Until : ",convert(sLicence?.expires),
                "\nOwner's Name : ",sLicence?.owner?.name,
                "\nOwner's Contacts : ",sLicence?.owner?.phone
            ))
            builder.setPositiveButton("CLOSE"){
                    dialog,which->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        addRobotViewModel.getSelectedLicences().observe(this,{
            if(it.isNotEmpty()){
                val li = it[0]
                val licence = Licence(li.ea_name,li.ea_notification, li.expires, li.key, li.owner, li.phone_secret_key, li.status, li.user)
                if(!authenticated){
                    authenticated = true
                    addRobotViewModel.authenticate(
                        AuthBody(
                            licence!!.key,
                            licence!!.phone_secret_key)
                    )
                }
                sLicence = licence
                showTopView(view, licence)
            }else
            {
                showTopView(view, null)
            }
        })
        addRobotViewModel.getLicences().observe(viewLifecycleOwner,{
            if (it.isNotEmpty())
            {
                view.findViewById<TextView>(R.id.label).visibility = VISIBLE
                robotsAdapter.differ.submitList(it.asReversed())
            }else{
                view.findViewById<TextView>(R.id.label).visibility = GONE
                robotsAdapter.differ.submitList(it)
            }
        })

        robotsAdapter.setOnItemClickListener {

            if(it != sLicence)
            {
                if(trading)
                {
                    val intent: Intent = Intent((activity as HomeActivity), ControlService::class.java)
                    (activity as HomeActivity).unbindService(mConnection)
                    (activity as HomeActivity).stopService(intent)
                    homeViewModel.tradingInProgress.postValue(false)
                }

                addRobotViewModel.authenticate(AuthBody(it.key,it.phone_secret_key))

            }

        }

    }

    private fun showTopView(view: View, licence: Licence?)
    {
        if (licence != null) {
            view.findViewById<RelativeLayout>(R.id.selected_bot_header).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.bot_name).text = licence.ea_name
            view.findViewById<TextView>(R.id.owner_name).text = concat("~ ",licence.owner.name)

            when (licence.status) {
                "Active" -> {
                    val startButton = view.findViewById(R.id.startButton) as LinearLayout
                    if(!trading)
                        startButton.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.note).visibility=GONE
                    view.findViewById<TextView>(R.id.owner_name).visibility = VISIBLE
                }

                "Expired" -> {
                    val startButton = view.findViewById(R.id.startButton) as LinearLayout
                    startButton.visibility = View.GONE
                    view.findViewById<TextView>(R.id.owner_name).visibility = GONE
                    view.findViewById<TextView>(R.id.note).apply{
                        visibility = View.VISIBLE
                        text = concat("! Your Licence key ~ ",
                            licence.key," Have Expired. Please Contact ",licence.owner.name," at ",
                            licence.owner.phone, " for  new Key or re-activation.")
                    }

                }
            }
            when(licence.owner.logo){
                "none" ->{
                    val imageview = view.findViewById(R.id.logo_img) as ImageView
                    imageview.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_arrow_circle_right_24))
                }
                else ->{
                    Glide.with(view)
                        .load(LOGO_BASE_URL +licence.owner.logo)
                        .centerCrop()
                        .into(view.findViewById(R.id.logo_img))
                }
            }

        }else{
            view.findViewById<RelativeLayout>(R.id.selected_bot_header).visibility = GONE
        }
    }
    private fun setupRecycler(view: View){

        val searchResultsRecyclerview= view.findViewById<RecyclerView>(R.id.bots_list_recycler_view)
        robotsAdapter = RobotsAdapter()
        searchResultsRecyclerview.layoutManager =  LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        searchResultsRecyclerview.hasFixedSize()
        searchResultsRecyclerview.adapter = robotsAdapter

    }

    override fun onStart() {
        super.onStart()
        Intent((activity as HomeActivity),ControlService::class.java).also {
            (activity as HomeActivity).bindService(it,mConnection,Context.BIND_AUTO_CREATE)
        }
    }


}