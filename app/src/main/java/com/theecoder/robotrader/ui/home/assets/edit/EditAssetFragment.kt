package com.theecoder.robotrader.ui.home.assets.edit


import android.icu.text.CaseMap
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.theecoder.robotrader.R
import com.theecoder.robotrader.network.module.Symbol
import com.theecoder.robotrader.ui.HomeActivity
import com.theecoder.robotrader.ui.home.assets.AssetsViewModel


class EditAssetFragment : Fragment(R.layout.fragment_edit_asset) {
    private lateinit var assetsViewModel: AssetsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back(view)
        val symbol= arguments?.getSerializable("symbol") as Symbol
        assetsViewModel = (activity as HomeActivity).assetsViewModel

        val lot = view.findViewById(R.id.outlined_edit_text) as EditText
        val act = view.findViewById(R.id.outlined_edit_text_2) as EditText
        val pla = view.findViewById(R.id.outlined_edit_text_3) as EditText

        if(symbol.lotSize != null){
            lot.setText(symbol.lotSize.toString())
        }
        if(symbol.action != null){
            act.setText(symbol.action.toString())
        }
        if(symbol.platform != null){
            pla.setText(symbol.platform.toString())
        }

        view.findViewById<TextView>(R.id.top_text).text = symbol.name
        view.findViewById<Button>(R.id.submit).setOnClickListener {
            if(lot.text.isNotEmpty() && act.text.isNotEmpty() && pla.text.isNotEmpty())
            {
                val li = listOf<String>("BUY","SELL","BOTH")
                if (act.text.toString().uppercase() in li)
                {
                    val li2 = listOf<String>("MT5","MT4")
                    if(pla.text.toString().uppercase() in li2)
                    {
                        val saveThis = Symbol(symbol.id,symbol.name,symbol.phone_secret,lot.text.toString().toDouble(),act.text.toString().uppercase(),pla.text.toString().uppercase())

                        assetsViewModel.saveSymbol(saveThis)

                        Toast.makeText(context,"Symbol Saved!",Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }else{
                        Toast.makeText(context,"Platform Fields Can Only be MT5 or MT4.",Toast.LENGTH_SHORT).show()
                    }

                }else
                {
                    Toast.makeText(context,"Action Fields Can Only be BUY, SELL or BOTH.",Toast.LENGTH_SHORT).show()
                }
            }else
            {
                Toast.makeText(context,"Text Fields Cannot be empty",Toast.LENGTH_SHORT).show()
            }
        }
        view.findViewById<ImageButton>(R.id.delete_symbol).apply {
            if (symbol.lotSize != null){
                visibility=VISIBLE
            }else
            {
                visibility= GONE
            }

            setOnClickListener {
                assetsViewModel.deleteSymbol(symbol)

                Toast.makeText(context,"Symbol will no long be traded.",Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun back(view: View)
    {
        view.findViewById<ImageButton>(R.id.backbtn).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}