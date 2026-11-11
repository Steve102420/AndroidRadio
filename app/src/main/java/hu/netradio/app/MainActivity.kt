package hu.netradio.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private lateinit var store: StationStore
    private lateinit var adapter: StationAdapter
    private lateinit var nameText: TextView
    private lateinit var statusText: TextView
    private lateinit var playButton: MaterialButton
    private lateinit var stopButton: MaterialButton

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingStation: Station? = null
    private var autoStarted = false
    private var lastError: String? = null

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            refreshList()
            // A beállításokban "Lejátszás"-t választottak
            if (result.resultCode == Activity.RESULT_OK) {
                store.lastStation()?.let { playStation(it) }
            }
        }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateUi()
        override fun onPlayerError(error: PlaybackException) {
            lastError = error.errorCodeName
            updateUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        // A hangerőgombok mindig a rádió (média) hangerejét állítják
        volumeControlStream = AudioManager.STREAM_MUSIC

        store = StationStore(this)
        autoStarted = savedInstanceState?.getBoolean(KEY_AUTO_STARTED) ?: false

        nameText = findViewById(R.id.nowPlayingName)
        statusText = findViewById(R.id.nowPlayingStatus)
        playButton = findViewById(R.id.playPauseButton)
        stopButton = findViewById(R.id.stopButton)

        adapter = StationAdapter(this, store.getStations(), store.lastUrl)
        val list = findViewById<ListView>(R.id.stationList)
        list.adapter = adapter
        list.emptyView = findViewById(R.id.emptyText)
        list.setOnItemClickListener { _, _, position, _ ->
            playStation(adapter.getItem(position))
        }

        playButton.setOnClickListener {
            val c = controller ?: return@setOnClickListener
            if (c.playWhenReady) {
                // Élő adásnál a szünet helyett leállítunk, így újraindításkor az élő adás szól
                c.stop()
            } else {
                store.lastStation()?.let { playStation(it) }
            }
        }

        stopButton.setOnClickListener { exitApp() }

        connectToService()
    }

    private fun connectToService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = try { future.get() } catch (_: Exception) { return@addListener }
            controller = c
            c.addListener(playerListener)

            val pending = pendingStation
            pendingStation = null
            when {
                pending != null -> playStation(pending)
                // Megnyitáskor azonnal szól a legutóbbi állomás (ha még nem szól)
                !autoStarted && !c.playWhenReady -> store.lastStation()?.let { playStation(it) }
            }
            autoStarted = true
            updateUi()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playStation(station: Station) {
        store.lastUrl = station.url
        adapter.selectedUrl = station.url
        adapter.notifyDataSetChanged()
        lastError = null

        val c = controller
        if (c == null) {
            pendingStation = station
            updateUi()
            return
        }
        val item = MediaItem.Builder()
            .setMediaId(station.url)
            .setUri(station.url)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(station.url)).build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(getString(R.string.app_name))
                    .setStation(station.name)
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
        updateUi()
    }

    private fun updateUi() {
        val c = controller
        val current = c?.currentMediaItem
        val station = store.getStations().firstOrNull { it.url == current?.mediaId }
            ?: store.lastStation()

        nameText.text = station?.name ?: getString(R.string.no_station)

        val playing = c?.playWhenReady == true
        statusText.text = when {
            c == null -> getString(R.string.status_connecting_service)
            lastError != null && c.playbackState == Player.STATE_IDLE ->
                getString(R.string.status_error, lastError)
            playing && c.playbackState == Player.STATE_BUFFERING -> getString(R.string.status_buffering)
            playing && c.playbackState == Player.STATE_READY -> getString(R.string.status_playing)
            playing -> getString(R.string.status_buffering)
            else -> getString(R.string.status_stopped)
        }
        playButton.setIconResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        playButton.text = getString(if (playing) R.string.pause else R.string.play)

        adapter.selectedUrl = current?.mediaId ?: store.lastUrl
        adapter.notifyDataSetChanged()
    }

    private fun refreshList() {
        adapter.stations = store.getStations()
        adapter.notifyDataSetChanged()
        updateUi()
    }

    /** Rádió kikapcsolása és kilépés az alkalmazásból. */
    private fun exitApp() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        releaseController()
        stopService(Intent(this, PlaybackService::class.java))
        finishAndRemoveTask()
    }

    private fun releaseController() {
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTO_STARTED, autoStarted)
    }

    override fun onDestroy() {
        // A lejátszás a háttérszolgáltatásban folytatódik
        releaseController()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_AUTO_STARTED = "auto_started"
    }
}
