package com.astontraineeship.audioplayer

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlin.time.Duration.Companion.seconds


private const val NOTIFICATION_ID = 1
private const val FIRST_TRACK_INDEX = 0

class PlayerService : Service() {
    private lateinit var player: MediaPlayer
    private var isPause: Boolean? = null
    private val listMusic = arrayOf(
        R.raw.track1 to "Track1",
        R.raw.track2 to "Track2",
        R.raw.track3 to "Track3"
    )
    private var currentTrackIndex = FIRST_TRACK_INDEX

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            Actions.START.toString() -> start()
            Actions.PAUSE.toString() -> pause()
            Actions.PREVIOUS.toString() -> previous()
            Actions.NEXT.toString() -> next()
            Actions.CLOSE.toString() -> onDestroy()
        }
        startPlayerForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startPlayerForeground() {
        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this, 0, resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val remoteViews = RemoteViews(packageName, R.layout.notification)
        remoteViews.setOnClickPendingIntent(R.id.root, resultPendingIntent)
        remoteViews.setTextViewText(R.id.notification_text_name, listMusic[currentTrackIndex].second)
        listener(remoteViews, applicationContext)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setAutoCancel(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }


    private fun next() {
        if (currentTrackIndex >= listMusic.size - 1) currentTrackIndex = 0
        else currentTrackIndex++

        start(listMusic[currentTrackIndex].first)
    }

    private fun previous() {
        if (currentTrackIndex == 0) currentTrackIndex = listMusic.size - 1
        else currentTrackIndex--

        start(listMusic[currentTrackIndex].first)
    }

    private fun pause() {
        if (player.isPlaying) {
            player.pause()
            isPause = true
        }
    }

    private fun start() {
        when (isPause) {
            null -> {
                player = MediaPlayer.create(this, listMusic[currentTrackIndex].first)
                player.setOnCompletionListener {
                    next()
                }
                isPause = false
                player.start()
                sendBroadcastIntent()
            }
            true -> {
                player.seekTo(player.currentPosition)
                isPause = false
                player.start()
                sendBroadcastIntent()
            }
            false -> {
                //Nothing
            }
        }
    }

    private fun start(trackId: Int) {
        if (isPause != null && player.isPlaying) player.pause()
        player = MediaPlayer.create(this, trackId)
        player.setOnCompletionListener {
            next()
        }
        isPause = false
        player.start()

        sendBroadcastIntent()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        this.stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun listener(remoteViews: RemoteViews, context: Context) {
        val intentPlay = Intent(Actions.START.toString())
        val intentPause = Intent(Actions.PAUSE.toString())
        val intentPrevious = Intent(Actions.PREVIOUS.toString())
        val intentNext = Intent(Actions.NEXT.toString())

        val pendingIntentPlay = createBroadcastPendingIntent(context, intentPlay)
        val pendingIntentPause = createBroadcastPendingIntent(context, intentPause)
        val pendingIntentPrevious = createBroadcastPendingIntent(context, intentPrevious)
        val pendingIntentNext = createBroadcastPendingIntent(context, intentNext)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.START.toString())
        intentFilter.addAction(Actions.PAUSE.toString())
        intentFilter.addAction(Actions.PREVIOUS.toString())
        intentFilter.addAction(Actions.NEXT.toString())

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action){
                    Actions.START.toString() -> start()
                    Actions.PAUSE.toString() -> pause()
                    Actions.PREVIOUS.toString() -> previous()
                    Actions.NEXT.toString() -> next()
                }
            }
        }

        context.registerReceiver(broadcastReceiver, intentFilter)

        remoteViews.setOnClickPendingIntent(R.id.notification_play, pendingIntentPlay)
        remoteViews.setOnClickPendingIntent(R.id.notification_pause, pendingIntentPause)
        remoteViews.setOnClickPendingIntent(R.id.notification_previous, pendingIntentPrevious)
        remoteViews.setOnClickPendingIntent(R.id.notification_next, pendingIntentNext)
    }

    private fun createBroadcastPendingIntent(context: Context, intent: Intent) : PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun sendBroadcastIntent() {
        val intent = Intent(TRACK_INFO_RECEIVED)
        intent.putExtra(TRACK_NAME, listMusic[currentTrackIndex].second)
        intent.putExtra(TRACK_LENGTH, player.duration)
        sendBroadcast(intent)
    }
}