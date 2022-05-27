package com.example.smartshoot.Fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import com.example.smartshoot.R
import android.widget.ArrayAdapter





class SettingFragment : Fragment() {

    private lateinit var before_SPN_setting: Spinner
    private lateinit var after_SPN_setting: Spinner
    private lateinit var ball_SPN_setting: Spinner
    private lateinit var shirt_SPN_setting: Spinner
    val secondsList = listOf("1 SEC","2 SEC", "3 SEC" , "4 SEC" , "5 SEC" )
    val colorsList = listOf("Orange","White", "Black", "Blue" , "Green" ,"Red" )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_setting, container , false)
        findViews(view)
        UpdateAdapter()

        before_SPN_setting.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putInt("beforeTime", position + 1)
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        after_SPN_setting.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putInt("afterTime", position + 1)
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        ball_SPN_setting.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putString("ballColor", colorsList[position])
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        shirt_SPN_setting.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putString("shirtColor", colorsList[position])
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        return view
    }

    private fun UpdateAdapter() {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item, secondsList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        before_SPN_setting.setAdapter(adapter)
        after_SPN_setting.setAdapter(adapter)

        val adapter2: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item, colorsList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ball_SPN_setting.setAdapter(adapter2)
        shirt_SPN_setting.setAdapter(adapter2)

        before_SPN_setting.setSelection(2)
        after_SPN_setting.setSelection(2)
        ball_SPN_setting.setSelection(0)
        shirt_SPN_setting.setSelection(1)
    }

    private fun findViews(view: View) {
          before_SPN_setting = view.findViewById(R.id.spinner1)
          after_SPN_setting  = view.findViewById(R.id.spinner2)
           ball_SPN_setting  = view.findViewById(R.id.spinner3)
          shirt_SPN_setting  = view.findViewById(R.id.spinner4)

    }


}