package com.theecoder.robotrader.ui

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.theecoder.robotrader.R
import com.theecoder.robotrader.databinding.ActivityHomeBinding
import com.theecoder.robotrader.network.db.LicenceDB
import com.theecoder.robotrader.repository.RTRepository
import com.theecoder.robotrader.ui.home.HomeViewModel
import com.theecoder.robotrader.ui.home.addrobot.AddRobotViewModel
import com.theecoder.robotrader.ui.home.addrobot.ViewModelProviderFactory
import com.theecoder.robotrader.ui.home.assets.AssetsViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    lateinit var addRobotViewModel : AddRobotViewModel
    lateinit var assetsViewModel : AssetsViewModel
    lateinit var homeViewModel : HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rtRepository = RTRepository(LicenceDB(this))
        val addRobotViewModelProviderFactory = ViewModelProviderFactory(rtRepository)
        addRobotViewModel = ViewModelProvider(this, addRobotViewModelProviderFactory)[AddRobotViewModel::class.java]

        val assetsViewModelProviderFactory = com.theecoder.robotrader.ui.home.assets.ViewModelProviderFactory(rtRepository)
        assetsViewModel = ViewModelProvider(this, assetsViewModelProviderFactory)[AssetsViewModel::class.java]

        val homeViewModelProviderFactory = com.theecoder.robotrader.ui.home.ViewModelProviderFactory(rtRepository)
        homeViewModel = ViewModelProvider(this, homeViewModelProviderFactory)[HomeViewModel::class.java]

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,R.id.navigation_help
            )
        )
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }
}