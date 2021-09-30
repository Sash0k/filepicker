package ru.sash0k.filepicker.demo

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import ru.sash0k.filepicker.BottomSheetImagePicker
import ru.sash0k.filepicker.ButtonType
import ru.sash0k.filepicker.demo.databinding.FragmentDemoBinding

class DemoFragment : Fragment(), BottomSheetImagePicker.OnImagesSelectedListener {

    private var _binding: FragmentDemoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonFirst.setOnClickListener { pickFile("example") }
    }

    private fun pickFile(tag: String) {
        BottomSheetImagePicker.Builder("imagepicker.demo")
            .cameraButton(ButtonType.Tile)
            .galleryButton(ButtonType.Button)
            .singleSelectTitle(R.string.imagePickerSingle)
            .peekHeight(R.dimen.peekHeight)
            .requestTag(tag)
            .show(childFragmentManager)
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        val view = when (tag) {
            "example" -> binding.firstImage
            else -> null
        }

        if (view != null) {
            uris.firstOrNull()?.let { Glide.with(this).load(it).into(view) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}