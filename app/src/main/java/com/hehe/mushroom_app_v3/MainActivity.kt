package com.hehe.mushroom_app_v3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.hehe.mushroom_app_v3.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private val useYolo = true
    private lateinit var binding: ActivityMainBinding
    private lateinit var yoloDetector: YoloDetector
    private lateinit var resNetClassifier: ResNetClassifier

    private var currentBitmap: Bitmap? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startActivity(Intent(this, CameraActivity::class.java))
            else showError("Camera permission denied")
        }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            if (bitmap != null) processImage(bitmap, box = null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        yoloDetector     = YoloDetector(this)
        resNetClassifier = ResNetClassifier(this)

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        val imagePath = intent.getStringExtra(CameraActivity.EXTRA_IMAGE_PATH) ?: return

        val file = File(imagePath)
        if (!file.exists()) return
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return
        val box: RectF? = if (useYolo && intent.hasExtra("box_left")) {
            RectF(
                intent.getFloatExtra("box_left",   0f),
                intent.getFloatExtra("box_top",    0f),
                intent.getFloatExtra("box_right",  0f),
                intent.getFloatExtra("box_bottom", 0f)
            )
        } else null

        processImage(bitmap, box)
    }

    private fun processImage(bitmap: Bitmap, box: RectF?) {
        currentBitmap = bitmap

        binding.ivOriginal.setImageBitmap(bitmap)
        binding.layoutEmptyState.visibility  = View.GONE
        binding.boundingBoxOverlay.clear()
        binding.ivCropped.visibility         = View.GONE
        binding.layoutCropEmpty.visibility   = View.GONE
        binding.tvNoDetection.visibility     = View.GONE
        binding.layoutResult.visibility      = View.GONE
        binding.layoutError.visibility       = View.GONE
        binding.layoutLoading.visibility     = View.VISIBLE

        lifecycleScope.launch {
            try {
                val (detection, classification) = withContext(Dispatchers.Default) {

                    val det: DetectionResult? = if (useYolo) {
                        if (box != null) {
                            DetectionResult(box, intent.getFloatExtra("box_conf", 1f))
                        } else {
                            yoloDetector.detect(bitmap)
                        }
                    } else {
                        null
                    }
                    val crop = if (useYolo && det != null) {
                        val b  = det.boundingBox
                        val x  = b.left.toInt().coerceAtLeast(0)
                        val y  = b.top.toInt().coerceAtLeast(0)
                        val w  = b.width().toInt().coerceAtMost(bitmap.width - x)
                        val h  = b.height().toInt().coerceAtMost(bitmap.height - y)
                        if (w > 0 && h > 0) Bitmap.createBitmap(bitmap, x, y, w, h) else bitmap
                    } else {
                        bitmap
                    }

                    if (useYolo && det == null) {
                        return@withContext Pair(
                            null,
                            ClassificationResult(
                                binaryLabel  = "UNKNOWN",
                                binaryConf   = 0f,
                                speciesLabel = "Tidak_Dikenali",
                                speciesConf  = 0f
                            )
                        )
                    }

                    val cls = resNetClassifier.classify(crop)
                    if (crop != bitmap) crop.recycle()

                    Pair(det, cls)
                }

                withContext(Dispatchers.Main) {
                    binding.layoutLoading.visibility = View.GONE
                    showResult(bitmap, detection?.boundingBox, classification)
                    if (useYolo && detection == null) showNotDetectedDialog()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.layoutLoading.visibility = View.GONE
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showResult(bitmap: Bitmap, box: RectF?, result: ClassificationResult) {
        val isPoison  = result.binaryLabel == "POISONOUS"
        val isUnknown = result.binaryLabel == "UNKNOWN"
        val statusColor = when {
            isUnknown -> "#888880"
            isPoison  -> "#9B3030"
            else      -> "#6B7260"
        }
        if (useYolo && box != null) {
            binding.boundingBoxOverlay.setBox(
                box, "MUSHROOM", false,
                bitmap.width.toFloat(), bitmap.height.toFloat()
            )
            val x = box.left.toInt().coerceAtLeast(0)
            val y = box.top.toInt().coerceAtLeast(0)
            val w = box.width().toInt().coerceAtMost(bitmap.width - x)
            val h = box.height().toInt().coerceAtMost(bitmap.height - y)
            if (w > 0 && h > 0) {
                val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                binding.ivCropped.setImageBitmap(cropped)
                binding.ivCropped.visibility = View.VISIBLE
            }
        } else {
            binding.tvNoDetection.visibility = View.GONE
        }

        binding.layoutResult.visibility = View.VISIBLE
        binding.tvBinaryLabel.text = when {
            isUnknown -> "UNKNOWN"
            isPoison  -> "POISONOUS"
            else      -> "EDIBLE"
        }
        binding.tvBinaryLabel.setTextColor(Color.parseColor(statusColor))
        binding.tvBinaryConf.text = if (isUnknown) "—" else "${(result.binaryConf * 100).toInt()}%"
        binding.tvBinaryConf.setTextColor(Color.parseColor(statusColor))
        binding.tvStatusIcon.text = when {
            isUnknown -> "❓"
            isPoison  -> "☠️"
            else      -> "✅"
        }
        binding.tvBinaryDesc.text = when {
            isUnknown -> "Confidence is too low (<70%)"
            isPoison  -> "Do not consume!"
            else      -> "Safe to consume"
        }

        binding.tvSpecies.text = result.speciesLabel.replace("_", " ")
        val speciesPercent = (result.speciesConf * 100).toInt()
        binding.progressSpecies.progress = speciesPercent
        binding.tvSpeciesConf.text = "$speciesPercent%"
        binding.tvSpeciesConf.setTextColor(Color.parseColor(statusColor))

        binding.layoutCharacteristics.removeAllViews()
        if (isUnknown) {
            val tv = android.widget.TextView(this)
            tv.text     = "Mushroom could not be identified.\nPlease take a clearer and closer photo."
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#6B6B62"))
            tv.setLineSpacing(4f, 1f)
            binding.layoutCharacteristics.addView(tv)
        } else {
            val info = MushroomData.getInfo(result.speciesLabel)
            if (info != null) {
                info.characteristics.forEachIndexed { index, ciri ->
                    val tv = android.widget.TextView(this)
                    tv.text     = "${index + 1}. $ciri"
                    tv.textSize = 12f
                    tv.setTextColor(Color.parseColor("#2C2C2C"))
                    tv.setPadding(0, 0, 0, 10)
                    tv.setLineSpacing(4f, 1f)
                    binding.layoutCharacteristics.addView(tv)
                }
            } else {
                val tv = android.widget.TextView(this)
                tv.text     = "Informasi ciri-ciri belum tersedia"
                tv.textSize = 12f
                tv.setTextColor(Color.parseColor("#6B6B62"))
                binding.layoutCharacteristics.addView(tv)
            }
        }
    }

    private fun showNotDetectedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🍄 Mushroom Not Detected")
            .setMessage(
                "The selected image does not contain a detectable mushroom.\n\n" +
                        "Tips for better detection:\n" +
                        "• Make sure the mushroom is clearly visible.\n" +
                        "• Take the photo from a closer distance.\n" +
                        "• Avoid cluttered backgrounds.\n" +
                        "• Ensure there is sufficient lighting."
            )
            .setPositiveButton("📷 Camera") { _, _ ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) startActivity(Intent(this, CameraActivity::class.java))
                else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("🖼 Choose from Gallery") { _, _ ->
                galleryLauncher.launch("image/*")
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showError(msg: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility   = View.VISIBLE
        binding.tvError.text = msg
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val exifStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            showError("Failed to read image: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        yoloDetector.close()
        resNetClassifier.close()
    }
}