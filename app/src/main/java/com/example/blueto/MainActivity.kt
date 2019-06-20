package com.example.blueto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.content.Intent
import android.bluetooth.BluetoothDevice
import java.nio.file.Files.size
import java.util.*
import kotlin.collections.ArrayList
import android.system.Os.accept
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothServerSocket
import android.os.Handler
import android.os.Message
import android.widget.ArrayAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.R.attr.data


val MY_UUID: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
val NAME = "test"
var devices = java.util.ArrayList<BluetoothDevice>()
var devicesMap = HashMap<String, BluetoothDevice>()
var mArrayAdapter: ArrayAdapter<String>? = null
val MESSAGE_READ = 99999

class MainActivity : AppCompatActivity() {
    private lateinit var viewAdapter: Adapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var msgs: ArrayList<Mensaje>
    val REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewManager = LinearLayoutManager(this)
        viewAdapter = Adapter(msgs)
        id_rv.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }


        if (mBluetoothAdapter == null) {

        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        val pairedDevices = mBluetoothAdapter.bondedDevices

        if (pairedDevices.size > 0) {

            for (device in pairedDevices) {

                mArrayAdapter?.add(device.name + "\n" + device.address)
            }
        }




    }
    val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    var data = (msg.obj as String)
                    addMsg(Mensaje("", data))
                    upsateRv(msgs)
                }
            }


            /**/
        }
    }

    fun upsateRv(list: ArrayList<Mensaje>) {
        viewAdapter.changeDataSet(list)
    }

    fun addMsg(msg: Mensaje) {
        msgs.add(msg)
    }

    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket?

        init {

            var tmp: BluetoothServerSocket? = null
            try {

                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
            }

            mmServerSocket = tmp
        }

        override fun run() {
            var socket: BluetoothSocket? = null

            while (true) {
                try {
                    socket = mmServerSocket!!.accept()
                } catch (e: IOException) {
                    break
                }

                if (socket != null) {

                    //manageConnectedSocket(socket)
                    mmServerSocket.close()
                    break
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish  */
        fun cancel() {
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
            }

        }
    }

    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {

            var tmp: BluetoothSocket? = null


            try {

                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
            }

            mmSocket = tmp
        }

        override fun run() {

            mBluetoothAdapter.cancelDiscovery()

            try {

                mmSocket!!.connect()
            } catch (connectException: IOException) {

                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                }

                return
            }


            //manageConnectedSocket(mmSocket)
        }

        /** Will cancel an in-progress connection, and close the socket  */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }

        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null


            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {

                    bytes = mmInStream!!.read(buffer)

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    break
                }

            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
            }

        }


        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }

        }
    }


}
