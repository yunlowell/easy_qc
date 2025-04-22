import 'package:flutter/material.dart';

class InspectionPage extends StatefulWidget {
  const InspectionPage({super.key});

  @override
  State<InspectionPage> createState() => _InspectionPageState();
}

class _InspectionPageState extends State<InspectionPage> {
  final _valueController = TextEditingController();
  final _standard = 120.0;
  final _tolerance = 5.0;
  String _result = '';

  void _check() {
    final input = double.tryParse(_valueController.text);
    if (input != null) {
      if ((input - _standard).abs() <= _tolerance) {
        setState(() => _result = '양품');
      } else {
        setState(() => _result = '불량');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Text('기준치: $_standard mm / 허용오차: ±$_tolerance mm'),
          const SizedBox(height: 10),
          TextField(
            controller: _valueController,
            decoration: const InputDecoration(
              labelText: '측정값 입력',
              border: OutlineInputBorder(),
            ),
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 10),
          ElevatedButton(onPressed: _check, child: const Text('검수')),
          const SizedBox(height: 10),
          Text('판정 결과: $_result',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
}
