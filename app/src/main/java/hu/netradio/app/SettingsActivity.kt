package hu.netradio.app

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Állomások kezelése: új link hozzáadása, szerkesztés, törlés, kiválasztás. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var store: StationStore
    private lateinit var adapter: StationAdapter
    private lateinit var nameInput: TextInputEditText
    private lateinit var urlInput: TextInputEditText
    private lateinit var urlLayout: TextInputLayout
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelEditButton: MaterialButton
    private var editingIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        volumeControlStream = AudioManager.STREAM_MUSIC

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        store = StationStore(this)
        nameInput = findViewById(R.id.nameInput)
        urlInput = findViewById(R.id.urlInput)
        urlLayout = findViewById(R.id.urlLayout)
        saveButton = findViewById(R.id.saveButton)
        cancelEditButton = findViewById(R.id.cancelEditButton)

        adapter = StationAdapter(this, store.getStations(), store.lastUrl)
        val list = findViewById<ListView>(R.id.stationList)
        list.adapter = adapter
        list.emptyView = findViewById(R.id.emptyText)
        list.setOnItemClickListener { _, _, position, _ -> showStationOptions(position) }

        saveButton.setOnClickListener { saveStation() }
        cancelEditButton.setOnClickListener { resetForm() }
    }

    private fun saveStation() {
        var url = urlInput.text?.toString()?.trim().orEmpty()
        var name = nameInput.text?.toString()?.trim().orEmpty()

        if (url.isEmpty()) {
            urlLayout.error = getString(R.string.error_url_empty)
            return
        }
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
            url = "http://$url"
        }
        if (android.util.Patterns.WEB_URL.matcher(url).matches().not()) {
            urlLayout.error = getString(R.string.error_url_invalid)
            return
        }
        urlLayout.error = null
        if (name.isEmpty()) name = android.net.Uri.parse(url).host ?: url

        val station = Station(name, url)
        if (editingIndex >= 0) {
            store.update(editingIndex, station)
            Toast.makeText(this, R.string.station_updated, Toast.LENGTH_SHORT).show()
        } else {
            if (!store.add(station)) {
                urlLayout.error = getString(R.string.error_url_exists)
                return
            }
            Toast.makeText(this, R.string.station_added, Toast.LENGTH_SHORT).show()
        }
        resetForm()
        refresh()
    }

    private fun showStationOptions(position: Int) {
        val station = adapter.getItem(position)
        val options = arrayOf(
            getString(R.string.option_play),
            getString(R.string.option_edit),
            getString(R.string.option_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(station.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        store.lastUrl = station.url
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    1 -> {
                        editingIndex = position
                        nameInput.setText(station.name)
                        urlInput.setText(station.url)
                        saveButton.setText(R.string.save_changes)
                        cancelEditButton.visibility = android.view.View.VISIBLE
                        nameInput.requestFocus()
                    }
                    2 -> confirmDelete(position, station)
                }
            }
            .show()
    }

    private fun confirmDelete(position: Int, station: Station) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, station.name))
            .setPositiveButton(R.string.option_delete) { _, _ ->
                store.remove(position)
                if (editingIndex == position) resetForm()
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resetForm() {
        editingIndex = -1
        nameInput.setText("")
        urlInput.setText("")
        urlLayout.error = null
        saveButton.setText(R.string.add_station)
        cancelEditButton.visibility = android.view.View.GONE
    }

    private fun refresh() {
        adapter.stations = store.getStations()
        adapter.selectedUrl = store.lastUrl
        adapter.notifyDataSetChanged()
    }
}
