package ru.sash0k.filepicker

object FilePicker {

   /**
    * One time setup for required configurations.
    * Should be called from Application's class onCreate
    *
    * @param loader - ImageLoader implementation
    * @param authority - FileProvider's authority. Is necessary if you allow picker to use Camera,
    *                    so it can store temporary files. Could be null otherwise
    *
    * Will throw exception in two cases:
    *       If ImageLoader was not initialized
    *       If Authority was null while accessing camera
    */
    fun init(loader: ImageLoader, authority: String? = null) = Configuration.setup(loader, authority)
}