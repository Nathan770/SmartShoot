package com.example.smartshoot.Fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartshoot.Albums
import com.example.smartshoot.R
import com.example.smartshoot.VideoAdapter
import com.example.smartshoot.VideoObj
import java.io.File


class HistoryFragment : Fragment() {


    private val TAG = "HistoryFragment"
    private lateinit var videoAdapter: VideoAdapter
    private var videoList = ArrayList<VideoObj>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_history, container , false)
        findViews(view)
        videoList.add(VideoObj("Test" , "01/01/22" , "00:30" , "https://i.ytimg.com/vi/G40Vap3va7U/maxresdefault.jpg"))

        val recyclerView: RecyclerView = view.findViewById(R.id.history_RCV_menu)
        videoAdapter = VideoAdapter(videoList ,this)
        val layoutManager = LinearLayoutManager(this.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = videoAdapter

        return view
    }

    private fun findViews(view: View) {

    }

    private fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 6036)
    }


    private fun checkSelfPermission(): Boolean {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        } else
            return true
    }

    fun loadAllImages() {
        var imagesList = ""
        Log.d(TAG, "loadAllImages: " +  imagesList)
    }

}