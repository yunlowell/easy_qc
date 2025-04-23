import 'package:flutter/material.dart'; // Flutter의 Material Design 위젯 라이브러리를 가져옵니다.
import 'pages/measurement_page.dart'; // 계측 페이지를 정의한 파일을 가져옵니다.
import 'pages/inspection_page.dart'; // 검수 페이지를 정의한 파일을 가져옵니다.
import 'pages/history_page.dart'; // 기록 페이지를 정의한 파일을 가져옵니다.

// 애플리케이션의 진입점입니다.
void main() {
  runApp(const MyApp()); // MyApp 위젯을 실행하여 애플리케이션을 시작합니다.
}

// 애플리케이션의 루트 위젯을 정의하는 클래스입니다.
class MyApp extends StatelessWidget {
  const MyApp({super.key}); // 생성자에서 key를 초기화합니다.

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Inspection App', // 애플리케이션의 제목입니다.
      theme: ThemeData(
        primarySwatch: Colors.blue, // 기본 색상 팔레트를 파란색으로 설정합니다.
        useMaterial3: true, // Material Design 3를 사용하도록 설정합니다.
      ),
      home: const HomePage(), // 애플리케이션의 첫 화면으로 HomePage 위젯을 설정합니다.
    );
  }
}

// 애플리케이션의 홈 화면을 정의하는 StatefulWidget입니다.
class HomePage extends StatefulWidget {
  const HomePage({super.key}); // 생성자에서 key를 초기화합니다.

  @override
  State<HomePage> createState() => _HomePageState(); // 상태를 관리하는 _HomePageState를 생성합니다.
}

// HomePage의 상태를 관리하는 클래스입니다.
class _HomePageState extends State<HomePage> {
  int _selectedIndex = 0; // 현재 선택된 하단 네비게이션 바의 인덱스를 저장합니다.

  // 각 네비게이션 탭에 해당하는 페이지 위젯 리스트입니다.
  final List<Widget> _pages = const [
    MeasurementPage(), // 계측 페이지
    InspectionPage(), // 검수 페이지
    HistoryPage(), // 기록 페이지
  ];

  // 하단 네비게이션 바의 아이템이 선택되었을 때 호출되는 메서드입니다.
  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index; // 선택된 인덱스를 업데이트합니다.
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('제품 계측 및 검수 시스템'), // 상단 앱바에 제목을 설정합니다.
      ),
      body: _pages[_selectedIndex], // 현재 선택된 페이지를 표시합니다.
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex, // 현재 선택된 인덱스를 설정합니다.
        onTap: _onItemTapped, // 아이템이 탭되었을 때 호출할 메서드를 설정합니다.
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.straighten), // 계측 탭의 아이콘
            label: '계측', // 계측 탭의 라벨
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.check), // 검수 탭의 아이콘
            label: '검수', // 검수 탭의 라벨
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.history), // 기록 탭의 아이콘
            label: '기록', // 기록 탭의 라벨
          ),
        ],
      ),
    );
  }
}