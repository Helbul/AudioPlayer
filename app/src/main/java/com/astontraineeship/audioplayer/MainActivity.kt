package com.astontraineeship.audioplayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.astontraineeship.audioplayer.databinding.ActivityMainBinding

const val TRACK_INFO_RECEIVED = "TRACK_INFO_RECEIVED"
const val TRACK_NAME = "TRACK_NAME"
const val TRACK_LENGTH = "TRACK_LENGTH"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            buttonPlay.setOnClickListener {
                startPlayerService(Actions.START)
            }

            buttonPause.setOnClickListener {
                startPlayerService(Actions.PAUSE)
            }

            buttonPrevious.setOnClickListener {
                startPlayerService(Actions.PREVIOUS)
            }

            buttonNext.setOnClickListener {
                startPlayerService(Actions.NEXT)
            }
        }

        val broadcastReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent) {
                binding.textName.text = intent.getStringExtra(TRACK_NAME)
                val length = intent.getIntExtra(TRACK_LENGTH, 0) / 1000
                binding.textTrackLength.text = String.format("%d:%02d", length/60, length%60)
            }
        }

        val intentFilter = IntentFilter(TRACK_INFO_RECEIVED)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun startPlayerService(playerAction: Actions) {
        Intent(this@MainActivity, PlayerService::class.java).also {
            it.action = playerAction.toString()
            startService(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startPlayerService(Actions.CLOSE)
    }
}