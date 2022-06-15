package com.example.smartshoot.Fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.smartshoot.R
import com.example.smartshoot.Adapter.VideoAdapter
import com.example.smartshoot.Object.VideoObj
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random


class HistoryFragment : Fragment() {

    private val TAG = "HistoryFragment"
    private lateinit var videoAdapter: VideoAdapter
    private var videoList = ArrayList<VideoObj>()
    private val db = Firebase.firestore
    private val imageLST = listOf("https://i.ytimg.com/vi/G40Vap3va7U/maxresdefault.jpg","https://i.f1g.fr/media/sport24/1200x630_crop/var/plain_site/storage/images/basket/nba/actualites/nba-l-incroyable-panier-gagnant-de-lebron-james-face-aux-warriors-en-video/28347805-1-fre-FR/NBA-l-incroyable-panier-gagnant-de-LeBron-James-face-aux-Warriors-en-video.jpg","https://prmeng.rosselcdn.net/sites/default/files/dpistyles_v2/ena_16_9_extra_big/2022/05/11/node_305598/39001466/public/2022/05/11/B9730881530Z.1_20220511184144_000%2BGL1KFM33U.2-0.jpg?itok=_0lx52ul1652287316","https://i.ytimg.com/vi/5rfiZxAAc3M/maxresdefault.jpg","https://static.vecteezy.com/system/resources/thumbnails/001/615/658/original/pov-angle-of-person-dribbling-basketball-free-video.jpg")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_history, container , false)
        findViews(view)

        getData()


        val recyclerView: RecyclerView = view.findViewById(R.id.history_RCV_menu)
        videoAdapter = VideoAdapter(videoList ,this)
        val layoutManager = LinearLayoutManager(this.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = videoAdapter

        return view
    }

    private fun getData() {
        var index  = 1
        db.collection("game")
            .get()
            .addOnSuccessListener { result ->
                videoList.removeAll(videoList)
                for (document in result) {
                    var imgnum = Random.nextInt(1,5)
                    videoList.add(VideoObj("document " + index , document.data["date"].toString() , document.data["shoot_number"].toString() , imageLST[imgnum]))
                    Log.d(TAG, "${document.id} => ${document.data}")
                    index++
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }

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