package hu.netradio.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Station(val name: String, val url: String)

/** Rádióállomások és az utoljára hallgatott állomás tárolása (SharedPreferences, JSON). */
class StationStore(context: Context) {

    private val prefs = context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)

    fun getStations(): MutableList<Station> {
        val json = prefs.getString(KEY_STATIONS, null)
        if (json == null) {
            // Első indítás: alapértelmezett állomás(ok)
            val defaults = DEFAULT_STATIONS.toMutableList()
            save(defaults)
            return defaults
        }
        val list = mutableListOf<Station>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(Station(o.getString("name"), o.getString("url")))
            }
        } catch (_: Exception) {
            return DEFAULT_STATIONS.toMutableList()
        }
        return list
    }

    fun save(list: List<Station>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
        prefs.edit().putString(KEY_STATIONS, arr.toString()).apply()
    }

    /** @return false, ha ez a link már szerepel a listában */
    fun add(station: Station): Boolean {
        val list = getStations()
        if (list.any { it.url == station.url }) return false
        list.add(station)
        save(list)
        return true
    }

    fun update(index: Int, station: Station) {
        val list = getStations()
        if (index !in list.indices) return
        val old = list[index]
        list[index] = station
        save(list)
        if (lastUrl == old.url) lastUrl = station.url
    }

    fun remove(index: Int) {
        val list = getStations()
        if (index !in list.indices) return
        list.removeAt(index)
        save(list)
    }

    var lastUrl: String?
        get() = prefs.getString(KEY_LAST_URL, null)
        set(value) = prefs.edit().putString(KEY_LAST_URL, value).apply()

    /** A legutóbb beállított állomás, vagy ha nincs, a lista első eleme. */
    fun lastStation(): Station? {
        val list = getStations()
        return list.firstOrNull { it.url == lastUrl } ?: list.firstOrNull()
    }

    companion object {
        private const val KEY_STATIONS = "stations"
        private const val KEY_LAST_URL = "last_url"

        val DEFAULT_STATIONS = listOf(
            Station("Rádió 1", "https://icast.connectmedia.hu/5201/live.mp3")
        )
    }
}
