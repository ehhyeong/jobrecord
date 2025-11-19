// user-home.js
// - 노란 프로필 버튼 클릭 시 드롭다운 토글
// - 로그인한 유저 정보(/api/users/me) 가져와서 이름/이메일 채우기
// - 로그아웃(/auth/logout) 호출 후 home.html로 이동

// 0. API Base (env.js에서 설정했다면 그 값 사용)
const apiBase =
  (window.ENV && window.ENV.API_BASE_URL)
    ? window.ENV.API_BASE_URL
    : '';

// 1. 드롭다운 토글 관련 요소
const profileIcon = document.getElementById('profile-icon');
const profileDropdown = document.getElementById('profile-dropdown');

if (profileIcon && profileDropdown) {
  // 프로필 아이콘 클릭 시 메뉴 토글
  profileIcon.addEventListener('click', function (event) {
    event.stopPropagation();
    profileDropdown.classList.toggle('active');
  });

  // 화면 바깥 아무데나 클릭하면 드롭다운 닫기
  window.addEventListener('click', function (event) {
    if (!profileDropdown.contains(event.target) && !profileIcon.contains(event.target)) {
      profileDropdown.classList.remove('active');
    }
  });
}

// 2. 페이지 로드 후 유저 정보 불러오기 + 로그아웃 설정
document.addEventListener('DOMContentLoaded', function () {
  const nameEl = document.querySelector('.user-name');
  const emailEl = document.querySelector('.user-email');

  // 2-1. 로그인한 사용자 정보 가져오기
  if (nameEl && emailEl) {
    fetch(apiBase + '/api/users/me', {
      method: 'GET',
      credentials: 'include', // 쿠키 같이 전송 (access_token)
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(function (res) {
        // 401/403이면 세션 만료로 보고 그냥 홈으로 돌려보내도 됨 (원하면 아래 주석 해제)
        // if (res.status === 401 || res.status === 403) {
        //   window.location.href = '/home';
        //   return Promise.reject('unauthorized');
        // }
        return res.json();
      })
      .then(function (body) {
        // ApiResponse 형태 가정: { success, data: { name, email, ... } }
        if (!body || !body.success || !body.data) {
          console.warn('사용자 정보를 가져오지 못했습니다.', body);
          return;
        }

        const user = body.data;
        nameEl.textContent = user.name || '';
        emailEl.textContent = user.email || '';
      })
      .catch(function (err) {
        console.error('현재 로그인한 사용자 정보 조회 실패:', err);
      });
  } else {
    console.warn('user-name 또는 user-email 요소를 찾을 수 없습니다.');
  }

  // 2-2. 로그아웃 버튼 클릭 시 처리
  const logoutLink = document.querySelector('.logout');
  if (logoutLink) {
    logoutLink.addEventListener('click', function (event) {
      event.preventDefault();

      fetch(apiBase + '/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
        .catch(function (err) {
          console.warn('로그아웃 요청 실패(그래도 홈으로 이동):', err);
        })
        .finally(function () {
          // 쿠키 정리는 서버에서 하고, 화면은 공개 home.html로 이동
          window.location.href = '/home'; // 공개 홈 경로에 맞게 필요하면 '/' 등으로 변경
        });
    });
  } else {
    console.warn('로그아웃 링크(.logout)를 찾을 수 없습니다.');
  }
});
