package ru.sash0k.filepicker.demo.loaders

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.sash0k.filepicker.ImageLoader
import ru.sash0k.filepicker.demo.R

class GlideImageLoader: ImageLoader {

    override fun loadImage(context: Context, view: ImageView, uri: Uri) {
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .placeholder(R.drawable.ic_baseline_image)
            .centerCrop()
            .into(view)
    }
}