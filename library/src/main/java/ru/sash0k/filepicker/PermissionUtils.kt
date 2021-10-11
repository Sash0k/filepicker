package ru.sash0k.filepicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal val Context.hasReadStoragePermission get() = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
internal val Context.hasCameraPermission get() = isPermissionGranted(Manifest.permission.CAMERA)