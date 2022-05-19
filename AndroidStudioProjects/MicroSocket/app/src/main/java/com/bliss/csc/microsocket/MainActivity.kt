package com.bliss.csc.microsocket

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bliss.csc.microsocket.databinding.ActivityMainBinding

import android.widget.Toast
import android.os.AsyncTask
import java.io.*
import java.lang.Exception
import java.net.*


class MainActivity : AppCompatActivity() {

    companion object{
        var socket = Socket()
        var addr: SocketAddress? = null
        lateinit var outstream: DataOutputStream
        lateinit var instream: DataInputStream
        lateinit var cManager: ConnectivityManager

        var ip = "192.168.0.1"
        var port = 2222
        var mHandler = Handler()
        var closed = false

    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener {
            if(binding.userIPEditText.text.isEmpty()) {
                ip = binding.userIPEditText.text.toString()
            }
        }
        binding.connectReleaseButton.setOnClickListener {
            if(!socket.isClosed){
                Disconnect().start()
            }
        }
    }

    class Connect:Thread(){

        override fun run(){
            try{
                socket = Socket(ip, port)
                outstream = DataOutputStream(socket.getOutputStream())
                instream = DataInputStream(socket.getInputStream())
                val b = instream.read()
                if(b==1){    //서버로부터 접속이 확인되었을 때
                    mHandler.obtainMessage(11).apply {
                        sendToTarget()
                    }
                    ClientSocket().start()
                }else{    //서버 접속에 성공하였으나 서버가 응답을 하지 않았을 때
                    mHandler.obtainMessage(14).apply {
                        sendToTarget()
                    }
                    socket.close()
                }
            }catch(e:Exception){    //연결 실패
                val state = 1
                mHandler.obtainMessage(state).apply {
                    sendToTarget()
                }
                socket.close()
            }

        }
    }

    class ClientSocket:Thread(){
        override fun run() {
            try{
                while (true) {
                    val ac = instream.read()
                    if(ac == 2) {    //서버로부터 메시지 수신 명령을 받았을 때
                        val bac = instream.readUTF()
                        val input = bac.toString()
                        val recvInput = input.trim()

                        val msg = mHandler.obtainMessage()
                        msg.what = 3
                        msg.obj = recvInput
                        mHandler.sendMessage(msg)
                    }else if(ac == 10){    //서버로부터 접속 종료 명령을 받았을 때
                        mHandler.obtainMessage(18).apply {
                            sendToTarget()
                        }
                        socket.close()
                        break
                    }
                }
            }catch(e: SocketException){    //소켓이 닫혔을 때
                mHandler.obtainMessage(15).apply {
                    sendToTarget()
                }
            }
        }
    }




    class Disconnect: Thread(){
        override fun run() {
            try{
                outstream.write(10)
                socket.close()
            }catch (e:Exception){

            }
        }
    }
}