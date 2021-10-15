package ru.sash0k.filepicker.demo

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.sash0k.filepicker.BottomSheetImagePicker
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
        binding.buttonPick.setOnClickListener { pickFile("example") }
    }

    private fun pickFile(tag: String) {
        BottomSheetImagePicker.Builder()
            .cameraButton(enabled = true)
            .storageButton(mimeTypes = arrayOf("image/*", "application/pdf", "text/plain"))
            .singleSelectTitle(R.string.imagePickerSingle)
            .peekHeight(R.dimen.peekHeight)
            .requestTag(tag)
            .show(childFragmentManager)
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        binding.firstImage.setImageResource(0)
        binding.secondImage.setImageResource(0)

        uris.firstOrNull()?.let { load(it, binding.firstImage) }
        if (uris.size > 1) load(uris[1], binding.secondImage)
    }

    private fun load(uri: Uri, view: ImageView) = Glide.with(this).load(uri).error(R.drawable.ic_frame_pdf).into(view)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}