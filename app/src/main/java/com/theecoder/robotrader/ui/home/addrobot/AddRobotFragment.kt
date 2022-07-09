package com.theecoder.robotrader.ui.home.addrobot

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.theecoder.robotrader.R
import com.theecoder.robotrader.network.module.AuthBody
import com.theecoder.robotrader.ui.HomeActivity
import com.theecoder.robotrader.utils.Resource

class AddRobotFragment : Fragment(R.layout.add_robot_fragment) {

    private lateinit var viewModel: AddRobotViewModel
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back(view)
        viewModel = (activity as HomeActivity).addRobotViewModel

        view.findViewById<Button>(R.id.submit).setOnClickListener{
            val key = view.findViewById<TextInputEditText>(R.id.outlined_edit_text)
            if (key.text?.length!! > 0){
                viewModel.authenticate(AuthBody(key.text.toString(),null))

                val builder = AlertDialog.Builder(context)
                var dialog: ProgressDialog? = null
                viewModel.accountData.observe(viewLifecycleOwner,{
                    when(it){
                        is Resource.Error ->{
                            dialog?.dismiss()
                            builder.setTitle("Error")
                            builder.setMessage(it.message.toString())
                            builder.setPositiveButton("OK"){
                                    dialog,which->
                                dialog.dismiss()
                            }
                            val alertDialog = builder.create()
                            alertDialog.show()
                        }
                        is Resource.Loading ->{
                            dialog?.dismiss()
                            dialog = ProgressDialog.show(context, "Loading", "Please wait...", true)
                        }
                        is Resource.Success ->{
                            dialog?.dismiss()
                            builder.setTitle("Success")
                            builder.setMessage("New account is successfully added!!")
                            builder.setPositiveButton("OK"){
                                    dialog,which->
                                dialog.dismiss()
                            }
                            val alertDialog = builder.create()
                            alertDialog.show()

                        }
                    }
                })
            }else
            {
                Toast.makeText(context,"Text Box is empty", Toast.LENGTH_SHORT).show()
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