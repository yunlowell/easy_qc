package com.example.easyqc3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
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
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Moments
import kotlin.math.abs
import kotlin.math.hypot


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

    private var mDetectAble = false

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
            //this.mViewMode = VIEW_MODE_GRAY
            //Toast.makeText(this, "GRAY 모드", Toast.LENGTH_SHORT).show()
            mDetectAble = false

        }
        btnCANNY=this.findViewById(R.id.btn3);
        btnCANNY.setOnClickListener {
            //this.mViewMode = VIEW_MODE_CANNY
            //Toast.makeText(this, "CANNY 모드", Toast.LENGTH_SHORT).show()
            mDetectAble = true
            trackedRect = null
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

                if(mDetectAble) {
                    // 측정 함수 호출
                    val pixelPerMM = 4.2 // 기준 물체로 미리 측정해둔 값
                    //detectAndMeasureRectangle(mIntermediateMat, mRgba, pixelPerMM);
                    detectAndTrackRectangle(mIntermediateMat, mRgba, pixelPerMM);
                }
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

    /** /////////////////////////////////// */ // 전역 변수 선언
    private var lastDetectedRect: RotatedRect? = null
    private val MAX_CENTER_DIST: Double = 50.0
    private val MAX_SIZE_CHANGE_RATIO: Double = 0.25
    private val DIST_WEIGHT: Double = 1.0
    private val AREA_WEIGHT: Double = 1000.0


    private fun detectBestCenteredRectangle(edgeMat: Mat, outputRgbaMat: Mat, pixelPerMM: Double) {
        val contours: List<MatOfPoint> = java.util.ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            edgeMat.clone(), contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) return

        val imageCenter = Point(edgeMat.width() / 2.0, edgeMat.height() / 2.0)

        var bestContour: MatOfPoint? = null
        var bestScore = Double.MAX_VALUE

        val distanceWeight = 1.0
        val areaWeight = 1000.0 // 면적이 중요하므로 가중치를 크게

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < 100) continue  // 너무 작은 건 무시


            val m = Imgproc.moments(contour)
            if (m._m00 == 0.0) continue
            val cx = m._m10 / m._m00
            val cy = m._m01 / m._m00
            val dist = hypot(cx - imageCenter.x, cy - imageCenter.y)

            val score = distanceWeight * dist + areaWeight * (1.0 / area)
            if (score < bestScore) {
                bestScore = score
                bestContour = contour
            }
        }

        if (bestContour != null) {
            val box = Imgproc.minAreaRect(MatOfPoint2f(*bestContour.toArray()))
            val size = box.size

            val widthMM = size.width / pixelPerMM
            val heightMM = size.height / pixelPerMM

            val points = arrayOfNulls<Point>(4)
            box.points(points)
            for (i in 0..3) {
                Imgproc.line(
                    outputRgbaMat,
                    points[i], points[(i + 1) % 4], Scalar(0.0, 255.0, 0.0), 2
                )
            }

            Imgproc.putText(
                outputRgbaMat,
                String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                box.center,
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, Scalar(255.0, 0.0, 0.0), 2
            )
        }
    }


    private var trackedRect: RotatedRect? = null
    private val TRACK_DIST_THRESH: Double = 80.0
    private val TRACK_SIZE_RATIO: Double = 0.3

    private fun detectAndTrackRectangle(edgeMat: Mat, outputRgbaMat: Mat, pixelPerMM: Double) {
        val contours: List<MatOfPoint> = java.util.ArrayList()
        Imgproc.findContours(
            edgeMat.clone(), contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) return

        val imageCenter = Point(edgeMat.width() / 2.0, edgeMat.height() / 2.0)
        val candidates: MutableList<RotatedRect> = java.util.ArrayList()

        for (contour in contours) {
            if (Imgproc.contourArea(contour) < 100) continue
            val rect = Imgproc.minAreaRect(MatOfPoint2f(*contour.toArray()))
            candidates.add(rect)
        }

        candidates.sortWith(Comparator.comparingDouble { r: RotatedRect ->
            hypot(
                r.center.x - imageCenter.x,
                r.center.y - imageCenter.y
            )
        })

        val bestTracked = trackRectangle(candidates)

        if (bestTracked != null) {
            val widthMM = bestTracked.size.width / pixelPerMM
            val heightMM = bestTracked.size.height / pixelPerMM

            val points = arrayOfNulls<Point>(4)
            bestTracked.points(points)
            for (i in 0..3) {
                Imgproc.line(
                    outputRgbaMat,
                    points[i], points[(i + 1) % 4], Scalar(0.0, 255.0, 0.0), 2
                )
            }

            Imgproc.putText(
                outputRgbaMat,
                String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                bestTracked.center,
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, Scalar(255.0, 0.0, 0.0), 2
            )
        }
    }

    private fun trackRectangle(candidates: List<RotatedRect>): RotatedRect? {
        if (candidates.isEmpty()) return trackedRect

        if (trackedRect == null) {
            trackedRect = candidates[0]
            return trackedRect
        }

        var best: RotatedRect? = null
        var minDist = Double.MAX_VALUE

        for (cand in candidates) {
            val dist = hypot(
                cand.center.x - trackedRect!!.center.x,
                cand.center.y - trackedRect!!.center.y
            )

            val widthRatio =
                abs(cand.size.width - trackedRect!!.size.width) / trackedRect!!.size.width
            val heightRatio =
                abs(cand.size.height - trackedRect!!.size.height) / trackedRect!!.size.height

            if (dist < TRACK_DIST_THRESH && widthRatio < TRACK_SIZE_RATIO && heightRatio < TRACK_SIZE_RATIO) {
                if (dist < minDist) {
                    minDist = dist
                    best = cand
                }
            }
        }

        if (best != null) {
            trackedRect = best
        }

        return trackedRect
    }


    private var customCenter: Point? = null // 사용자가 터치한 중심


    // Activity에서 터치 리스너 설정
    fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // 화면 터치 좌표를 OpenCV 좌표로 변환해야 할 수 있음 (예: 비율 보정 필요)
            setCustomCenter(Point(event.x.toDouble(), event.y.toDouble()))
        }
        return true
    }

    fun setCustomCenter(p: Point?) {
        customCenter = p
    }

    private fun detectAndTrackRectangle2(edgeMat: Mat, outputRgbaMat: Mat, pixelPerMM: Double) {
        val contours: List<MatOfPoint> = java.util.ArrayList()
        Imgproc.findContours(
            edgeMat.clone(), contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) return

        val centerRef = if (customCenter != null) customCenter!! else Point(
            edgeMat.width() / 2.0,
            edgeMat.height() / 2.0
        )
        val candidates: MutableList<RotatedRect> = java.util.ArrayList()

        for (contour in contours) {
            if (Imgproc.contourArea(contour) < 100) continue
            val rect = Imgproc.minAreaRect(MatOfPoint2f(*contour.toArray()))
            candidates.add(rect)
        }

        candidates.sortWith(Comparator.comparingDouble { r: RotatedRect ->
            hypot(
                r.center.x - centerRef.x,
                r.center.y - centerRef.y
            )
        })

        val bestTracked = trackRectangle2(candidates)

        if (bestTracked != null) {
            val widthMM = bestTracked.size.width / pixelPerMM
            val heightMM = bestTracked.size.height / pixelPerMM

            val points = arrayOfNulls<Point>(4)
            bestTracked.points(points)
            for (i in 0..3) {
                Imgproc.line(
                    outputRgbaMat,
                    points[i], points[(i + 1) % 4], Scalar(0.0, 255.0, 0.0), 2
                )
            }

            Imgproc.putText(
                outputRgbaMat,
                String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                bestTracked.center,
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, Scalar(255.0, 0.0, 0.0), 2
            )
        }
    }

    private fun trackRectangle2(candidates: List<RotatedRect>): RotatedRect? {
        if (candidates.isEmpty()) return trackedRect

        if (trackedRect == null) {
            trackedRect = candidates[0]
            return trackedRect
        }

        var best: RotatedRect? = null
        var minDist = Double.MAX_VALUE

        for (cand in candidates) {
            val dist = hypot(
                cand.center.x - trackedRect!!.center.x,
                cand.center.y - trackedRect!!.center.y
            )

            val widthRatio =
                abs(cand.size.width - trackedRect!!.size.width) / trackedRect!!.size.width
            val heightRatio =
                abs(cand.size.height - trackedRect!!.size.height) / trackedRect!!.size.height

            if (dist < TRACK_DIST_THRESH && widthRatio < TRACK_SIZE_RATIO && heightRatio < TRACK_SIZE_RATIO) {
                if (dist < minDist) {
                    minDist = dist
                    best = cand
                }
            }
        }

        if (best != null) {
            trackedRect = best
        }

        return trackedRect
    }

}
