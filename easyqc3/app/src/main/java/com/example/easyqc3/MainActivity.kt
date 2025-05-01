package com.example.easyqc3

/*  opencv Camera View만 나오는 sample
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : CameraActivity(), CvCameraViewListener2 {

    companion object {
        private const val TAG = "OCVSample::Activity"
    }

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    init {
        Log.i(TAG, "Instantiated new ${this::class.java}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "called onCreate")

        // OpenCV 로드
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        mOpenCvCameraView = findViewById(R.id.activity_surface_view)
        mOpenCvCameraView?.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView?.setCvCameraViewListener(this)
    }

    override fun onResume() {
        super.onResume()
        mOpenCvCameraView?.enableView()
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase> {
        return listOfNotNull(mOpenCvCameraView)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        // 필요하면 초기화 작업
    }

    override fun onCameraViewStopped() {
        // 필요하면 정리 작업
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        return inputFrame.rgba()
    }
}
*/

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

import androidx.appcompat.app.AppCompatActivity;


import android.view.SurfaceView
import android.widget.Button


class MainActivity : CameraActivity(), CvCameraViewListener2 {

    companion object {
        private const val TAG = "OCVSample::Activity"

        private const val VIEW_MODE_RGBA = 0
        private const val VIEW_MODE_GRAY = 1
        private const val VIEW_MODE_CANNY = 2
        private const val VIEW_MODE_FEATURES = 5
    }

    private var mViewMode: Int = VIEW_MODE_CANNY    //VIEW_MODE_RGBA
    private lateinit var mRgba: Mat
    private lateinit var mIntermediateMat: Mat
    private lateinit var mGray: Mat

    private var mItemPreviewRGBA: MenuItem? = null
    private var mItemPreviewGray: MenuItem? = null
    private var mItemPreviewCanny: MenuItem? = null
    private var mItemPreviewFeatures: MenuItem? = null

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    init {
        Log.i(TAG, "Instantiated new ${this::class.java}")
    }

    //// 버튼 ID 참조용
    private lateinit var btnRGB: Button
    private lateinit var btnGRAY: Button
    private lateinit var btnCANNY: Button
    private lateinit var btnCameraCalibration: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "called onCreate")

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show()
            return
        }

        // Load native library after OpenCV initialization
        System.loadLibrary("mixed_sample")

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.tutorial2_surface_view)

        mOpenCvCameraView = findViewById(R.id.tutorial2_activity_surface_view)
        mOpenCvCameraView?.visibility = CameraBridgeViewBase.VISIBLE
        mOpenCvCameraView?.setCvCameraViewListener(this)


        btnRGB = this.findViewById(R.id.btn1);
        btnRGB.setOnClickListener {
            this.mViewMode = VIEW_MODE_RGBA
            Toast.makeText(this, "RGBA 모드", Toast.LENGTH_SHORT).show()
        }

        btnGRAY= this.findViewById(R.id.btn2);
        btnGRAY.setOnClickListener {
            this.mViewMode = VIEW_MODE_GRAY
            Toast.makeText(this, "GRAY 모드", Toast.LENGTH_SHORT).show()
        }
        btnCANNY=this.findViewById(R.id.btn3);
        btnCANNY.setOnClickListener {
            this.mViewMode = VIEW_MODE_CANNY
            Toast.makeText(this, "CANNY 모드", Toast.LENGTH_SHORT).show()
        }
        btnCameraCalibration=this.findViewById(R.id.btn4);
        btnCameraCalibration.setOnClickListener {

            Toast.makeText(this, "Camera Calibration", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "called onCreateOptionsMenu")
        mItemPreviewRGBA = menu.add("Preview RGBA")
        mItemPreviewGray = menu.add("Preview GRAY")
        mItemPreviewCanny = menu.add("Canny")
        mItemPreviewFeatures = menu.add("Find features")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "called onOptionsItemSelected; selected item: $item")

        mViewMode = when (item) {
            mItemPreviewRGBA -> VIEW_MODE_RGBA
            mItemPreviewGray -> VIEW_MODE_GRAY
            mItemPreviewCanny -> VIEW_MODE_CANNY
            mItemPreviewFeatures -> VIEW_MODE_FEATURES
            else -> mViewMode
        }

        return true
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onResume() {
        super.onResume()
        mOpenCvCameraView?.enableView()

        invalidateOptionsMenu(); // 메뉴 갱신 요청
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase> {
        return listOfNotNull(mOpenCvCameraView)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mIntermediateMat = Mat(height, width, CvType.CV_8UC4)
        mGray = Mat(height, width, CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
        mGray.release()
        mIntermediateMat.release()
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        return when (mViewMode) {
            VIEW_MODE_GRAY -> {
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
                mRgba
            }
            VIEW_MODE_RGBA -> {
                inputFrame.rgba()
            }
            VIEW_MODE_CANNY -> {
                mRgba = inputFrame.rgba()
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80.0, 100.0)
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
                mRgba
            }
            VIEW_MODE_FEATURES -> {
                mRgba = inputFrame.rgba()
                mGray = inputFrame.gray()
                FindFeatures(mGray.nativeObjAddr, mRgba.nativeObjAddr)
                mRgba
            }
            else -> {
                inputFrame.rgba()
            }
        }
    }

    external fun FindFeatures(matAddrGr: Long, matAddrRgba: Long)
}
