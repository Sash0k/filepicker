package ru.sash0k.filepicker

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

internal class ImageTileAdapter(
    private val isMultiSelect: Boolean,
    private val showCameraTile: Boolean,
    private val clickListener: (ClickedTile) -> Unit,
    private val selectionCountChanged: (Int) -> Unit
) : RecyclerView.Adapter<VHImageTileBase>() {

    var imageList: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selection = HashSet<Int>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getSelectedImages(): ArrayList<Uri> = ArrayList<Uri>(selection.size).apply {
        selection.forEach { add(imageList.getOrNull(it) ?: return@forEach) }
    }

    fun clear() {
        selection = HashSet()
        selectionCountChanged(0)
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> VT_CAMERA
        else -> VT_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHImageTileBase =
        LayoutInflater.from(parent.context).inflate(if (viewType == VT_IMAGE) R.layout.tile_image else R.layout.tile_special, parent, false).let { view ->
            when (viewType) {
                VT_IMAGE -> VHImageTileBase.VHImageTile(view)
                VT_CAMERA -> VHImageTileBase.VHCameraTile(view) { clickListener.invoke(ClickedTile.CameraTile) }
                else -> throw IllegalStateException("viewType $viewType not allowed")
            }
        }

    override fun getItemCount(): Int = imageList.size + showCameraTile.toInt()

    override fun onBindViewHolder(holder: VHImageTileBase, position: Int) {
        val pos = getPosition(position)
        (holder as? VHImageTileBase.VHImageTile)?.update(
            imageList[pos],
            isMultiSelect,
            selection.contains(pos),
            ::onImageTileClick,
            ::onLongTileClick
        )
    }

    private fun onImageTileClick(selectView: View, position: Int) {
        if (position == 0) return
        val pos = getPosition(position)

        if (selection.size == 0) clickListener.invoke(ClickedTile.ImageTile(imageList[pos]))
        else multiselect(selectView, pos)
    }

    private fun onLongTileClick(selectView: View, position: Int) {
        val pos = getPosition(position)
        multiselect(selectView, pos)
    }

    private fun multiselect(selectView: View, pos: Int) {
        val checkBox = selectView as? CheckBox
        if (selection.contains(pos)) {
            checkBox?.isChecked = false
            selection.remove(pos)
        } else {
            checkBox?.isChecked = true
            selection.add(pos)
        }
        selectionCountChanged.invoke(selection.size)
    }

    private fun getPosition(position: Int): Int = position - showCameraTile.toInt()

    private fun Boolean.toInt() = if (this) 1 else 0

    companion object {
        private const val VT_CAMERA = 0x1000
        private const val VT_IMAGE = 0x1002
    }
}

sealed class ClickedTile {
    object CameraTile : ClickedTile()
    data class ImageTile(val uri: Uri) : ClickedTile()
}

sealed class VHImageTileBase(
    view: View
) : RecyclerView.ViewHolder(view) {

    class VHImageTile(view: View) : VHImageTileBase(view) {
        private val context = view.context
        private val ivImage = view.findViewById<ImageView>(R.id.ivImage)
        private val ivSelect = view.findViewById<View>(R.id.ivSelect)

        private var clickListener: ((selectView: View, position: Int) -> Unit)? = null
        private var longClickListener: ((selectView: View, position: Int) -> Unit)? = null

        init {
            view.setOnClickListener { clickListener?.invoke(ivSelect, adapterPosition) }
            view.setOnLongClickListener { longClickListener?.invoke(ivSelect, adapterPosition)
                return@setOnLongClickListener true
            }
        }

        fun update(
            uri: Uri,
            multiselect: Boolean,
            selected: Boolean,
            clickListener: (selectView: View, position: Int) -> Unit,
            longClickListener: (selectView: View, position: Int) -> Unit,
        ) {
            this.clickListener = clickListener
            this.longClickListener = if (multiselect) longClickListener else null

            Configuration.imageLoader.loadImage(context, ivImage, uri)
            (ivSelect as? CheckBox)?.apply {
                isVisible = multiselect
                isChecked = selected
            }
        }
    }

    class VHCameraTile(view: View, clickListener: () -> Unit) : VHImageTileBase(view) {
        init {
            view.setOnClickListener { clickListener.invoke() }
            view.findViewById<ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_baseline_camera_alt_36)
        }
    }
}