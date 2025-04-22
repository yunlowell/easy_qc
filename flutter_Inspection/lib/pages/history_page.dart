import 'package:flutter/material.dart';

class HistoryPage extends StatelessWidget {
  const HistoryPage({super.key});

  @override
  Widget build(BuildContext context) {
    // 샘플 데이터
    final history = [
      {'date': '2024-04-20', 'value': '115.4', 'result': '양품'},
      {'date': '2024-04-21', 'value': '127.1', 'result': '불량'},
    ];
    return ListView.builder(
      itemCount: history.length,
      itemBuilder: (context, index) {
        final item = history[index];
        return ListTile(
          leading: const Icon(Icons.assignment),
          title: Text('측정일: ${item['date']}'),
          subtitle: Text('측정값: ${item['value']} mm'),
          trailing: Text(item['result']!),
        );
      },
    );
  }
}
