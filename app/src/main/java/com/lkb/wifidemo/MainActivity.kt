package com.lkb.wifidemo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {
    var btnOnOff: Button? = null
    var btnDiscover: Button? = null
    var btnSend: Button? = null
    var listView: ListView? = null
    var read_msg_box: TextView? = null
    var connectionStatus: TextView? = null
    var writeMsg: EditText? = null
    var wifiManager: WifiManager? = null
    var mManager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null
    var mReceiver: BroadcastReceiver? = null
    var mIntentFilter: IntentFilter? = null
    var peers: MutableList<WifiP2pDevice> = ArrayList()
    lateinit var deviceNameArray: Array<String?>
    lateinit var deviceArray: Array<WifiP2pDevice?>
    var serverClass: ServerClass? = null
    var clientClass: ClientClass? = null
    var sendReceive: SendReceive? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialWork()
        exqListener()
    }

    var handler = Handler { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                    try {
                        read_msg_box!!.text = tempMsg
                    } catch (ex: Exception) {
                        Log.d("APP-SEND", ex.toString())
                    }
            }
        }
        true
    }

    private fun exqListener() {
        btnOnOff?.setOnClickListener {
            if (wifiManager!!.isWifiEnabled) {
                wifiManager!!.isWifiEnabled = false
                btnOnOff!!.text = "ON"
            } else {
                wifiManager!!.isWifiEnabled = true
                btnOnOff!!.text = "OFF"
            }
        }
        btnDiscover!!.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@setOnClickListener
            }
            mManager!!.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus!!.text = "Discover Started"
                }

                override fun onFailure(i: Int) {
                    connectionStatus!!.text = "Discover Starting Failed"
                }
            })
        }
        listView!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                val device = deviceArray[position]
                val config = WifiP2pConfig()
                config.deviceAddress = device!!.deviceAddress
                mManager!!.connect(mChannel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            applicationContext,
                            "Connected to" + device.deviceName,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reason: Int) {
                        Toast.makeText(applicationContext, "Not Connected", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            }
        btnSend!!.setOnClickListener(View.OnClickListener {
            val msg = writeMsg!!.text.toString()
            try {
                sendReceive!!.write(msg.toByteArray())
            } catch (ex: Exception) {
                return@OnClickListener
            }
        })
    }

    private fun initialWork() {
        btnOnOff = findViewById(R.id.onOff)
        btnDiscover = findViewById(R.id.discover)
        btnSend = findViewById(R.id.sendButton) as Button
        listView = findViewById(R.id.peerListView) as ListView
        read_msg_box = findViewById(R.id.readMsg) as TextView
        connectionStatus = findViewById(R.id.connectionStatus)
        writeMsg = findViewById(R.id.writeMsg) as EditText

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager!!.initialize(this, mainLooper, null)
        mReceiver = WifiDirectBroadcastReceiver(mManager, mChannel, this)
        mIntentFilter = IntentFilter()
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    var peerListListener =
        PeerListListener { peerList ->
            if (peerList.deviceList != peers) {
                peers.clear()
                peers.addAll(peerList.deviceList)
                deviceNameArray = arrayOfNulls(peerList.deviceList.size)
                deviceArray = arrayOfNulls(peerList.deviceList.size)
                var index = 0
                for (device in peerList.deviceList) {
                    deviceNameArray[index] = device.deviceName
                    deviceArray[index] = device
                    index++
                }
                val adapter = ArrayAdapter(
                    applicationContext, android.R.layout.simple_list_item_1, deviceNameArray
                )
                listView!!.adapter = adapter
            }
            if (peers.size == 0) {
                Toast.makeText(applicationContext, "No Device Found", Toast.LENGTH_SHORT).show()
                return@PeerListListener
            }
        }
    var connectionInfoListener =
        ConnectionInfoListener { info ->
            val groupOwnerAddress = info.groupOwnerAddress
            if (info.groupFormed && info.isGroupOwner) {
                connectionStatus!!.text = "Host"
                serverClass = ServerClass()
                serverClass!!.start()
            } else if (info.groupFormed) {
                connectionStatus!!.text = "Client"
                clientClass = ClientClass(groupOwnerAddress)
                clientClass!!.start()
            }
        }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }


    inner class ServerClass : Thread() {
        var socket: Socket? = null
        var serverSocket: ServerSocket? = null
        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                socket = serverSocket!!.accept()
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class SendReceive(private val socket: Socket?) : Thread() {
        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (socket != null) {
                try {
                    bytes = inputStream!!.read(buffer)
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            try {
                inputStream = socket!!.getInputStream()
                outputStream = socket.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class ClientClass(hostAddress: InetAddress) : Thread() {
        var socket: Socket
        var hostAdd: String
        override fun run() {
            try {
                socket.connect(InetSocketAddress(hostAdd, 8888), 500)
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            hostAdd = hostAddress.hostAddress
            socket = Socket()
        }
    }

    companion object {
        const val MESSAGE_READ = 1
    }
}
