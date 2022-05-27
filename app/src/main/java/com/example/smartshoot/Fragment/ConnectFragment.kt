package com.example.smartshoot.Fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartshoot.R
import com.google.android.material.button.MaterialButton
import com.google.android.gms.nearby.Nearby

import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.lang.Exception
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes

import com.google.android.gms.nearby.connection.ConnectionResolution

import com.google.android.gms.nearby.connection.ConnectionInfo

import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback

import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo

import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import android.content.DialogInterface
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

import com.google.android.gms.nearby.connection.Payload

import com.google.android.gms.nearby.connection.PayloadCallback
import android.os.ParcelFileDescriptor

import android.app.Activity
import android.content.Context

import android.content.Intent
import android.net.Uri
import java.nio.charset.StandardCharsets
import android.os.Build.VERSION_CODES

import android.os.Build.VERSION

import androidx.collection.SimpleArrayMap
import java.io.*

class ConnectFragment : Fragment() {
    private val TAG = "ConnectFragment"
    private lateinit var mView: View
    private lateinit var con_BTN_send: MaterialButton
    private lateinit var subscribeSwitch: SwitchCompat
    private lateinit var publishSwitch: SwitchCompat
    private lateinit var nearbyMsgRecyclerView: RecyclerView
    private var mEndpointId = ""
    private lateinit var msgAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_connect, container, false)
        mView = view
        findViews(view)
        subscribeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Log.d(TAG, "onCreateView: startAdvertising")
                publishSwitch.visibility = View.GONE
                startAdvertising()
            } else {
                //unsubscribe()
            }
        }

        publishSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Log.d(TAG, "onCreateView: startDiscovery")
                subscribeSwitch.visibility = View.GONE
                startDiscovery()
            } else {
                //unpublish()
            }
        }
        con_BTN_send.setOnClickListener {
            Log.d(TAG, "onCreateView: "  + mEndpointId)
            showImageChooser(mEndpointId)
        }
        return view
    }

    private fun findViews(view: View) {
        con_BTN_send = view.findViewById(R.id.con_BTN_send)
        subscribeSwitch = view.findViewById(R.id.subscribe_switch)
        publishSwitch = view.findViewById(R.id.publish_switch)
        nearbyMsgRecyclerView = view.findViewById(R.id.nearby_msg_recycler_view)

    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(requireContext()!!)
            .startAdvertising(
                android.os.Build.MODEL, "148nanah", connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener(
                OnSuccessListener { unused: Void? ->
                    Log.d(TAG, "startAdvertising: addOnSuccessListener " + unused )
                })
            .addOnFailureListener(
                OnFailureListener { e: Exception? -> })
    }
    //private val test : ReceiveFilePayloadCallback = ReceiveFilePayloadCallback(requireView().context)

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                AlertDialog.Builder(context)
                    .setTitle("Accept connection to " + info.endpointName)
                    .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
                    .setPositiveButton(
                        "Accept"
                    ) { dialog: DialogInterface?, which: Int ->  // The user confirmed, so we can accept the connection.
                        Nearby.getConnectionsClient(requireContext()).acceptConnection(endpointId!!, ReceiveFilePayloadCallback(requireContext()))
                        Log.d(TAG, "onConnectionInitiated: "+ endpointId)
                        mEndpointId = endpointId
                        activity?.runOnUiThread {
                            con_BTN_send.visibility = View.VISIBLE
                        }
                    }
                    .setNegativeButton(
                        android.R.string.cancel
                    ) { dialog: DialogInterface?, which: Int ->  // The user canceled, so we should reject the connection.
                        Nearby.getConnectionsClient(requireContext()).rejectConnection(endpointId!!)
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                Log.d(TAG, "onConnectionResult: " + endpointId)
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d(TAG, "onConnectionResult: OK")
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Log.d(TAG, "onConnectionResult: REJECTED")
                    }
                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        Log.d(TAG, "onConnectionResult: ERROR")
                    }
                    else -> {
                        Log.d(TAG, "onConnectionResult: ELSE")
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                Log.d(TAG, "onDisconnected: ")
            }
        }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(requireContext())
            .startDiscovery("148nanah" , endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? ->
                Log.d(TAG, "startDiscovery: addOnSuccessListener" + unused)
            }
            .addOnFailureListener { e: Exception? -> }
    }
    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // An endpoint was found. We request a connection to it.
                Log.d(TAG, "onEndpointFound: " + endpointId)
                Nearby.getConnectionsClient(context!!)
                    .requestConnection(android.os.Build.MODEL, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener(
                        OnSuccessListener { unused: Void? ->
                            Log.d(TAG, "onEndpointFound: " + unused)
                        })
                    .addOnFailureListener(
                        OnFailureListener { e: Exception? -> })
            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
            }
        }

    private val READ_REQUEST_CODE = 42
    private val ENDPOINT_ID_EXTRA = "com.foo.myapp.EndpointId"

    /**
     * Fires an intent to spin up the file chooser UI and select an image for sending to endpointId.
     */
    private fun showImageChooser(endpointId: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        Log.d(TAG, "showImageChooser: " + endpointId)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString("endPoint", endpointId)
            apply()
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {
            //val endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA)
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            val endpointId = sharedPref.getString("endPoint", "")
            Log.d(TAG, "onActivityResult: inside send file : "+ endpointId)
            // The URI of the file selected by the user.
            val uri: Uri? = resultData.data
            val filePayload: Payload
            filePayload = try {
                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd: ParcelFileDescriptor = uri?.let {
                    requireContext().getContentResolver().openFileDescriptor(
                        it, "r")
                }!!
                Payload.fromFile(pfd)
            } catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }

            // Construct a simple message mapping the ID of the file payload to the desired filename.
            val filenameMessage = filePayload.id.toString() + ":" + uri.getLastPathSegment()
            Log.d(TAG, "onActivityResult: filenameMessage : " + filenameMessage)
            // Send the filename message as a bytes payload.
            val filenameBytesPayload =
                Payload.fromBytes(filenameMessage.toByteArray())
            Nearby.getConnectionsClient(requireContext()).sendPayload(endpointId!!, filenameBytesPayload)

            // Finally, send the file payload.
            Nearby.getConnectionsClient(requireContext()).sendPayload(endpointId, filePayload)
        }
    }

    class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageVH>() {
        private var itemsList: MutableList<String> = arrayListOf("check nathan")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageVH {
            return MessageVH(TextView(parent.context))
        }

        override fun onBindViewHolder(holder: MessageVH, position: Int) {
            Log.d("nathan", "onBindViewHolder: " + getItem(position))
            holder.bind(getItem(position))
        }

        override fun getItemCount(): Int = itemsList.size

        private fun getItem(pos: Int): String? = if (itemsList.isEmpty()) null else itemsList[pos]

        fun addItem(item: String) {
            itemsList.add(item)
            notifyItemInserted(itemsList.size)
        }

        fun removeItem(item: String) {
            val pos = itemsList.indexOf(item)
            itemsList.remove(item)
            notifyItemRemoved(pos)
        }

        inner class MessageVH(private val tv: TextView) : RecyclerView.ViewHolder(tv) {
            fun bind(item: String?) {
                item?.let { tv.text = it }
            }
        }
    }

    internal class ReceiveFilePayloadCallback(private val context: Context) :
        PayloadCallback() {
        private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
        private val completedFilePayloads = SimpleArrayMap<Long, Payload?>()
        private val filePayloadFilenames = SimpleArrayMap<Long, String>()
        private val TAG = "ConnectFragment"
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived: "+ payload)
            if (payload.type == Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                val payloadId = addPayloadFilename(payloadFilenameMessage)
                processFilePayload(payloadId)
            } else if (payload.type == Payload.Type.FILE) {
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingFilePayloads.put(payload.id, payload)
//                Log.d(TAG, "onPayloadReceived: "+ payload)
//                val uri: Uri? = payload.asFile()?.asUri()
//                try {
//                    // Copy the file to a new location.
//                    val `in` = uri?.let { context.contentResolver.openInputStream(it) }
//                    copyStream(`in`, FileOutputStream(File(context.cacheDir, "test.png")))
//                    Log.d(TAG, "onPayloadReceived: save succeed")
//                } catch (e: IOException) {
//                    // Log the error.
//                    Log.d(TAG, "onPayloadReceived: error")
//                } finally {
//                    // Delete the original file.
//                    if (uri != null) {
//                        context.contentResolver.delete(uri, null, null)
//                    }
//                }
            }
        }



        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage: String): Long {
            val parts = payloadFilenameMessage.split(":").toTypedArray()
            val payloadId = parts[0].toLong()
            val filename = parts[1]
            filePayloadFilenames.put(payloadId, filename)
            return payloadId
        }

        private fun processFilePayload(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                val uri = filePayload.asFile()!!.asUri()
                try {
                    // Copy the file to a new location.
                    val `in` = context.contentResolver.openInputStream(uri!!)
                    copyStream(`in`, FileOutputStream(File(context.cacheDir, filename)))
                } catch (e: IOException) {
                    Log.d(TAG, "processFilePayload: error")
                } finally {
                    // Delete the original file.
                    context.contentResolver.delete(uri!!, null, null)
                }
            }
        }

        // add removed tag back to fix b/183037922
        private fun processFilePayload2(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                    // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                    // allowed to access filepaths from another process directly. Instead, we must open the
                    // uri using our ContentResolver.
                    val uri = filePayload.asFile()!!.asUri()
                    try {
                        // Copy the file to a new location.
                        val `in` = context.contentResolver.openInputStream(uri!!)
                        copyStream(
                            `in`, FileOutputStream(
                                File(
                                    context.cacheDir, filename
                                )
                            )
                        )
                    } catch (e: IOException) {
                        // Log the error.
                    } finally {
                        // Delete the original file.
                        context.contentResolver.delete(uri!!, null, null)
                    }
                } else {
                    val payloadFile = filePayload.asFile()!!.asJavaFile()

                    // Rename the file.
                    payloadFile!!.renameTo(File(payloadFile.parentFile, filename))
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId = update.payloadId
                Log.d(TAG, "onPayloadTransferUpdate: " + incomingFilePayloads)
                val payload = incomingFilePayloads.remove(770)
                Log.d(TAG, "onPayloadTransferUpdate: after"+ payload)
                completedFilePayloads.put(payloadId, payload)
//                if (payload!!.type == Payload.Type.FILE) {
//                    processFilePayload(payloadId)
//                }
            }
        }

        companion object {
            /** Copies a stream from one location to another.  */
            @Throws(IOException::class)
            private fun copyStream(`in`: InputStream?, out: OutputStream) {
                try {
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`!!.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                } finally {
                    `in`!!.close()
                    out.close()
                }
            }
        }
    }
}