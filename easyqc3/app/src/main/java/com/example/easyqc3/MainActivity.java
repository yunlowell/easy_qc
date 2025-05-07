package com.example.easyqc3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.hypot;

/**
 * MainActivity: OpenCV 카메라 화면에서 윤곽선 기반 사각형을 추적하고 실시간 치수(mm)를 측정하는 활동 클래스
 */
public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnTouchListener {

    // 디버그용 태그 문자열
    private static final String TAG = "OCVSample::Activity";

    // 카메라 뷰 모드 상수 정의
    private static final int VIEW_MODE_RGBA = 0;
    private static final int VIEW_MODE_GRAY = 1;
    private static final int VIEW_MODE_CANNY = 2;
    private static final int VIEW_MODE_FEATURES = 5;

    // 현재 선택된 뷰 모드
    private int mViewMode = VIEW_MODE_CANNY;

    // OpenCV 영상 처리용 Mat 객체
    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    // 메뉴 항목 변수들
    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewGray;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;

    // OpenCV 카메라 뷰
    private CameraBridgeViewBase mOpenCvCameraView;

    // 사각형 검출 가능 여부
    private boolean mDetectAble = false;
    private boolean mDrawTouchCircle = false;

    // UI 버튼들
    private Button btnGRAY;
    private Button btnCANNY;
    private Button btnCameraCalibration;

    // 사각형 추적 변수
    private RotatedRect trackedRect = null;
    private static final double TRACK_DIST_THRESH = 80.0;       // 추적 허용 거리
    private static final double TRACK_SIZE_RATIO = 0.3;         // 추적 허용 크기비율

    // 사용자가 터치한 커스텀 중심 좌표
    private Point customCenter = null;

    // JNI 함수 (특징점 검출용)
    public native void FindFeatures(long matAddrGr, long matAddrRgba);

    // 네이티브 라이브러리 로딩
    static {
        System.loadLibrary("mixed_sample");
    }

    /** 액티비티 생성 시 호출 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "called onCreate");

        // OpenCV 초기화 실패 시 앱 종료
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }

        // 화면 꺼짐 방지 플래그 설정
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 레이아웃 설정
        setContentView(R.layout.tutorial2_surface_view);

        // 카메라 뷰 구성
        mOpenCvCameraView = findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);

        // 버튼 이벤트 설정
        btnGRAY = findViewById(R.id.btn2);
        btnGRAY.setOnClickListener(v -> mDetectAble = false);

        btnCANNY = findViewById(R.id.btn3);
        btnCANNY.setOnClickListener(v -> {
            mDetectAble = true;
            trackedRect = null;
        });

        btnCameraCalibration = findViewById(R.id.btn4);
        btnCameraCalibration.setOnClickListener(v -> {
            startActivity(new Intent(this, CameraCalibrationActivity.class));
        });
    }

    /** 메뉴 생성 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    /** 메뉴 선택 처리 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mItemPreviewRGBA) mViewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewGray) mViewMode = VIEW_MODE_GRAY;
        else if (item == mItemPreviewCanny) mViewMode = VIEW_MODE_CANNY;
        else if (item == mItemPreviewFeatures) mViewMode = VIEW_MODE_FEATURES;
        return true;
    }

    /** 앱 일시 중지 시 처리 */
    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    /** 앱 재개 시 처리 */
    @Override
    protected void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null) mOpenCvCameraView.enableView();
        invalidateOptionsMenu();
    }

    /** 앱 종료 시 처리 */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    /** 카메라 뷰 목록 반환 */
    @Override
    public List<CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    /** 카메라 뷰 시작 시 처리 */
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);       // 컬러 프레임
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4); // 중간처리용
        mGray = new Mat(height, width, CvType.CV_8UC1);       // 그레이스케일
    }

    /** 카메라 뷰 종료 시 처리 */
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    /** 카메라 프레임 처리 (주 뷰 로직) */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        switch (mViewMode) {
            case VIEW_MODE_GRAY:
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                return mRgba;
            case VIEW_MODE_RGBA:
                return inputFrame.rgba();
            case VIEW_MODE_CANNY:
                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80.0, 100.0);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                if (mDetectAble) {
                    double pixelPerMM = 4.2; // mm당 픽셀 수 (기준값)
                    detectAndTrackRectangle2(mIntermediateMat, mRgba, pixelPerMM);

                    if(mDrawTouchCircle) drawTouchFeedback(mRgba); // 사용자 터치 피드백 그리기
                }
                return mRgba;
            case VIEW_MODE_FEATURES:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
                return mRgba;
            default:
                return inputFrame.rgba();
        }
    }

    /** 사용자 터치 이벤트 처리 */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
        //    setCustomCenter(new Point(event.getX(), event.getY()));
        //}
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDrawTouchCircle = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mDrawTouchCircle = false;
        }

        if ((event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) && mOpenCvCameraView != null && mRgba != null) {
            int viewWidth = mOpenCvCameraView.getWidth();
            int viewHeight = mOpenCvCameraView.getHeight();
            int frameWidth = mRgba.width();
            int frameHeight = mRgba.height();

            // 화면에 비례하여 실제 프레임이 가운데에 있을 경우 offset 보정
            double aspectRatioView = (double) viewWidth / viewHeight;
            double aspectRatioFrame = (double) frameWidth / frameHeight;

            double imageX, imageY;

            if (aspectRatioView > aspectRatioFrame) {
                // 화면이 더 넓은 경우 (상하 여백 존재)
                double scale = (double) frameHeight / viewHeight;
                double offsetX = (viewWidth - frameWidth / scale) / 2.0;
                imageX = (event.getX() - offsetX) * scale;
                imageY = event.getY() * scale;
            } else {
                // 화면이 더 긴 경우 (좌우 여백 존재)
                double scale = (double) frameWidth / viewWidth;
                double offsetY = (viewHeight - frameHeight / scale) / 2.0;
                imageX = event.getX() * scale;
                imageY = (event.getY() - offsetY) * scale;
            }

            // 변환된 좌표가 카메라 프레임 내부에 있는 경우에만 설정
            if (imageX >= 0 && imageX < frameWidth && imageY >= 0 && imageY < frameHeight) {
                setCustomCenter(new Point(imageX, imageY));
                trackedRect = null;
            }
        }
        return true;
    }

    /** 사용자 중심 좌표 설정 */
    public void setCustomCenter(Point p) {
        customCenter = p;
    }

    /**
     * 가장 중심에 가까운 사각형을 선택하여 추적 및 치수 측정
     */
    private void detectAndTrackRectangle2(Mat edgeMat, Mat outputRgbaMat, double pixelPerMM) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgeMat.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) return;

        // 중심 기준점 설정 (사용자 지정 중심이 없을 경우 화면 중앙)
        Point centerRef = (customCenter != null) ? customCenter : new Point(outputRgbaMat.width() / 2.0, outputRgbaMat.height() / 2.0);

        // 윤곽선으로부터 외접 사각형 추출
        List<RotatedRect> candidates = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) < 100) continue;  // 너무 작은 객체는 무시
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            candidates.add(rect);
        }

        // 중심에서 가까운 순으로 정렬
        candidates.sort(Comparator.comparingDouble(r ->
                hypot(r.center.x - centerRef.x, r.center.y - centerRef.y)
        ));

        // 가장 적합한 사각형 선택
        RotatedRect bestTracked = trackRectangle2(candidates);

        if (bestTracked != null) {
            double widthMM = bestTracked.size.width / pixelPerMM;
            double heightMM = bestTracked.size.height / pixelPerMM;

            // 사각형을 선으로 표시
            Point[] points = new Point[4];
            bestTracked.points(points);
            for (int i = 0; i < 4; i++) {
                Imgproc.line(outputRgbaMat, points[i], points[(i + 1) % 4], new Scalar(0, 255, 0), 2);
            }

            // 텍스트로 mm 단위 치수 출력
            Imgproc.putText(
                    outputRgbaMat,
                    String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                    bestTracked.center,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.8,
                    new Scalar(255, 0, 0), 2
            );
        }
    }

    /**
     * 이전에 추적한 사각형과 가장 유사한 사각형을 선택
     */
    private RotatedRect trackRectangle2(List<RotatedRect> candidates) {
        if (candidates.isEmpty()) return trackedRect;

        if (trackedRect == null) {
            trackedRect = candidates.get(0);
            return trackedRect;
        }

        RotatedRect best = null;
        double minDist = Double.MAX_VALUE;

        for (RotatedRect cand : candidates) {
            double dist = hypot(cand.center.x - trackedRect.center.x, cand.center.y - trackedRect.center.y);
            double widthRatio = abs(cand.size.width - trackedRect.size.width) / trackedRect.size.width;
            double heightRatio = abs(cand.size.height - trackedRect.size.height) / trackedRect.size.height;

            // 거리와 크기 변화율이 허용 범위 내에 있을 때만 추적
            if (dist < TRACK_DIST_THRESH && widthRatio < TRACK_SIZE_RATIO && heightRatio < TRACK_SIZE_RATIO) {
                if (dist < minDist) {
                    minDist = dist;
                    best = cand;
                }
            }
        }

        if (best != null) {
            trackedRect = best;
        }

        return trackedRect;
    }

    /**
     * 가장 중심에 가까운 사각형을 탐지하고 그 크기를 mm 단위로 측정하여 화면에 출력
     */
    private void detectBestCenteredRectangle(Mat edgeMat, Mat outputRgbaMat, double pixelPerMM) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeMat.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) return;

        Point imageCenter = new Point(edgeMat.width() / 2.0, edgeMat.height() / 2.0);
        MatOfPoint bestContour = null;
        double bestScore = Double.MAX_VALUE;

        // 각 윤곽선의 무게 중심과 면적을 기반으로 점수 계산
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area < 100) continue;

            Moments m = Imgproc.moments(contour);
            if (m.get_m00() == 0.0) continue;

            double cx = m.get_m10() / m.get_m00();
            double cy = m.get_m01() / m.get_m00();
            double dist = hypot(cx - imageCenter.x, cy - imageCenter.y);
            double score = dist + 1000.0 * (1.0 / area);  // 거리 + 면적 기반 점수

            if (score < bestScore) {
                bestScore = score;
                bestContour = contour;
            }
        }

        if (bestContour != null) {
            RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(bestContour.toArray()));
            Size size = box.size;
            double widthMM = size.width / pixelPerMM;
            double heightMM = size.height / pixelPerMM;

            // 사각형 외곽선 그리기
            Point[] points = new Point[4];
            box.points(points);
            for (int i = 0; i < 4; i++) {
                Imgproc.line(outputRgbaMat, points[i], points[(i + 1) % 4], new Scalar(0, 255, 255), 2);
            }

            // 측정 결과 출력
            Imgproc.putText(
                    outputRgbaMat,
                    String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                    box.center,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.8,
                    new Scalar(0, 0, 255), 2
            );
        }
    }


    /**
     * 가장 큰 윤곽선을 선택해 사각형을 검출하고 치수를 mm 단위로 측정하여 화면에 출력
     */
    private void detectAndMeasureRectangle(Mat edgeMat, Mat outputRgbaMat, double pixelPerMM) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeMat.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0.0;
        MatOfPoint biggest = null;

        // 면적이 가장 큰 윤곽선 선택
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                biggest = contour;
            }
        }

        if (biggest != null) {
            RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(biggest.toArray()));
            Size size = box.size;
            double widthMM = size.width / pixelPerMM;
            double heightMM = size.height / pixelPerMM;

            // 사각형 외곽선 그리기 (초록색)
            Point[] points = new Point[4];
            box.points(points);
            for (int i = 0; i < 4; i++) {
                Imgproc.line(outputRgbaMat, points[i], points[(i + 1) % 4], new Scalar(0, 255, 0), 2);
            }

            // 측정 결과 출력
            Imgproc.putText(
                    outputRgbaMat,
                    String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                    box.center,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.8,
                    new Scalar(255, 0, 0), 2
            );
        }
    }



    // 추가적으로 필요한 기능이 있다면 이곳에 작성 가능합니다.
    // 예: 측정값 로깅, 사각형 ID 부여, 스냅샷 저장, 서버 전송 등

    // 예시 - 로그 출력 템플릿 (사용자 정의용)
    private void logMeasurement(String label, double width, double height) {
        Log.i("MEASURE", String.format("%s: %.2fmm x %.2fmm", label, width, height));
    }

    // 예시 - 측정값을 텍스트로 반환 (단위 테스트용)
    private String getSizeString(RotatedRect rect, double pixelPerMM) {
        double widthMM = rect.size.width / pixelPerMM;
        double heightMM = rect.size.height / pixelPerMM;
        return String.format("%.2fmm x %.2fmm", widthMM, heightMM);
    }

    /**
     * 사용자 터치 좌표에 원을 그려주는 함수
     */
    private void drawTouchCircle(Mat mat, Point center) {
        // 빨간색 원을 터치 위치에 반지름 20픽셀로 그림
        Imgproc.circle(mat, center, 20, new Scalar(255, 0, 0), 3);
    }

    /**
     * onCameraFrame 안에서 사용자 터치 위치에 원을 그려주는 부분 추가
     */
    private void drawTouchFeedback(Mat frame) {
        if (customCenter != null) {
            drawTouchCircle(frame, customCenter);
        }
    }

}
