package ru.sash0k.filepicker.demo.loaders

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import coil.load
import ru.sash0k.filepicker.ImageLoader

class CoilImageLoader: ImageLoader {
    override fun loadImage(context: Context, view: ImageView, uri: Uri) {
        view.load(uri)
    }
}