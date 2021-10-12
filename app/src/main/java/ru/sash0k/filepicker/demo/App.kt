package ru.sash0k.filepicker.demo

import android.app.Application
import ru.sash0k.filepicker.FilePicker
import ru.sash0k.filepicker.demo.loaders.CoilImageLoader
import ru.sash0k.filepicker.demo.loaders.GlideImageLoader

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        FilePicker.init(
            loader = GlideImageLoader(), //CoilImageLoader(),
            authority = "${BuildConfig.APPLICATION_ID}.provider"
        )
    }
}