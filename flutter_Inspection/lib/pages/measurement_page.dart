import 'package:flutter/material.dart';
import 'package:camera/camera.dart';

class MeasurementPage extends StatefulWidget {
  const MeasurementPage({super.key});

  @override
  State<MeasurementPage> createState() => _MeasurementPageState();
}

class _MeasurementPageState extends State<MeasurementPage> {
  CameraController? _controller;
  bool _isInitialized = false;
  double _rawValue = 100.0;
  double _scaleFactor = 0.1;
  double _actualValue = 0.0;
  final _controllerText = TextEditingController();

  @override
  void initState() {
    super.initState();
    _initCamera();
    _calculateMeasurement();
  }

  Future<void> _initCamera() async {
    final cameras = await availableCameras();
    _controller = CameraController(cameras[0], ResolutionPreset.medium);
    await _controller?.initialize();
    setState(() {
      _isInitialized = true;
    });
  }

  void _calculateMeasurement() {
    _actualValue = _rawValue * _scaleFactor;
  }

  void _applyCorrection() {
    final input = double.tryParse(_controllerText.text);
    if (input != null && _rawValue > 0) {
      setState(() {
        _scaleFactor = input / _rawValue;
        _calculateMeasurement();
      });
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _isInitialized
        ? Column(
            children: [
              AspectRatio(
                aspectRatio: _controller!.value.aspectRatio,
                child: CameraPreview(_controller!),
              ),
              Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('측정값: $_rawValue px'),
                    Text('보정 비율: $_scaleFactor mm/px'),
                    Text('→ 실제: ${_actualValue.toStringAsFixed(2)} mm',
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 10),
                    const Text('실제 측정값 입력 (보정용):'),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _controllerText,
                            decoration: const InputDecoration(
                              labelText: '예: 115.0',
                              border: OutlineInputBorder(),
                            ),
                            keyboardType: TextInputType.number,
                          ),
                        ),
                        const SizedBox(width: 10),
                        ElevatedButton(
                          onPressed: _applyCorrection,
                          child: const Text('보정'),
                        ),
                      ],
                    )
                  ],
                ),
              )
            ],
          )
        : const Center(child: CircularProgressIndicator());
  }
}
