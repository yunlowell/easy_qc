package com.example.easyqc3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


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

/*
        btnRGB = this.findViewById(R.id.btn1);
        btnRGB.setOnClickListener {
            this.mViewMode = VIEW_MODE_RGBA
            Toast.makeText(this, "RGBA 모드", Toast.LENGTH_SHORT).show()
        }
*/
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
            val intent = Intent(this, CameraCalibrationActivity::class.java)
            startActivity(intent)
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

                // 측정 함수 호출
                val pixelPerMM = 4.2 // 기준 물체로 미리 측정해둔 값
                detectAndMeasureRectangle(mIntermediateMat, mRgba, pixelPerMM);

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

    //추출된 contours 중에서 가장큰 사각형을 찾아서 표시한다.
    private fun detectAndMeasureRectangle(edgeMat: Mat, outputRgbaMat: Mat, pixelPerMM: Double) {
        // 1. 윤곽선 검출
        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            edgeMat.clone(), contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        // 2. 가장 큰 사각형 찾기
        var maxArea = 0.0
        var biggest: MatOfPoint? = null
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                maxArea = area
                biggest = contour
            }
        }

        if (biggest != null) {
            // 3. 최소 외접 사각형 추출
            val box = Imgproc.minAreaRect(MatOfPoint2f(*biggest.toArray()))
            val size: Size = box.size
            val widthPx: Double = size.width
            val heightPx: Double = size.height

            // 4. 픽셀 → mm 변환
            val widthMM = widthPx / pixelPerMM
            val heightMM = heightPx / pixelPerMM

            Log.i("MEASURE", String.format("Width: %.2f mm, Height: %.2f mm", widthMM, heightMM))

            // 5. 사각형 시각화
            val boxPoints: Array<Point?> = arrayOfNulls<Point>(4)
            box.points(boxPoints)
            for (i in 0..3) {
                Imgproc.line(
                    outputRgbaMat,
                    boxPoints[i], boxPoints[(i + 1) % 4], Scalar(0.0, 255.0, 0.0), 2
                )
            }

            // 6. 측정값 표시 (선택)
            Imgproc.putText(
                outputRgbaMat,
                String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                box.center,
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, Scalar(255.0, 0.0, 0.0), 2
            )
        }
    }


}
