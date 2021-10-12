package ru.sash0k.filepicker

internal object Configuration {
    private var mImageLoader: ImageLoader? = null
    private var mAuthority: String? = null

    fun setup(imageLoader: ImageLoader, authority: String?) {
        mImageLoader = imageLoader
        mAuthority = authority
    }

    private const val exception = "is null. You probably forget to call FilePicker.init()"

    val imageLoader: ImageLoader get() = mImageLoader ?: throw IllegalStateException("ImageLoader $exception")
    val authority: String get() = mAuthority ?: throw IllegalStateException("Authority $exception")
}