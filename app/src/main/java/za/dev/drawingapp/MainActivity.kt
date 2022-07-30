package za.dev.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {


    private var drawingView: DrawingView? = null
    private var mImageButtonCurrPaint: ImageButton? = null
    var customProgressDialog: Dialog? =null
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null)
            {
                val imageBackground: ImageView = findViewById(R.id.iv_background)

                imageBackground.setImageURI(result.data?.data)
            }
        }


    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted)
                {
                    Toast.makeText(this@MainActivity, "Permission granted to read storage files", Toast.LENGTH_SHORT).show()

                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else
                {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE)
                    {
                        Toast.makeText(this@MainActivity,"OOPS you just Denied the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set brush size
        drawingView = findViewById(R.id.DrawingView)
        drawingView?.setSizeBrush(20.toFloat())

        val linearLayoutPaintColor = findViewById<LinearLayout>(R.id.ll_paint_color)


        mImageButtonCurrPaint = linearLayoutPaintColor[1] as ImageButton
        mImageButtonCurrPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ib_brush:ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {

            requestStoragePermission()

        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {

            if (isReadStorageAllowed())
            {
                showProgressDialog()
                lifecycleScope.launch {

                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeBrush(5.toFloat())
            brushDialog.dismiss()
        })


        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeBrush(20.toFloat())
            brushDialog.dismiss()
        })


        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }

    fun paintClicked(view: View)
    {
        if (view !== mImageButtonCurrPaint)
        {
            val imgBtn = view as ImageButton
            val colorTag = imgBtn.tag.toString()
            drawingView?.setColor(colorTag)

            imgBtn.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

            mImageButtonCurrPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal))

            mImageButtonCurrPaint = view

        }
    }


    private fun isReadStorageAllowed(): Boolean
    {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun requestStoragePermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            showRationaleDialog("Kids Drawing App", "Kids Drawing App"+
                    "needs to access your external storage")
        }
        else
        {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    }

    private fun showRationaleDialog(title: String, message: String)
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _-> dialog.dismiss()}
        builder.create().show()
    }


    private fun getBitmapFromView(view: View) : Bitmap
    {
        val returnbitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val Canvas  = Canvas(returnbitmap)
        val bgDrawable = view.background
        if (bgDrawable != null)
        {
            bgDrawable.draw(Canvas)
        }
        else
        {
            Canvas.drawColor(Color.WHITE)
        }

        view.draw(Canvas)

        return returnbitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String
    {
        var result = ""
        withContext(Dispatchers.IO)
        {
            if (mBitmap != null)
            {
                try {
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val file = File(externalCacheDir?.absoluteFile.toString() +
                    File.separator + "DrawingApp_" + System.currentTimeMillis() / 1000 + " .png")


                    val FileOuput = FileOutputStream(file)
                    FileOuput.write(bytes.toByteArray())
                    FileOuput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (!result.isEmpty())
                        {
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            ShareImage(result)
                        }
                        else
                        {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception)
                {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun showProgressDialog()
    {
        customProgressDialog= Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog()
    {
        if (customProgressDialog != null)
        {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun ShareImage(result: String)
    {
        MediaScannerConnection.scanFile(this, arrayOf(result), null)
        {
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "Image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}