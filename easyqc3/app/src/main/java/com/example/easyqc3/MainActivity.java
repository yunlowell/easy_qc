package com.example.easyqc3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
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
import org.opencv.objdetect.ArucoDetector;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


// DB관련
import com.example.easyqc3.model.HistoryItem;
import com.example.easyqc3.model.MeasurementSetting;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import static java.lang.Math.abs;
import static java.lang.Math.hypot;

import androidx.annotation.NonNull;

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

    private TextView tvLengthValue;
    private TextView tvRealLengthValue;
    private TextView tvJudgement_value;

    // 사각형 추적 변수
    private RotatedRect trackedRect = null;
    private static final double TRACK_DIST_THRESH = 80.0;       // 추적 허용 센터거리 pixel
    private static final double TRACK_SIZE_RATIO = 0.3;         // 추적 허용 크기비율 0.3->30%

    private final double[] mScaleH_pixelPerMM = new double[]{0.0, 0.0};   // 세로 스케일 (px/mm)
    private final double[] mScaleW_pixelPerMM = new double[]{0.0, 0.0};   // 가로 스케일 (px/mm)

    private final int[] mScaleInCount = new int[]{0, 0};   // 가로 세로 스케일 적합 횟수 (px/mm)
    private int mTrackingSuccessCount = 0;   // 추적 성공 횟수


    // 사용자가 터치한 커스텀 중심 좌표
    private Point customCenter = null;


    //openCV CameraFrame Thread에서 UI변경 호출할때 사용.
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private double mRealwidthMM = 0.0;  // 실제 측정된 길이
    private Double mDBreferenceLength = 0.0;
    private Double mDBtolerance = 0.0;
    private String mDBunit = "mm";
    private String mEmail = null;   // user email

    // JNI 함수 (특징점 검출용)
    public native void FindFeatures(long matAddrGr, long matAddrRgba);

    // 네이티브 라이브러리 로딩
    static {
        System.loadLibrary("mixed_sample");
    }

    /** 액티비티 생성 시 호출 */
    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "called onCreate");

        //calibration 버튼 활성화
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("calibration_button_enabled", true).apply();

        // user 계정 불러오기
        SharedPreferences sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        mEmail = sharedPrefs.getString("email", null);

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
        btnGRAY.setOnClickListener(v -> {
            //mDetectAble = false;
                    saveDataToFirestore(mEmail, 30.1, "okay", mDBreferenceLength, mDBtolerance, mDBunit);

        });

        btnCANNY = findViewById(R.id.btn3);
        btnCANNY.setOnClickListener(v -> {
            if(mDetectAble){
                btnCANNY.setText("검증 시작");
                mDetectAble = false;
            }
            else {
                btnCANNY.setText("검증 중지");
                findViewById(R.id.result_all_Layout).setVisibility(View.VISIBLE);
                resetResult();
                mDetectAble = true;
            }
            trackedRect = null;
        });

        btnCameraCalibration = findViewById(R.id.btn4);
        btnCameraCalibration.setOnClickListener(v -> {
            startActivity(new Intent(this, CameraCalibrationActivity.class));
            //btnCameraCalibration.setEnabled(false);     //한번 동작하면 비활성화 하도록 수정.
        });

        // 기준값 update용
        tvLengthValue = findViewById(R.id.length_value);
        tvRealLengthValue = findViewById(R.id.real_length_value);
        tvJudgement_value = findViewById(R.id.judgement_value);

        findViewById(R.id.result_all_Layout).setVisibility(View.INVISIBLE);


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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("calibration_button_enabled", true);

        btnCameraCalibration.setEnabled(enabled);

        //테스트 코드 email기반으로 Firestore에서 설정값을 불러오기 /////////////
        SharedPreferences sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String email = sharedPrefs.getString("email", null);
        getSettingsFromFirestore(email);
        /// //////////////////////////////////////////////////////////////ㅁ

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

        // 카메라 calibration 데이터 로드
        loadCameraCalibration(width, height);
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

                List<Mat> corners = new ArrayList<>();
                Mat ids = new Mat();
                Mat gray = new Mat();
                Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2GRAY);

                // 마커 감지
                detector.detectMarkers(gray, corners, ids);
                //Log.i("Aruco", "감지된 마커 수: " + ids.rows());

                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80.0, 100.0);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                if (mDetectAble) {
                    detectAndTrackRectangle2(mIntermediateMat, mRgba);

                    if(mDrawTouchCircle) drawTouchFeedback(mRgba);  // 사용자 터치 피드백 그리기

                    detectArucoAndEstimateScale(mRgba, corners, ids);   // 아루코 마커 검출 및 스케일 추정
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
    @SuppressLint("ClickableViewAccessibility")
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
    private void detectAndTrackRectangle2(Mat edgeMat, Mat outputRgbaMat) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // 외부 윤곽선만 감지
        Imgproc.findContours(edgeMat.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.isEmpty()) return;

        // 외부 + 내부 윤곽선 모두 감지
        //Imgproc.findContours(edgeMat.clone(), contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        //if (contours.isEmpty() || hierarchy.empty()) return;

        // 중심 기준점 설정 (사용자 지정 중심이 없을 경우 화면 중앙)
        Point centerRef = (customCenter != null) ? customCenter : new Point(outputRgbaMat.width() / 2.0, outputRgbaMat.height() / 2.0);

        // 윤곽선으로부터 외접 사각형 추출
        List<RotatedRect> candidates = new ArrayList<>();

        // 윤곽선으로부터 후보 사각형 추출
        for (int i = 0; i < contours.size(); i++) {
            //double[] h = hierarchy.get(0, i);
            //int parentIdx = (int) h[3];

            double  contourArea = Imgproc.contourArea(contours.get(i));
            if (contourArea < 400 || contourArea > 90000) continue;  // 작은(20x20) 큰(300x300) 윤곽선은 제외

            //돌기 제거 없이 있는 그대로 모든 윤곽선에 대해 사각형 추출
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            candidates.add(rect);   // 모든 윤곽선에 대해 사각형 추출

            /* 돌기 제거하는 코드 넣으면 제품 추출이 잘 안됨.
            // 1. contour를 2f 타입으로 변환
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());

            // 2. 근사 다각형 계산 (0.02는 정밀도 - 필요 시 조절) - 돌기 부분 제거하기
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approxCurve, 0.02 * Imgproc.arcLength(contour2f, true), true);

            // 3. 꼭짓점이 4개일 때만 사각형으로 간주  모든 contour에 대해 사각형 추출 하도록 수정.
            //if (approxCurve.total() == 4) {
                RotatedRect rect = Imgproc.minAreaRect(approxCurve);
                candidates.add(rect);   // 모든 윤곽선에 대해 사각형 추출
            //}
            */

        }

        // 중심에서 가까운 순으로 정렬
        candidates.sort(Comparator.comparingDouble(r ->
                hypot(r.center.x - centerRef.x, r.center.y - centerRef.y)
        ));

        // 가장 적합한 사각형 선택
        RotatedRect bestTracked = trackRectangle2(candidates);

        if (bestTracked != null) {
            //double widthMM = bestTracked.size.width / mScaleW_pixelPerMM;   //pixelPerMM;
            //double heightMM = bestTracked.size.height / mScaleH_pixelPerMM; //pixelPerMM;

            // 사각형을 선으로 표시
            Point[] points = new Point[4];
            bestTracked.points(points);
            for (int i = 0; i < 4; i++) {
                Imgproc.line(outputRgbaMat, points[i], points[(i + 1) % 4], new Scalar(0, 255, 0), 2);
            }

            // 변 길이 계산 (반시계 방향으로 p0→p1, p1→p2 ...)
            double len0 = Math.hypot(points[0].x - points[1].x, points[0].y - points[1].y); // 변 1
            double len1 = Math.hypot(points[1].x - points[2].x, points[1].y - points[2].y); // 변 2
            double len2 = Math.hypot(points[2].x - points[3].x, points[2].y - points[3].y); // 변 3
            double len3 = Math.hypot(points[3].x - points[0].x, points[3].y - points[0].y); // 변 4

            // 평균 가로/세로 추정 (변 0,2 = 가로 / 변 1,3 = 세로)
            double width  = (len0 + len2) / 2.0;
            double height = (len1 + len3) / 2.0;

            // 가로세로 바뀜 방지: 길이가 더 긴 쪽을 가로로 간주
            double avgWidth = Math.max(width, height);
            double avgHeight = Math.min(width, height);

            // mm 변환
            double widthMM  = avgWidth  / (mScaleW_pixelPerMM[0] + mScaleW_pixelPerMM[1]);
            double heightMM = avgHeight / (mScaleH_pixelPerMM[0] + mScaleH_pixelPerMM[1]);

            if (mScaleH_pixelPerMM[1] > 0.0 && mScaleW_pixelPerMM[1] > 0.0) {
                widthMM  *= 2.0;
                heightMM *= 2.0;
            }

            Point ptDraw = new Point(
                bestTracked.center.x - avgWidth/2, bestTracked.center.y + avgHeight/2 + 30
            );

            // 텍스트로 mm 단위 치수 출력
            Imgproc.putText(
                    outputRgbaMat,
                    String.format("%.1fmm x %.1fmm", widthMM, heightMM),
                    ptDraw,  //bestTracked.center,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7,
                    new Scalar(0, 255, 0), 2
            );

            mRealwidthMM = widthMM;

            // 가로 측정치를 UI에 표시하고, 판정기준에 부함할때 판정치를 UI에 표시
            if (mDetectAble)
                mHandler.post(() -> {
                    showConfirmedResult( mRealwidthMM );  //이것 때문에 자주 죽네... 방법 검토 필요함.
                });

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
            mTrackingSuccessCount++;        // 추적 성공 횟수 증가
        }
        else {
            mTrackingSuccessCount = 0;      // 추적 실폐하면 0으로 초기화
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
                    new Scalar(0, 255, 0), 2
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

    // ArUco 기반 카메라 보정 및 포즈 추정 기반 실측 측정 코드 추가
    // 카메라 내부 행렬 및 왜곡 계수 (보정된 파일에서 로드)
    private Mat cameraMatrix = new Mat();
    private Mat distCoeffs = new Mat();
    private ArucoDetector detector = new ArucoDetector();
    private double arucoMarkerLengthMM = 50.0; // 사용된 ArUco 마커의 실제 한 변 길이 (mm)

    private int mWidth=0, mHeight=0;
    private CameraCalibrator mCalibrator;
    /**
     * 캘리브레이션 데이터 로드
     */
    private void loadCameraCalibration(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients())) {
                mCalibrator.setCalibrated();
            }

            //mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
        }
    }

    /**
     * ArUco 마커를 이용하여 기울어짐을 보정한 가로/세로 별 스케일(px/mm) 계산
     */
    private void detectArucoAndEstimateScale(Mat inputMat, List<Mat> corners, Mat ids) {

        if (ids.total() > 0 && corners.size() > 0) {

            mScaleW_pixelPerMM[0] = mScaleW_pixelPerMM[1] = 0.0;
            mScaleH_pixelPerMM[0] = mScaleH_pixelPerMM[1] = 0.0;

            for (int i = 0; i < ids.rows() && i<2 ; i++) {
                Mat corner = corners.get(i);
                if (corner.cols() < 4) continue;


                //p0 -> lefttop 으로 고정되지 않아서 가로세로가 바뀌는 문제 발생함.
                Point p0 = new Point(corner.get(0, 0));
                Point p1 = new Point(corner.get(0, 1));
                Point p2 = new Point(corner.get(0, 2));
                Point p3 = new Point(corner.get(0, 3));

                double width1 = Math.hypot(p0.x - p1.x, p0.y - p1.y);
                double width2 = Math.hypot(p2.x - p3.x, p2.y - p3.y);
                double height1 = Math.hypot(p1.x - p2.x, p1.y - p2.y);
                double height2 = Math.hypot(p3.x - p0.x, p3.y - p0.y);

                //double avgWidth = (width1 + width2) / 2.0;
                //double avgHeight = (height1 + height2) / 2.0;

                // 대략 수평인 변 두 개와 수직인 변 두 개로 판단
                double width = (width1 + width2) / 2.0;
                double height = (height1 + height2) / 2.0;

                // 가로세로 바뀜 방지: 길이가 더 긴 쪽을 가로로 간주
                double avgWidth = Math.max(width, height);
                double avgHeight = Math.min(width, height);

                mScaleW_pixelPerMM[i] = avgWidth / arucoMarkerLengthMM;
                mScaleH_pixelPerMM[i] = avgHeight / arucoMarkerLengthMM;

                Scalar fontColor = null;
                if (mScaleW_pixelPerMM[i] -mScaleH_pixelPerMM[i] <= 0.05 && mScaleW_pixelPerMM[i] -mScaleH_pixelPerMM[i] >= -0.05) {
                    fontColor = new Scalar(0, 255, 0);
                    mScaleInCount[i] += 1;
                } else {
                    fontColor = new Scalar(255, 0, 0);
                    mScaleInCount[i] = 0;
                }

                Imgproc.putText(inputMat, String.format("No.%d scale(px/mm) W:%.2f , H:%.2f count(%d)", i+1, mScaleW_pixelPerMM[i], mScaleH_pixelPerMM[i], mScaleInCount[i]),
                        new Point(30, 90 + i * 25), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, fontColor, 2);

            }
        }
    }

    /*
    data 가져오는 함수
     */
    private void getSettingsFromFirestore(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(email)
                .collection("measurements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        mDBreferenceLength = document.getDouble("referenceLength");
                        mDBtolerance = document.getDouble("tolerance");
                        mDBunit = document.getString("unit");

                        tvLengthValue.setText(String.format(" %.2f %s", mDBreferenceLength, mDBunit));
                        Log.d("Firestore", "기준값: " + mDBreferenceLength + " " + mDBunit + ", 허용 오차: " + mDBtolerance);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "데이터 가져오기 실패: " + e.getMessage(), e);
                });
    }

    private void resetResult(){
        // 기준값 update용
        //tvLengthValue 해당값은 설정 되어 있으므로 reset 하지 않음
        tvRealLengthValue.setText(String.format(" -- %s", mDBunit));
        tvJudgement_value.setText(" -- ");
        tvJudgement_value.setTextColor(getResources().getColor(R.color.green));
    }

    private void showConfirmedResult(double widthMM) {

        //Aruco마커 2개의 가로세로 스케일이 적합하고, Tracking이 성공한 경우에만 결과를 판단한다.
        if( mScaleInCount[0]>10 && mScaleInCount[1]>10 && mTrackingSuccessCount>1) {

            mScaleInCount[0] = mScaleInCount[1] = mTrackingSuccessCount = 0; // 초기화

            //검증 종료를 한다.
            //mDetectAble = false;
            //btnCANNY.setText("검증 시작");
            btnCANNY.callOnClick();

            // 기준값 update용
            tvRealLengthValue.setText(String.format(" %.2f %s", widthMM, mDBunit));

            // 허용 오차 범위 계산
            double lowerBound = mDBreferenceLength - mDBtolerance;
            double upperBound = mDBreferenceLength + mDBtolerance;

            // 결과 판단 및 텍스트 색상 변경
            if (widthMM >= lowerBound && widthMM <= upperBound) {
                tvJudgement_value.setText("OK");
                tvJudgement_value.setTextColor(getResources().getColor(R.color.green));

                //양품으로 history 저장.
                saveDataToFirestore(mEmail, widthMM, "okay", mDBreferenceLength, mDBtolerance, mDBunit);
            } else {
                tvJudgement_value.setText("Fail");
                tvJudgement_value.setTextColor(getResources().getColor(R.color.red));

                //불량으로 history 저장.
                saveDataToFirestore(mEmail, widthMM, "fail", mDBreferenceLength, mDBtolerance, mDBunit);
            }



        }
    }

    private void saveDataToFirestore(
            String email,
            double measuredValue,
            String result,
            double mDBreferenceLength,
            double mDBtolerance,
            String mDBunit
    ) {
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // HistoryItem 객체 생성
        HistoryItem historyItem = new HistoryItem(
                mDBreferenceLength,
                mDBtolerance,
                mDBunit,
                measuredValue,
                result,
                email,
                currentDateTime
        );

        // Firestore에 저장
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(email)
                .collection("measurements")
                .document(currentDateTime)
                .set(historyItem)
                .addOnSuccessListener(aVoid -> Log.d("StandardActivity", "데이터 저장 성공!"))
                .addOnFailureListener(e -> Log.e("StandardActivity", "데이터 저장 실패: " + e));
    }

}
