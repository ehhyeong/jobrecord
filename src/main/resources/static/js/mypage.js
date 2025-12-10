// password-reset.js에서 선언한 API_BASE_URL을 사용

// 2233076 13주차 추가: JWT 토큰 만료 시 로그인 페이지 리다이렉트
// 공통 fetch 헬퍼 (401/403 자동 처리)
async function authFetch(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        credentials: 'include' // 쿠키 자동 전송
    });

    // 토큰 만료 또는 인증 실패 시
    if (response.status === 401 || response.status === 403) {
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
        window.location.href = '/auth/login';
        throw new Error('unauthorized');
    }

    return response;
}

// 1. 페이지 로드 시 내 정보 조회
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await authFetch(`${API_BASE_URL}/api/users/me`, {
            method: 'GET'
        });

        if (!response.ok) throw new Error('정보 조회 실패');

        const result = await response.json();
        const user = result.data; // UserDto.Response 구조에 맞춤

        // 화면에 데이터 바인딩
        document.getElementById('user-name').textContent = user.name;
        document.getElementById('user-email').textContent = user.email;
        document.getElementById('user-phone').textContent = user.phone || '(미등록)';
        
        // 날짜 포맷팅 (ISO 문자열을 Date 객체로 변환)
        const formatDate = (dateString) => {
            if (!dateString) return '-';
            try {
                const date = new Date(dateString);
                if (isNaN(date.getTime())) return '-';
                return date.toLocaleDateString('ko-KR');
            } catch (e) {
                return '-';
            }
        };
        
        document.getElementById('user-created').textContent = formatDate(user.createdAt);
        document.getElementById('user-updated').textContent = formatDate(user.updatedAt);

    } catch (error) {
        console.error(error);
        // authFetch에서 이미 처리됨
    }
});

// 2. 회원 탈퇴 로직
async function requestWithdraw() {
    const password = document.getElementById('withdraw-password').value;
    if (!password) {
        alert('비밀번호를 입력해주세요.');
        return;
    }

    try {
        const response = await authFetch(`${API_BASE_URL}/api/users/me`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ password: password })
        });

        const result = await response.json();

        if (response.ok) {
            alert('회원 탈퇴가 완료되었습니다.');
            // 서버에서 로그아웃 처리하여 쿠키 정리
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                credentials: 'include'
            }).catch(() => {});
            window.location.href = '/';
        } else {
            alert(result.message || '탈퇴 실패: 비밀번호를 확인해주세요.');
        }
    } catch (error) {
        console.error(error);
        if (error.message !== 'unauthorized') {
            alert('서버 오류가 발생했습니다.');
        }
    }
}

// --- 유틸리티 함수들 ---

// 로그아웃
window.logout = function logout() {
    fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include'
    })
    .catch(() => {})
    .finally(() => {
        window.location.href = '/';
    });
}

// 모달 제어
window.openWithdrawModal = function openWithdrawModal() {
    document.getElementById('withdraw-modal').style.display = 'flex';
}

window.closeWithdrawModal = function closeWithdrawModal() {
    document.getElementById('withdraw-modal').style.display = 'none';
}

window.openPasswordModal = function openPasswordModal() {
    const modal = document.getElementById('password-reset-modal');
    if (modal) {
        modal.style.display = 'flex';
        
        // 2233076 13주차 추가: 마이페이지에서 이메일을 자동으로 채우고 수정 불가능하게 설정
        // 현재 로그인한 사용자의 이메일을 미리 채우고 수정 불가능하게 설정
        const userEmail = document.getElementById('user-email');
        const reqEmail = document.getElementById('req-email');
        if (userEmail && reqEmail) {
            reqEmail.value = userEmail.textContent;
            reqEmail.readOnly = true; // 수정 불가능하게 설정
        }
        
        // 비밀번호 재설정 모달 상태 초기화
        if (window.resetPasswordModalState) {
            window.resetPasswordModalState();
        }
    }
}

window.closePasswordModal = function closePasswordModal() {
    const modal = document.getElementById('password-reset-modal');
    if (modal) {
        modal.style.display = 'none';
        
        // 모달 내용 초기화
        if (window.resetPasswordModalState) {
            window.resetPasswordModalState();
        }
    }
}

// requestWithdraw도 전역으로 노출
window.requestWithdraw = requestWithdraw;