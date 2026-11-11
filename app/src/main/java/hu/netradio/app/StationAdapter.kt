package hu.netradio.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class StationAdapter(
    private val context: Context,
    var stations: List<Station>,
    var selectedUrl: String? = null
) : BaseAdapter() {

    override fun getCount() = stations.size
    override fun getItem(position: Int) = stations[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_station, parent, false)
        val st = stations[position]
        view.findViewById<TextView>(R.id.stationName).text = st.name
        view.findViewById<TextView>(R.id.stationUrl).text = st.url
        view.findViewById<ImageView>(R.id.stationActive).visibility =
            if (st.url == selectedUrl) View.VISIBLE else View.INVISIBLE
        return view
    }
}
