package com.example.asrandroidclient.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.tool.ByteArrayQueue
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Created by Excalibur on 2017/5/30.
 */
class ChatService {
    private val mAdapter: BluetoothAdapter
    private var mAcceptThread: AcceptThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int
    private var byteArrayQueue = ByteArrayQueue()

    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
    }

    @get:Synchronized
    @set:Synchronized
    var state: Int
        get() = mState
        private set(state) {
            mState = state
        }

    @Synchronized
    fun start() {
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread == null) {
            mAcceptThread = AcceptThread()
            mAcceptThread?.start()
        }
        state = STATE_LISTEN
    }


    // 开启一个ConnectThread来管理对应的当前连接。之前取消任意现存的mConnectThread
    // mConnectThread，mAcceptThread线程，然后开启新的mConnectThread，传入当前
    // 刚刚接受的socket连接，最后通过Handler来通知UI连接
    @Synchronized
    fun connected(
        socket: BluetoothSocket?,
        device: BluetoothDevice?
    ) {
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread?.cancel()
            mAcceptThread = null
        }
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()
        state = STATE_CONNECTED
    }

    // 停止所有相关线程，设当前状态为none
    @Synchronized
    fun stop() {
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        state = STATE_NONE
    }

    // 在STATE_CONNECTED状态下，调用mConnectedThread里的write方法，写入byte
    fun write(out: ByteArray?) {
        var r: ConnectedThread?
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        r?.write(out)
    }

    // 连接失败的时候处理，通知UI，并设为STATE_LISTEN状态
    private fun connectionFailed() {
        state = STATE_LISTEN
        start()
    }

    // 当连接失去的时候，设为STATE_LISTEN
    private fun connectionLost() {
        state = STATE_LISTEN
        start()
    }

    // 创建监听线程，准备接受新连接。使用阻塞方式，调用BluetoothServerSocket.accept()
    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
            }
            mmServerSocket = tmp
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (mState != STATE_CONNECTED) {
                socket = try {
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    break
                }
                if (socket != null) {
                    connected(socket, socket.remoteDevice)
                    Logger.i("客户端接入成功：${socket.remoteDevice.name}")
                    MyApp.mainViewModel.bleConnectedEvent.postValue(true)
                    try {
                        mmServerSocket.close()
                    } catch (e: IOException) {
                        Logger.e("连接报错：${e.message}")
                    }
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }


    // 双方蓝牙连接后一直运行的线程。构造函数中设置输入输出流。
    // Run方法中使用阻塞模式的InputStream.read()循环读取输入流
    // 然后psot到UI线程中更新聊天信息。也提供了write()将聊天消息写入输出流传输至对方，
    // 传输成功后回写入UI线程。最后cancel()关闭连接的socket
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            Logger.i("初始化输入输出线程")
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    parseData(buffer, bytes)
                } catch (e: IOException) {
                    connectionLost()
                    Logger.e("socket连接失效")
                    break
                }
                // Logger.i("读取数据：$bytes")
            }
        }

        fun write(buffer: ByteArray?) {
            try {
                mmOutStream!!.write(buffer)
            } catch (e: IOException) {
                Logger.i("Send Fail")
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }

    /**
     * 解析
     */
    fun parseData(buffer: ByteArray, length: Int) {
        Logger.i("接收的信息:${String(buffer, 0, length)}")
        val jsonStr = String(buffer, 0, length)
        val bleData = Gson().fromJson(jsonStr, BleData::class.java)
        if (bleData.url.contains("http")) {
            MyApp.socketEventViewModel.setUrl(bleData.url)
        }

    }

    companion object {
        private const val NAME = "PREVENT"

        // UUID-->通用唯一识别码，能唯一地辨识咨询
        private val MY_UUID = UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )

        //串口
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }
}