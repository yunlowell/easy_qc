import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function GoogleCallbackPage() {
    const navigate = useNavigate();

    useEffect(() => {
        const tokenFromQuery = new URLSearchParams(window.location.search).get('firebase_token');

        if (tokenFromQuery) {
            localStorage.setItem('token', tokenFromQuery);
            navigate('/home');
        } else {
            alert('토큰이 전달되지 않았습니다.');
        }
    }, [navigate]);

    return (
        <div style={{ textAlign: 'center', marginTop: '100px' }}>
            <h3>구글 로그인 처리 중입니다...</h3>
        </div>
    );
}

export default GoogleCallbackPage;
