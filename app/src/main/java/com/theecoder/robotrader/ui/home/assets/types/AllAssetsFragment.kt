package com.theecoder.robotrader.ui.home.assets.types

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theecoder.robotrader.R
import com.theecoder.robotrader.adapters.SymbolsAdapter
import com.theecoder.robotrader.network.module.AuthBody
import com.theecoder.robotrader.network.module.Licence
import com.theecoder.robotrader.network.module.Symbol
import com.theecoder.robotrader.ui.HomeActivity
import com.theecoder.robotrader.ui.home.addrobot.AddRobotViewModel
import com.theecoder.robotrader.ui.home.assets.AssetsViewModel
import com.theecoder.robotrader.utils.Resource


class AllAssetsFragment : Fragment(R.layout.fragment_all_assets) {

    private lateinit var assetsViewModel: AssetsViewModel
    private lateinit var addRobotViewModel : AddRobotViewModel

    var sLicence: Licence? = null

    private lateinit var symbolsAdapter: SymbolsAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler(view)
        assetsViewModel = (activity as  HomeActivity).assetsViewModel
        addRobotViewModel = (activity as HomeActivity).addRobotViewModel

        addRobotViewModel.getSelectedLicences().observe(this,{
            if(it.isNotEmpty()){
                val li = it[0]
                val licence = Licence(li.ea_name,li.ea_notification, li.expires, li.key, li.owner, li.phone_secret_key, li.status, li.user)

                assetsViewModel.getSymbols(AuthBody(null, licence.phone_secret_key))
                sLicence = licence
            }
        })
        val builder = AlertDialog.Builder(context)
        var dialog: ProgressDialog? = null
        assetsViewModel.symbolsData.observe(viewLifecycleOwner,{

            when(it) {
                is Resource.Loading -> {
                    dialog?.dismiss()
                    dialog = ProgressDialog.show(context, "Loading", "Please wait...", true)
                    view.findViewById<LinearLayout>(R.id.error_note).visibility = GONE
                }
                is Resource.Error -> {

                    dialog?.dismiss()
                    view.findViewById<LinearLayout>(R.id.error_note).visibility = VISIBLE
                    view.findViewById<Button>(R.id.reload_button).setOnClickListener {
                        assetsViewModel.getSymbols(AuthBody(null, sLicence!!.phone_secret_key))
                    }
                }
                is Resource.Success -> {
                    dialog?.dismiss()
                    view.findViewById<LinearLayout>(R.id.error_note).visibility = GONE
                    if (it.data?.symbols!!.isNotEmpty()) {
                        symbolsAdapter.differ.submitList(it.data?.symbols)
                    }
                }
            }
        })


        symbolsAdapter.setOnItemClickListener {

            val symbol = Symbol(it.id,it.name,sLicence!!.phone_secret_key,null,null,null)
            val bundle= Bundle()
            bundle.putSerializable("symbol",symbol)

            findNavController().navigate(R.id.action_assetsFragment_to_editAssetFragment,bundle)
        }
    }
    private fun setupRecycler(view: View){

        val searchResultsRecyclerview= view.findViewById<RecyclerView>(R.id.symbols_recyclerView)
        symbolsAdapter = SymbolsAdapter()
        searchResultsRecyclerview.layoutManager =  LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        searchResultsRecyclerview.hasFixedSize()
        searchResultsRecyclerview.adapter = symbolsAdapter

    }
}