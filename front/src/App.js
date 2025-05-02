import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import SplashPage from './pages/SplashPage';
import LoginPage from './pages/LoginPage';
import HomePage from './pages/HomePage';
import SettingPage from './pages/SettingPage';
import HistoryPage from './pages/HistoryPage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<SplashPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/setting" element={<SettingPage />} />
        <Route path="/history" element={<HistoryPage />} />
      </Routes>
    </Router>
  );
}

export default App;
