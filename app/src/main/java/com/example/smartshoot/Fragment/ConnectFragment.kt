package com.example.smartshoot.Fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartshoot.R
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import com.google.android.material.button.MaterialButton
import java.nio.charset.Charset

class ConnectFragment : Fragment() {
    private val TAG = "ConnectFragment"
    private lateinit var mView: View
    private lateinit var con_BTN_send: MaterialButton
    private lateinit var subscribeSwitch: SwitchCompat
    private lateinit var publishSwitch: SwitchCompat
    private lateinit var nearbyMsgRecyclerView: RecyclerView


    /**
     * Sets the time to live in seconds for the publish or subscribe.
     */
    private val TTL_IN_SECONDS = 120 // Two minutes.

    /**
     * Choose of strategies for publishing or subscribing for nearby messages.
     */
    private val PUB_SUB_STRATEGY = Strategy.Builder().setTtlSeconds(TTL_IN_SECONDS).build()

    /**
     * The [Message] object used to broadcast information about the device to nearby devices.
     */
    private lateinit var message: Message

    /**
     * A [MessageListener] for processing messages from nearby devices.
     */
    private lateinit var messageListener: MessageListener

    /**
     * MessageAdapter is a custom class that we will define later. It's for adding
     * [messages][Message] to the [RecyclerView]
     */
    private lateinit var msgAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_connect, container , false)
        mView = view
        findViews(view)
        setupMessagesDisplay()
        subscribeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                subscribe()
            } else {
                unsubscribe()
            }
        }

        publishSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                publish()
            } else {
                unpublish()
            }
        }
        con_BTN_send.setOnClickListener {}

        // The message being published is simply the Build.MODEL of the device. But since the
        // Messages API is expecting a byte array, you must convert the data to a byte array.
        message = Message(Build.MODEL.toByteArray(Charset.forName("UTF-8")))

        messageListener = object : MessageListener() {
            override fun onFound(message: Message) {
                // Called when a new message is found.
                val msgBody = String(message.content)
                msgAdapter.addItem(msgBody)
            }

            override fun onLost(message: Message) {
                // Called when a message is no longer detectable nearby.
                val msgBody = String(message.content)
                msgAdapter.removeItem(msgBody)
            }
        }
        return view
    }

    private fun findViews(view: View) {
        con_BTN_send = view.findViewById(R.id.con_BTN_send)
        subscribeSwitch = view.findViewById(R.id.subscribe_switch)
        publishSwitch = view.findViewById(R.id.publish_switch)
        nearbyMsgRecyclerView = view.findViewById(R.id.nearby_msg_recycler_view)

    }

    private fun publish() {
        Log.d(TAG, "publish: ")
        val options = PublishOptions.Builder()
            .setStrategy(PUB_SUB_STRATEGY)
            .setCallback(object : PublishCallback() {
                override fun onExpired() {
                    super.onExpired()
                    // flick the switch off since the publishing has expired.
                    // recall that we had set expiration time to 120 seconds
                    activity?.runOnUiThread {
                        publishSwitch.isChecked = false
                    }
                    activity?.runOnUiThread() {
                        publishSwitch.isChecked = false
                    }
                }
            }).build()

        Nearby.getMessagesClient(requireActivity()).publish(message, options)
    }

    private fun unpublish() {
        Log.d(TAG, "unpublish: ")
        Nearby.getMessagesClient(requireContext()).unpublish(message)
    }

    private fun subscribe() {
        Log.d(TAG, "subscribe: ")
        val options = SubscribeOptions.Builder()
            .setStrategy(PUB_SUB_STRATEGY)
            .setCallback(object : SubscribeCallback() {
                override fun onExpired() {
                    super.onExpired()
                    activity?.runOnUiThread {
                        subscribeSwitch.isChecked = false
                    }
                }
            }).build()

        Nearby.getMessagesClient(requireActivity()).subscribe(messageListener, options)
    }

    private fun unsubscribe() {
        Log.d(TAG, "unsubscribe: ")
        Nearby.getMessagesClient(requireActivity()).unsubscribe(messageListener)
    }

    private fun setupMessagesDisplay() {
        msgAdapter = MessageAdapter()
        with(nearbyMsgRecyclerView) {
            layoutManager = LinearLayoutManager(context)
            this.adapter = msgAdapter
        }
    }

}

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageVH>() {
    private var itemsList: MutableList<String> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageVH {
        return MessageVH(TextView(parent.context))
    }

    override fun onBindViewHolder(holder: MessageVH, position: Int) {
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