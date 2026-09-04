package com.zen.fnftools

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zen.fnftools.util.NamedBitmap

class FrameAdapter(
    private val items: MutableList<NamedBitmap>
) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

    private val selected = mutableSetOf<Int>()

    /** Splits a trailing run of digits off a name, e.g. "anim0007" -> ("anim", "0007"). */
    private fun splitName(name: String): Pair<String, String> {
        val match = Regex("^(.*?)(\\d+)$").find(name)
        return if (match != null) {
            match.groupValues[1] to match.groupValues[2]
        } else {
            name to ""
        }
    }

    inner class FrameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val tvIndex: TextView = view.findViewById(R.id.tvIndex)
        val ivThumb: ImageView = view.findViewById(R.id.ivThumb)
        val etFramePrefix: EditText = view.findViewById(R.id.etFramePrefix)
        val tvFrameSuffix: TextView = view.findViewById(R.id.tvFrameSuffix)
        var watcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_frame, parent, false)
        return FrameViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        val item = items[position]
        val (prefix, suffix) = splitName(item.name)

        holder.tvIndex.text = position.toString().padStart(2, '0')
        holder.ivThumb.setImageBitmap(item.bitmap)
        holder.tvFrameSuffix.text = suffix

        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = selected.contains(position)
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
            if (isChecked) selected.add(pos) else selected.remove(pos)
        }

        holder.watcher?.let { holder.etFramePrefix.removeTextChangedListener(it) }
        holder.etFramePrefix.setText(prefix)
        holder.etFramePrefix.setSelection(holder.etFramePrefix.text?.length ?: 0)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val (_, existingSuffix) = splitName(items[pos].name)
                items[pos] = NamedBitmap((s?.toString() ?: "") + existingSuffix, items[pos].bitmap)
            }
        }
        holder.etFramePrefix.addTextChangedListener(watcher)
        holder.watcher = watcher
    }

    override fun getItemCount(): Int = items.size

    fun selectAll() {
        selected.clear()
        selected.addAll(items.indices)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    fun selectedCount(): Int = selected.size

    /** Sets the editable prefix on every selected row, keeping each row's own numeric suffix. */
    fun applyPrefixToSelected(newPrefix: String) {
        for (pos in selected) {
            if (pos !in items.indices) continue
            val (_, suffix) = splitName(items[pos].name)
            items[pos] = NamedBitmap(newPrefix + suffix, items[pos].bitmap)
        }
        notifyDataSetChanged()
    }
}
