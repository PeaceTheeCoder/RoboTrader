package com.theecoder.robotrader.ui.help


import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theecoder.robotrader.R
import com.theecoder.robotrader.adapters.FaqAdapter
import com.theecoder.robotrader.network.module.Faqs
import android.content.Intent
import android.net.Uri


class HelpFragment : Fragment(R.layout.fragment_help) {

    private val myFaqs = MutableLiveData<List<Faqs>>()
    private lateinit var faqAdapter: FaqAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler(view)
        myFaqs.postValue(faqs(null))

        myFaqs.observe(viewLifecycleOwner,{
            faqAdapter.differ.submitList(it.toMutableList())
        })

        faqAdapter.setOnItemClickListener {

            if ( faqAdapter.selectedPos !=null) {
                if (it.selected)myFaqs.postValue(faqs(null))
                else myFaqs.postValue(faqs(faqAdapter.selectedPos))
            }

        }

        view.findViewById<CardView>(R.id.whatsapp_btn).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/27632024783"))
            startActivity(browserIntent)
        }
        view.findViewById<CardView>(R.id.web_btn).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.peacetheecoder.com/robotrader/"))
            startActivity(browserIntent)
        }

    }

    private fun setupRecycler(view: View){

        val faqRecyclerview= view.findViewById<RecyclerView>(R.id.faq_recyclerView)
        faqAdapter = FaqAdapter()
        faqRecyclerview.layoutManager =  LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        faqRecyclerview.hasFixedSize()
        faqRecyclerview.adapter = faqAdapter

    }
    private fun faqs(select:Int?):List<Faqs>{
        return listOf(
            Faqs(
                "Why does the app crash every time i press start?",
                "There are 2 possibilities\n\n" +
                        "1. You need to make sure that you have enabled draw on top or appear on top permissions." +
                        " You Find this by going to settings > Apps > RoboTrader.\n\n" +
                        "2. You need to Make sure that you have downloaded both MetaTrader5 and MetaTrader4 in you phone. " +
                        "You can get this from google play store or any app store you use.",
                select == 0
            ),
            Faqs(
                "Why is the app saying Automated Trading is completed but there is no trade active?",
                "This is caused by several issues.\n\n" +
                        "1. Make sure that you have enough balance in your trading account.\n\n" +
                        "2. Make sure that you have activated the accessibility service.\n\n" +
                        "3. Make sure you have set lotsizes which are valid when you added symbols in trader.\n\n" +
                        "4. Make sure that you have added the symbols in quotes, inside you Mt5/Mt4.\n\n" +
                        "5. If all that is fine, please stop the robot and start it again. If the problem still continues, contact support.",
                select == 1
            ),
            Faqs(
                "Why is the app Not taking or trying to take trades for so long?",
                "This is because of the following\n\n" +
                        "1. The Mentor switched off the bot from the server side.\n\n" +
                        "2. Your phone was locked and you screen was off.\n\n" +
                        "3. The robot you are using takes time to find signal.",
                select == 2
            ),
            Faqs(
                "How do i contact technical support?",
                "Press the green icon in your bottom right corner.",
                select == 3
            ),
            Faqs(
                "How do i get the installation video? ",
                "Press the purple icon in your bottom right corner.",
                select == 4
            )
        )
    }

}