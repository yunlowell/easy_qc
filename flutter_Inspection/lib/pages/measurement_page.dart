import 'package:flutter/material.dart'; // Flutter의 Material Design 위젯 라이브러리를 가져옵니다.
import 'package:camera/camera.dart'; // 카메라 기능을 제공하는 패키지를 가져옵니다.

  // 계측 페이지를 정의하는 StatefulWidget입니다.
  class MeasurementPage extends StatefulWidget {
    const MeasurementPage({super.key}); // 생성자에서 key를 초기화합니다.

    @override
    State<MeasurementPage> createState() => _MeasurementPageState(); // 상태를 관리하는 _MeasurementPageState를 생성합니다.
  }

  // MeasurementPage의 상태를 관리하는 클래스입니다.
  class _MeasurementPageState extends State<MeasurementPage> {
    CameraController? _controller; // 카메라 컨트롤러를 관리하는 변수입니다.
    bool _isInitialized = false; // 카메라 초기화 상태를 나타내는 변수입니다.
    double _rawValue = 100.0; // 측정된 원시 값(픽셀 단위)입니다.
    double _scaleFactor = 0.1; // 보정 비율(mm/px)입니다.
    double _actualValue = 0.0; // 보정된 실제 측정값(mm 단위)입니다.
    final _controllerText = TextEditingController(); // 사용자 입력을 처리하기 위한 텍스트 컨트롤러입니다.

    @override
    void initState() {
      super.initState();
      _initCamera(); // 카메라 초기화를 수행합니다.
      _calculateMeasurement(); // 초기 측정값을 계산합니다.
    }

    // 카메라를 초기화하는 비동기 메서드입니다.
    Future<void> _initCamera() async {
      final cameras = await availableCameras(); // 사용 가능한 카메라 목록을 가져옵니다.
      _controller = CameraController(cameras[0], ResolutionPreset.medium); // 첫 번째 카메라를 medium 해상도로 설정합니다.
      await _controller?.initialize(); // 카메라를 초기화합니다.
      setState(() {
        _isInitialized = true; // 초기화 상태를 true로 설정합니다.
      });
    }

    // 측정값을 계산하는 메서드입니다.
    void _calculateMeasurement() {
      _actualValue = _rawValue * _scaleFactor; // 원시 값에 보정 비율을 곱하여 실제 값을 계산합니다.
    }

    // 사용자 입력값을 기반으로 보정 비율을 적용하는 메서드입니다.
    void _applyCorrection() {
      final input = double.tryParse(_controllerText.text); // 입력값을 double로 변환합니다.
      if (input != null && _rawValue > 0) { // 입력값이 유효하고 원시 값이 0보다 큰 경우
        setState(() {
          _scaleFactor = input / _rawValue; // 보정 비율을 계산합니다.
          _calculateMeasurement(); // 새로운 보정 비율로 측정값을 다시 계산합니다.
        });
      }
    }

    @override
    void dispose() {
      _controller?.dispose(); // 카메라 컨트롤러를 해제합니다.
      super.dispose();
    }

    @override
    Widget build(BuildContext context) {
      return _isInitialized
          ? Column(
              children: [
                // 카메라 미리보기를 표시하는 위젯입니다.
                AspectRatio(
                  aspectRatio: _controller!.value.aspectRatio, // 카메라의 종횡비를 설정합니다.
                  child: CameraPreview(_controller!), // 카메라 미리보기를 표시합니다.
                ),
                Padding(
                  padding: const EdgeInsets.all(12), // 내부 여백을 설정합니다.
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start, // 자식 위젯을 왼쪽 정렬합니다.
                    children: [
                      Text('측정값: $_rawValue px'), // 원시 측정값을 표시합니다.
                      Text('보정 비율: $_scaleFactor mm/px'), // 보정 비율을 표시합니다.
                      Text(
                        '→ 실제: ${_actualValue.toStringAsFixed(2)} mm', // 보정된 실제 값을 소수점 2자리까지 표시합니다.
                        style: const TextStyle(fontWeight: FontWeight.bold), // 텍스트를 굵게 표시합니다.
                      ),
                      const SizedBox(height: 10), // 간격을 추가합니다.
                      const Text('실제 측정값 입력 (보정용):'), // 입력 안내 텍스트입니다.
                      Row(
                        children: [
                          Expanded(
                            // 사용자 입력을 받는 텍스트 필드입니다.
                            child: TextField(
                              controller: _controllerText, // 텍스트 컨트롤러를 설정합니다.
                              decoration: const InputDecoration(
                                labelText: '예: 115.0', // 입력 필드의 라벨입니다.
                                border: OutlineInputBorder(), // 입력 필드의 테두리를 설정합니다.
                              ),
                              keyboardType: TextInputType.number, // 숫자 입력 전용 키보드를 표시합니다.
                            ),
                          ),
                          const SizedBox(width: 10), // 간격을 추가합니다.
                          ElevatedButton(
                            onPressed: _applyCorrection, // 버튼 클릭 시 보정 메서드를 호출합니다.
                            child: const Text('보정'), // 버튼 텍스트입니다.
                          ),
                        ],
                      )
                    ],
                  ),
                )
              ],
            )
          : const Center(child: CircularProgressIndicator()); // 카메라 초기화 중 로딩 표시를 합니다.
    }
  }