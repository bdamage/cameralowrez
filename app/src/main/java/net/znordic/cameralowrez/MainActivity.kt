package net.znordic.cameralowrez

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : RemoteIntentConfig.RemoteConfigListener, AppCompatActivity() {

    private var TAG: String = "CUSTOMCAMERA"
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var mRemoteConfig : RemoteIntentConfig? = null

    var mPictureWidth : Int = 1440;
    var mPictureHeight: Int = 1920;
    var mPictureQuality: Int = 80;
    var mPictureUseAspectRatio : Boolean = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)

        if(ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 500)

        }

        if(ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 500)
        }

        if(ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 500)
        }


        mRemoteConfig = RemoteIntentConfig(this)

        // Create an instance of Camera
        mCamera = getCameraInstance()

        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview: FrameLayout = findViewById(R.id.camera_preview)
            preview.addView(it)
        }
        val captureButton: Button = findViewById(R.id.button_capture)


        captureButton.background =  getDrawable(R.drawable.ic_record_alt)

        captureButton.setOnClickListener {
            // get an image from the camera
            mCamera?.takePicture(null, null, mPicture)
        }

        val flashButton: Button = findViewById(R.id.buttonFlash)

        flashButton.foreground = getDrawable(R.drawable.ic_auto_flash)

        flashButton.setOnClickListener {

            val params: Camera.Parameters? = mCamera?.parameters

            Log.d(TAG, "flash mode: " + params!!.flashMode)
            if(params!!.flashMode != Camera.Parameters.FLASH_MODE_OFF) {
                params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                flashButton.foreground = getDrawable(R.drawable.ic_no_flash)
            } else {
                params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                flashButton.foreground = getDrawable(R.drawable.ic_flash)
            }
            mCamera?.parameters = params

        }


    }


    override fun onDestroy() {
        super.onDestroy()
        appSettingsSave();
        mRemoteConfig!!.unregisterReceiver()
        //mCamera?.stopPreview()
        //releaseCamera() // release the camera immediately on pause event
    }

    override fun onResume() {
        super.onResume()
        appSettingsLoad();

    }

    override fun onPause() {
        super.onPause()
        //mCamera?.stopPreview()
        //releaseCamera() // release the camera immediately on pause event
    }


    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }
    private fun setPic(path: String, filename: String) {
        // Get the dimensions of the View
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val b: Bitmap = BitmapFactory.decodeFile(path)


        val width = mPictureWidth
        var height = mPictureHeight

        if(mPictureUseAspectRatio) {
            val aspectRatio: Float = b.getWidth().toFloat() / b.getHeight().toFloat()
            height = Math.round(width / aspectRatio)
        }
        val out: Bitmap = Bitmap.createScaledBitmap(
            b, width, height, false
        )
        //  val out: Bitmap = Bitmap.createScaledBitmap(b, 320, 480, false)

        val file = File(dir, filename)

        Log.d(TAG, "resize image file: ${file.absoluteFile}")

        val fOut: FileOutputStream
        try {
            fOut = FileOutputStream(file)
            out.compress(Bitmap.CompressFormat.JPEG, mPictureQuality, fOut)
            fOut.flush()
            fOut.close()
            b.recycle()
            out.recycle()
        } catch (e: java.lang.Exception) {
            println(e.toString())
        }
    }

    private val mPicture = Camera.PictureCallback { data, _ ->
        val pictureFile: File = this.getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            Log.d(TAG, ("Error creating media file, check storage permissions"))
            return@PictureCallback
        }

        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
            fos.flush()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: ${e.message}")
        }

        setPic("" + pictureFile.absoluteFile, "" + pictureFile.name)

        val returnIntent = Intent()

        returnIntent.putExtra("output", "" + pictureFile.absoluteFile)
        returnIntent.putExtra("PhotoPath", "" + pictureFile.absoluteFile)
        var myUri : Uri = pictureFile.toUri()
        returnIntent.setData(myUri)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()

    }

    /** A safe way to get an instance of the Camera object. */
    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }



    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true
        } else {
            // no camera on this device
            return false
        }
    }
    val MEDIA_TYPE_IMAGE = 1
    val MEDIA_TYPE_VIDEO = 2

    /** Create a file Uri for saving an image or video */
    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    /** Create a File for saving an image or video */
    private fun getOutputMediaFile(type: Int): File? {

        val mediaStorageDir = File("" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
    }

    override fun onRemoteConfigEvent(intent: Intent?) {
        Log.d(TAG, "onRemoteConfigEvent")

        val width = intent!!.getStringExtra("width")
        if (width.isNullOrEmpty() == false)
            mPictureWidth =  width.toInt()

        val height = intent!!.getStringExtra("height")
        if(height.isNullOrEmpty()==false)
            mPictureHeight =  height.toInt()

        val quality = intent!!.getStringExtra("quality")
        if (quality.isNullOrEmpty() == false)
            mPictureQuality =  quality.toInt()

        val AspectRatio = intent!!.getStringExtra("aspectratio")
        if (AspectRatio.isNullOrEmpty() == false)
            mPictureUseAspectRatio =  AspectRatio.toBoolean()

        appSettingsSave()
    }


    private fun appSettingsSave() {
        val sharedPref: SharedPreferences = getSharedPreferences("CAMERASETTINGS", MODE_PRIVATE)

        val editor = sharedPref.edit()
        editor.putInt("height", mPictureHeight);
        editor.putInt("width", mPictureWidth);
        editor.putInt("quality", mPictureQuality);
        editor.putBoolean("aspectratio", mPictureUseAspectRatio);

        editor.commit()
    }


    private fun appSettingsLoad() {
        val sharedPref: SharedPreferences = getSharedPreferences("CAMERASETTINGS", MODE_PRIVATE)

        mPictureWidth = sharedPref.getInt("width", 1440);
        mPictureHeight = sharedPref.getInt("height", 1980);
        mPictureQuality = sharedPref.getInt("quality", 80);
        mPictureUseAspectRatio = sharedPref.getBoolean("aspectratio", true);

    }


}