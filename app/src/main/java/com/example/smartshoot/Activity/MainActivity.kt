package com.example.smartshoot.Activity

import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.chaquo.python.Python
import com.example.smartshoot.ConnectCamera.CameraConnectionFragment
import com.example.smartshoot.Fragment.ConnectFragment
import com.example.smartshoot.Fragment.GameFragment
import com.example.smartshoot.Fragment.HistoryFragment
import com.example.smartshoot.ConnectCamera.ImageUtils
import com.example.smartshoot.R
import com.example.smartshoot.Fragment.SettingFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jcodec.api.android.AndroidSequenceEncoder
import org.jcodec.common.io.FileChannelWrapper
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Rational
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.KITKAT)
class MainActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener {
    private val TAG = "MainActivity"
    private lateinit var main_BNV_menu: BottomNavigationView
    private lateinit var main_TLB_title: MaterialToolbar
    private lateinit var main_BTN_start: MaterialButton
    private lateinit var main_BTN_startHightlight: MaterialButton
    private lateinit var main_BTN_stop: MaterialButton
    private lateinit var main_BTN_startSpotlight: MaterialButton
    private lateinit var validateHoop_BTN_main: MaterialButton
    private lateinit var detecting_LBL_main: TextView
    private lateinit var container: FrameLayout
    private lateinit var image_IMG_main: ImageView

    private val db = Firebase.firestore

    private var hoopfinded: Boolean = false
    private var highlight: Boolean = false
    private var spotlight: Boolean = false
    private var isStoped: Boolean = false
    private var shootDetected: Boolean = false
    private var detectedFrame: Boolean = false
    private val mFrames: ArrayList<Bitmap> = ArrayList<Bitmap>()
    private val maxFps = 300
    private var beforeSecond = 3
    private var afterSecond = 3
    private var unSpot = 30
    private val fps = 30
    private var counter = 0
    private var ballColor = "Orange"
    private var shirtColor = "White"

    private val py = Python.getInstance()
    private val pyMoudle = py.getModule("API")

    private lateinit var fragmentVideo: Fragment
    private lateinit var fragmentAmit: CameraConnectionFragment

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViews()

        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()

        val myFragmentGame = GameFragment()
        val myFragmentConnect = ConnectFragment()
        val myFragmentHistory = HistoryFragment()
        val myFragmentSetting = SettingFragment()

        fragmentTransaction.add(R.id.main_LAY_app, myFragmentGame).commit()
        getSettingFromUser()


        main_BNV_menu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_game -> {
                    changeFrag(fragmentManager, myFragmentGame, "Game")
                    getSettingFromUser()
                    main_BTN_start.visibility = View.VISIBLE
                    true
                }
                R.id.page_connect -> {
                    main_BTN_start.visibility = View.GONE
                    changeFrag(fragmentManager, myFragmentConnect, "Connect")
                    true
                }
                R.id.page_history -> {
                    main_BTN_start.visibility = View.GONE
                    changeFrag(fragmentManager, myFragmentHistory, "History")

                    true
                }
                R.id.page_settings -> {
                    main_BTN_start.visibility = View.GONE
                    changeFrag(fragmentManager, myFragmentSetting, "Setting")
                    true

                }
                else -> false
            }
        }
        main_BTN_start.setOnClickListener {
            Log.d(TAG, "onCreate: main_BTN_find")
            isStoped = false
            setFragment()
            main_BTN_startHightlight.visibility = View.VISIBLE
            main_BTN_startSpotlight.visibility = View.VISIBLE
            main_BTN_start.visibility = View.GONE
            detecting_LBL_main.visibility = View.VISIBLE
        }
        main_BTN_startHightlight.setOnClickListener {
            Log.d(TAG, "onCreate: main_BTN_startHightlight")
            main_BTN_stop.visibility = View.VISIBLE
            main_BTN_startHightlight.visibility = View.GONE
            main_BTN_startSpotlight.visibility = View.GONE
            highlight = true
        }
        main_BTN_startSpotlight.setOnClickListener {
            main_BTN_stop.visibility = View.VISIBLE
            main_BTN_startSpotlight.visibility = View.GONE
            main_BTN_startHightlight.visibility = View.GONE
            spotlight = true
        }
        main_BTN_stop.setOnClickListener {
            closeCamera()
            isStoped = true
            image_IMG_main.visibility = View.GONE
            main_BTN_start.visibility = View.VISIBLE
            main_BTN_stop.visibility = View.GONE
            storeGameInDb()

        }
        validateHoop_BTN_main.setOnClickListener {
            hoopfinded = true
            validateHoop_BTN_main.visibility = View.GONE
            detecting_LBL_main.visibility = View.GONE
        }

    }

    private fun getSettingFromUser() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        beforeSecond = sharedPref.getInt("beforeTime", 3)
        afterSecond = sharedPref.getInt("afterTime", 3)
        ballColor = sharedPref.getString("ballColor", "Orange").toString()
        shirtColor = sharedPref.getString("shirtColor", "White").toString()

        Log.d(TAG, "getSettingFromUser: " + beforeSecond)
        Log.d(TAG, "getSettingFromUser: " + afterSecond)
        Log.d(TAG, "getSettingFromUser: " + ballColor)
        Log.d(TAG, "getSettingFromUser: " + shirtColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun storeGameInDb() {
        val ShootNum = pyMoudle.callAttr("get_num_of_shots")

        val game = hashMapOf(
            "shoot_number" to ShootNum.toInt(),
            "date" to SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date()),
            "user_id" to Firebase.auth.currentUser!!.uid
        )
        Log.d(TAG, "storeGameInDb: " + game)
        db.collection("game")
            .add(game)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

    }

    private fun changeFrag(
        fragmentManager: FragmentManager,
        myFragment: androidx.fragment.app.Fragment,
        fragName: String
    ) {
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_LAY_app, myFragment)
            .addToBackStack(null)
            .commit()
        main_TLB_title.title = fragName
    }


    private fun findViews() {
        main_BNV_menu = findViewById(R.id.main_BNV_menu)
        main_TLB_title = findViewById(R.id.main_TLB_title)
        main_BTN_start = findViewById(R.id.main_BTN_start)
        main_BTN_startHightlight = findViewById(R.id.main_BTN_startHightlight)
        main_BTN_stop = findViewById(R.id.main_BTN_stop)
        main_BTN_startSpotlight = findViewById(R.id.main_BTN_startSpotlight)
        validateHoop_BTN_main = findViewById(R.id.validateHoop_BTN_main)
        detecting_LBL_main = findViewById(R.id.detecting_LBL_main)
        container = findViewById(R.id.container)
        image_IMG_main = findViewById(R.id.image_IMG_main)
        image_IMG_main.rotation = 90F
        val num = pyMoudle.callAttr("init")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //show live camera footage
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //setFragment()
        } else {
            finish()
        }
    }

    var previewHeight = 0;
    var previewWidth = 0
    var sensorOrientation = 0;
    //fragment which show llive footage from camera

    protected fun setFragment() {
        Log.d(TAG, "setFragment: ")
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraId: String? = null
        try {
            cameraId = manager.cameraIdList[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val camera2Fragment = CameraConnectionFragment.newInstance(
            object :
                CameraConnectionFragment.ConnectionCallback {
                override fun onPreviewSizeChosen(size: Size?, rotation: Int) {

                    previewHeight = size!!.height
                    previewWidth = size.width
                    sensorOrientation = rotation - getScreenOrientation()
                }
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )

        camera2Fragment.setCamera(cameraId)
        fragmentVideo = camera2Fragment
        fragmentManager.beginTransaction().replace(R.id.container, fragmentVideo).commit()


    }

    protected fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    //getting frames of live camera footage and passing them to model
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private var rgbFrameBitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0]!!,
                    yuvBytes[1]!!,
                    yuvBytes[2]!!,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            processImage()
        } catch (e: Exception) {
            Log.d(TAG, "onImageAvailable: ", e)
            return
        }
    }


    private fun processImage() {
        imageConverter!!.run()
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        rgbFrameBitmap?.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight)
        val bitmap = rgbFrameBitmap
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val image = stream.toByteArray()

        if (highlight) {
            closeCamera()
            if (!hoopfinded) {
                val isHoopDetected = pyMoudle.callAttr("detect_hoop", image)
                Log.d(TAG, "processImage: $isHoopDetected")
                if (isHoopDetected.toInt() == 1) {
                    runOnUiThread {
                        validateHoop_BTN_main.visibility = View.VISIBLE
                    }
                    //Show the frame from get_status
                    Log.d(TAG, "detected img finded:")
                    detectedFrame = true
                }else{
                    runOnUiThread {
                        validateHoop_BTN_main.visibility = View.GONE
                    }
                }

            } else {
                // sent to highlights
                if (mFrames.size == maxFps) {
                    mFrames.removeAt(0)
                }

                mFrames.add(bitmap!!)
                val mHighlight = pyMoudle.callAttr("highlights", image, ballColor)
                val statusGame = pyMoudle.callAttr("get_status")
                if (statusGame.toString() == "1" || shootDetected) {
                    shootDetected = true
                    counter = counter + 1

                } else if (statusGame.toString() == "-1") {
                    Log.d(TAG, "processImage: error")
                }
                if (counter == afterSecond * fps) {
                    saveVideo(0)
                }
            }

            runOnUiThread {
                image_IMG_main.setImageBitmap(changeFrame())
            }

        }
        if (spotlight) {
            closeCamera()
            // sent to spotlight
            val mSpotlight = pyMoudle.callAttr("spotlight", image, shirtColor)
            val statusGame = pyMoudle.callAttr("get_status")
            Log.d(TAG, "processImage: " + statusGame)
            if (statusGame.toInt() == 1) {
                mFrames.add(bitmap!!)
                Log.d(TAG, "processImage: save bitmap")
            } else {
                mFrames.add(bitmap!!)
                unSpot = unSpot + 1
                if (unSpot == (3 * fps)) {
                    if (mFrames.size >= (2 * fps)) {
                        Log.d(TAG, "processImage: go to save")
                        saveVideo(1)
                        mFrames.clear()
                    } else {
                        mFrames.clear()
                    }
                    unSpot = 0
                }

            }
            runOnUiThread {
                image_IMG_main.setImageBitmap(changeFrame())
            }

        }

        postInferenceCallback!!.run()
    }

    private fun changeFrame(): Bitmap {
        val photoFromPython = pyMoudle.callAttr("get_frame")
        val str = photoFromPython.toString()
        val data = android.util.Base64.decode(str, android.util.Base64.DEFAULT)
        val bpm: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return bpm
    }

    private fun saveVideo(mode: Int) {
        Log.d(TAG, "saveVideo: ")
        if (mode == 0) {

        } else {

        }
        var num = 0
        if (mFrames.size - 1 - (beforeSecond * fps) < 0) {
            num = 0

        } else {
            num = mFrames.size - 1 - (beforeSecond * fps)
        }
        var video = mFrames.subList(num, mFrames.size)

        if (video.size > (afterSecond + beforeSecond) * fps * 0) {
            Log.d(TAG, "saveVideo: "+ video.size)
            val file: File = File(this.filesDir.path.toString()+"/amitandnathantesting.mp4")
            file.createNewFile()
            val out: FileChannelWrapper = NIOUtils.writableFileChannel(file.getAbsolutePath());

            try {
                val encoder: AndroidSequenceEncoder = AndroidSequenceEncoder(out, Rational.R(15, 1))
                video.forEach { bitmap ->
                    encoder.encodeImage(bitmap);
                }
                encoder.finish();
                Log.d(TAG, "savedVideo: " + file.getAbsolutePath())
            } finally {
                NIOUtils.closeQuietly(out);
            }
        }
        shootDetected = false
        counter = 0
    }

    protected fun fillBytes(
        planes: Array<Image.Plane>,
        yuvBytes: Array<ByteArray?>
    ) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }

    }

    //rotate image if image captured on samsung devices
    //Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    fun rotateBitmap(input: Bitmap): Bitmap? {
        Log.d("trySensor", sensorOrientation.toString() + "     " + getScreenOrientation())
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(sensorOrientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    private fun closeCamera() {
        runOnUiThread { container.visibility = View.GONE }
    }
}

