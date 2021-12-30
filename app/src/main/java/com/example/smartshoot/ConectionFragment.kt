package com.example.smartshoot

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Strategy
import java.lang.Exception

import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo

import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes

import com.google.android.gms.nearby.connection.ConnectionResolution

import com.google.android.gms.nearby.connection.ConnectionInfo

import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import android.content.DialogInterface
import android.net.Uri
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

import com.google.android.gms.nearby.connection.Payload

import com.google.android.gms.nearby.connection.PayloadCallback
import android.os.Build.VERSION_CODES

import android.os.Build.VERSION

import androidx.collection.SimpleArrayMap
import java.io.*
import java.nio.charset.StandardCharsets


class ConectionFragment : Fragment() {
    // P2P_POINT_TO_POINT


    private val TAG = "BlankFragment"
    private lateinit var con_BTN_send: MaterialButton
    private lateinit var con_BTN_receive: MaterialButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_conection, container, false)
        findViews(view)

        con_BTN_send.setOnClickListener { startAdvertising() }

        con_BTN_receive.setOnClickListener { startDiscovery() }


        return view
    }

    private fun findViews(view: View) {
        con_BTN_receive = view.findViewById(R.id.con_BTN_receive)
        con_BTN_send = view.findViewById(R.id.con_BTN_send)
    }

    private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(
            Strategy.P2P_POINT_TO_POINT
        ).build()
        Nearby.getConnectionsClient(requireContext())
            .startAdvertising(
                "Host", "com.example.smartshoot", connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                Log.d(TAG, "startAdvertising: " + unused)
            }
            .addOnFailureListener { e: Exception? ->
                //todo: print exteption
            }
    }

    private fun startDiscovery() {
        val discoveryOptions =
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(requireContext())
            .startDiscovery("com.example.smartshoot", endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? -> }
            .addOnFailureListener { e: Exception? -> }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                //todo : list of str of user 5 sec and show to user and stop disovery if user choses
                // An endpoint was found. We request a connection to it.
                Nearby.getConnectionsClient(requireContext())
                    .requestConnection(
                        "nathan",//getLocalUserName()/*MY Name*/,
                        endpointId /*chosed name*/,
                        connectionLifecycleCallback
                    )
                    .addOnSuccessListener(
                        OnSuccessListener { unused: Void? -> })
                    .addOnFailureListener(
                        OnFailureListener { e: Exception? -> })
            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
            }
        }


    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                AlertDialog.Builder(context)
                    .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                    .setMessage("Confirm the code matches on both devices: " + connectionInfo.getAuthenticationDigits())
                    .setPositiveButton(
                        "Accept"
                    ) { dialog: DialogInterface?, which: Int ->  // The user confirmed, so we can accept the connection.
                        Nearby.getConnectionsClient(context!!)
                            .acceptConnection(endpointId, ReceiveFilePayloadCallback(requireContext()))
                    }
                    .setNegativeButton(
                        android.R.string.cancel
                    ) { dialog: DialogInterface?, which: Int ->  // The user canceled, so we should reject the connection.
                        Nearby.getConnectionsClient(context!!).rejectConnection(endpointId)
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {}
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                    ConnectionsStatusCodes.STATUS_ERROR -> {}
                    else -> {}
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
            }
        }

    internal class ReceiveFilePayloadCallback(context: Context) :
        PayloadCallback() {
        private val context: Context
        private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
        private val completedFilePayloads = SimpleArrayMap<Long, Payload?>()
        private val filePayloadFilenames = SimpleArrayMap<Long, String>()
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                val payloadId = addPayloadFilename(payloadFilenameMessage)
                processFilePayload(payloadId)
            } else if (payload.type == Payload.Type.FILE) {
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingFilePayloads.put(payload.id, payload)
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
                val uri: Uri = filePayload.asFile()!!.asUri()!!
                try {
                    // Copy the file to a new location.
                    val mIn: InputStream? = context.getContentResolver().openInputStream(uri)
                    if (mIn != null) {
                        copyStream( mIn , FileOutputStream(File(context.getCacheDir(), filename)))
                    }
                } catch (e: IOException) {
                    // Log the error.
                } finally {
                    // Delete the original file.
                    context.getContentResolver().delete(uri, null, null)
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
                    val uri: Uri = filePayload.asFile()!!.asUri()!!
                    try {
                        // Copy the file to a new location.
                        val sIn: InputStream = context.getContentResolver().openInputStream(uri)!!
                        copyStream(sIn, FileOutputStream(File(context.getCacheDir(), filename)))
                    } catch (e: IOException) {
                        // Log the error.
                    } finally {
                        // Delete the original file.
                        context.getContentResolver().delete(uri, null, null)
                    }
                } else {
                    val payloadFile: File? = filePayload.asFile()!!.asJavaFile()

                    // Rename the file.
                    payloadFile?.renameTo(File(payloadFile.getParentFile(), filename))
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId = update.payloadId
                val payload = incomingFilePayloads.remove(payloadId)
                completedFilePayloads.put(payloadId, payload)
                if (payload!!.type == Payload.Type.FILE) {
                    processFilePayload(payloadId)
                }
            }
        }

        companion object {
            /** Copies a stream from one location to another.  */
            @Throws(IOException::class)
            private fun copyStream(qIn : InputStream, out: OutputStream) {
                try {
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (qIn.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                } finally {
                    qIn.close()
                    out.close()
                }
            }
        }

        init {
            this.context = context
        }
    }
    }





