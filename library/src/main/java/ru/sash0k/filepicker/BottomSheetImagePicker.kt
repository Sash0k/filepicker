package ru.sash0k.filepicker

import android.Manifest
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.util.*

class BottomSheetImagePicker internal constructor() :
    BottomSheetDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private var currentPhotoUri: Uri? = null

    private var isMultiSelect = false
    private var multiSelectMin = 1
    private var multiSelectMax = Int.MAX_VALUE

    private var requestTag = ""

    private var showCameraTile = false
    private var showCameraButton = false
    private var showStorageButton = false
    private var storageMimetypes = emptyArray<String>()

    @StringRes private var resTitleSingle = R.string.imagePickerSingle
    @StringRes private var resTitleMulti = R.string.imagePickerMulti
    @StringRes private var loadingRes = R.string.imagePickerLoading
    @StringRes private var emptyRes = R.string.imagePickerEmpty

    @DimenRes private var peekHeight = R.dimen.imagePickerPeekHeight
    @DimenRes private var columnSizeRes = R.dimen.imagePickerColumnSize

    private var onImagesSelectedListener: OnImagesSelectedListener? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var progressbar: ProgressBar
    private lateinit var emptyView: TextView

    private lateinit var tvHeader: TextView
    private lateinit var btnDone: TextView
    private lateinit var btnClear: ImageButton
    private lateinit var btnCamera: ImageButton
    private lateinit var btnStorage: ImageButton

    private val adapter by lazy {
        ImageTileAdapter(
            isMultiSelect,
            showCameraTile,
            ::tileClick,
            ::selectionCountChanged
        )
    }

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialog

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnImagesSelectedListener) {
            onImagesSelectedListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadArguments()
        if (savedInstanceState != null) {
            currentPhotoUri = savedInstanceState.getParcelable(STATE_CURRENT_URI)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.imagepicker, container, false).also {
            (parentFragment as? OnImagesSelectedListener)?.let { onImagesSelectedListener = it }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressbar = view.findViewById(R.id.progress)
        emptyView = view.findViewById(R.id.tvEmpty)
        btnClear = view.findViewById(R.id.btnClear)
        btnDone = view.findViewById(R.id.btnDone)
        tvHeader = view.findViewById(R.id.tvHeader)
        btnCamera = view.findViewById(R.id.btnCamera)
        btnStorage = view.findViewById(R.id.btnStorage)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)

        if (requireContext().hasReadStoragePermission) {
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        } else permissionStorage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

        tvHeader.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                recycler.smoothScrollToPosition(0)
            }
        }

        emptyView.setText(loadingRes)
        btnCamera.setOnClickListener { launchCamera() }
        btnStorage.setOnClickListener { actionOpenDocuments.launch(storageMimetypes) }
        btnClear.setOnClickListener {
            adapter.clear()
            selectionCountChanged(adapter.selection.size)
        }
        btnDone.setOnClickListener {
            onImagesSelectedListener?.onImagesSelected(adapter.getSelectedImages(), requestTag)
            dismissAllowingStateLoss()
        }

        recycler.layoutManager = AutofitLayoutManager(requireContext(), columnSizeRes)
        (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        recycler.adapter = adapter

        val oldSelection = savedInstanceState?.getIntArray(STATE_SELECTION)
        if (oldSelection != null) {
            adapter.selection = oldSelection.toHashSet()
        }
        selectionCountChanged(adapter.selection.size)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(R.id.design_bottom_sheet)
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.peekHeight = activity?.resources?.getDimensionPixelSize(peekHeight) ?: 400
                bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
            }
        }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                view?.alpha = if (slideOffset < 0f) 1f + slideOffset else 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    private fun tileClick(tile: ClickedTile) {
        when (tile) {
            is ClickedTile.CameraTile -> { launchCamera() }
            is ClickedTile.ImageTile -> {
                onImagesSelectedListener?.onImagesSelected(listOf(tile.uri), requestTag)
                dismissAllowingStateLoss()
            }
        }
    }

    private fun selectionCountChanged(count: Int) {
        if (count == 0) {
            btnStorage.isVisible = showStorageButton
            btnCamera.isVisible = showCameraButton

            tvHeader.setText(resTitleSingle)
            btnDone.isVisible = false
            btnClear.isVisible = false
        } else {
            btnStorage.isVisible = false
            btnCamera.isVisible = false

            tvHeader.text = getString(resTitleMulti, count)
            btnDone.isVisible = true
            btnClear.isVisible = true
        }
    }

    private val permissionStorage = registerForActivityResult(RequestPermission()) { success ->
        if (success) LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        else dismissAllowingStateLoss()
    }

    private val permissionCamera = registerForActivityResult(RequestPermission()) { success ->
        if (success) launchCamera()
    }

    private val actionOpenDocuments = registerForActivityResult(OpenMultipleDocuments()) { uris ->
        onImagesSelectedListener?.onImagesSelected(uris, requestTag)
        dismissAllowingStateLoss()
    }

    private val actionTakePicture = registerForActivityResult(TakePicture()) { success ->
        if (success) currentPhotoUri?.let { uri ->  onImagesSelectedListener?.onImagesSelected(listOf(uri), requestTag) }
        dismissAllowingStateLoss()
    }

    private fun getTmpFileUri(): Uri {
        val context = requireContext()
        val tmpFile = File.createTempFile("filepicker", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(context, Configuration.authority, tmpFile)
    }

    private fun launchCamera() {
        if (!requireContext().hasCameraPermission) {
            permissionCamera.launch(Manifest.permission.CAMERA)
            return
        }

        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                currentPhotoUri = uri
                actionTakePicture.launch(uri)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_CURRENT_URI, currentPhotoUri)
        outState.putIntArray(STATE_SELECTION, adapter.selection.toIntArray())
    }

    private fun loadArguments() {
        val args = arguments ?: return
        val hasCamera = activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ?: false

        isMultiSelect = args.getBoolean(KEY_MULTI_SELECT, isMultiSelect)
        multiSelectMin = args.getInt(KEY_MULTI_SELECT_MIN, multiSelectMin)
        multiSelectMax = args.getInt(KEY_MULTI_SELECT_MAX, multiSelectMax)
        showCameraTile = hasCamera && args.getBoolean(KEY_SHOW_CAMERA_TILE, showCameraTile)
        showCameraButton = hasCamera && args.getBoolean(KEY_SHOW_CAMERA_BTN, showCameraButton)
        showStorageButton = args.getBoolean(KEY_SHOW_STORAGE_BTN, showStorageButton)
        storageMimetypes = args.getStringArray(KEY_STORAGE_MIMETYPES) ?: storageMimetypes
        columnSizeRes = args.getInt(KEY_COLUMN_SIZE_RES, columnSizeRes)
        requestTag = args.getString(KEY_REQUEST_TAG, requestTag)

        resTitleSingle = args.getInt(KEY_TITLE_RES_SINGLE, resTitleSingle)
        resTitleMulti = args.getInt(KEY_TITLE_RES_MULTI, resTitleMulti)

        peekHeight = args.getInt(KEY_PEEK_HEIGHT, peekHeight)

        emptyRes = args.getInt(KEY_TEXT_EMPTY, emptyRes)
        loadingRes = args.getInt(KEY_TEXT_LOADING, loadingRes)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id != LOADER_ID) throw IllegalStateException("illegal loader id: $id")
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"
        return CursorLoader(requireContext(), uri, projection, null, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        progressbar.isVisible = false
        emptyView.setText(emptyRes)
        data ?: return

        val columnIndex = data.getColumnIndex(MediaStore.Images.Media._ID)
        val items = ArrayList<Uri>()
        while (items.size < MAX_CURSOR_IMAGES && data.moveToNext()) {
            val id = data.getLong(columnIndex)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            items.add(contentUri)
        }
        data.moveToFirst()
        adapter.imageList = items
        emptyView.isVisible = items.size == 0
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.imageList = emptyList()
    }

    interface OnImagesSelectedListener {
        fun onImagesSelected(uris: List<Uri>, tag: String?)
    }

    companion object {
        private const val TAG = "BottomSheetImagePicker"
        private const val LOADER_ID = 0x1337
        private const val KEY_REQUEST_TAG = "requestTag"

        private const val KEY_MULTI_SELECT = "multiSelect"
        private const val KEY_MULTI_SELECT_MIN = "multiSelectMin"
        private const val KEY_MULTI_SELECT_MAX = "multiSelectMax"
        private const val KEY_SHOW_CAMERA_TILE = "showCameraTile"
        private const val KEY_SHOW_CAMERA_BTN = "showCameraButton"
        private const val KEY_SHOW_STORAGE_BTN = "showStorageButton"
        private const val KEY_STORAGE_MIMETYPES = "storageMimetypes"
        private const val KEY_COLUMN_SIZE_RES = "columnCount"

        private const val KEY_TITLE_RES_SINGLE = "titleResSingle"
        private const val KEY_TITLE_RES_MULTI = "titleResMulti"
        private const val KEY_TEXT_EMPTY = "emptyText"
        private const val KEY_TEXT_LOADING = "loadingText"

        private const val KEY_PEEK_HEIGHT = "peekHeight"

        private const val STATE_CURRENT_URI = "stateUri"
        private const val STATE_SELECTION = "stateSelection"

        private const val MAX_CURSOR_IMAGES = 512
    }

    class Builder {
        private val args = Bundle()

        fun requestTag(requestTag: String) = args.run {
            putString(KEY_REQUEST_TAG, requestTag)
            this@Builder
        }

        fun multiSelect(min: Int = 1, max: Int = Int.MAX_VALUE) = args.run {
            putBoolean(KEY_MULTI_SELECT, true)
            putInt(KEY_MULTI_SELECT_MIN, min)
            putInt(KEY_MULTI_SELECT_MAX, max)
            this@Builder
        }

        fun columnSize(@DimenRes columnSizeRes: Int) = args.run {
            putInt(KEY_COLUMN_SIZE_RES, columnSizeRes)
            this@Builder
        }

        fun cameraButton(enabled: Boolean) = args.run {
            putBoolean(KEY_SHOW_CAMERA_BTN, enabled)
            putBoolean(KEY_SHOW_CAMERA_TILE, enabled)
            this@Builder
        }

        fun storageButton(mimeTypes: Array<String>) = args.run {
            if (mimeTypes.isEmpty()) {
                putBoolean(KEY_SHOW_STORAGE_BTN, false)
            } else {
                putBoolean(KEY_SHOW_STORAGE_BTN, true)
                putStringArray(KEY_STORAGE_MIMETYPES, mimeTypes)
            }
            this@Builder
        }

        fun singleSelectTitle(@StringRes titleRes: Int) = args.run {
            putInt(KEY_TITLE_RES_SINGLE, titleRes)
            this@Builder
        }

        fun peekHeight(@DimenRes peekHeightRes: Int) = args.run {
            putInt(KEY_PEEK_HEIGHT, peekHeightRes)
            this@Builder
        }

        fun emptyText(@StringRes emptyRes: Int) = args.run {
            putInt(KEY_TEXT_EMPTY, emptyRes)
            this@Builder
        }

        fun loadingText(@StringRes loadingRes: Int) = args.run {
            putInt(KEY_TEXT_LOADING, loadingRes)
            this@Builder
        }

        fun multiSelectTitles(@StringRes titleCount: Int) = args.run {
            putInt(KEY_TITLE_RES_MULTI, titleCount)
            this@Builder
        }

        fun show(fm: FragmentManager, tag: String? = null) = BottomSheetImagePicker()
            .apply { arguments = args }
            .show(fm, tag)
    }
}
