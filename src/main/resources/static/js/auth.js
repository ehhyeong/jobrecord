// /js/auth.js
// - 모든 페이지에서 공통으로 쓸 인증/API 유틸
// - 쿠키 전송(credentials: 'include') + 401/403 처리까지 여기서

(function () {
  // env.js에 API_BASE_URL이 있으면 그 값 쓰고, 없으면 현재 도메인
  const apiBase =
    (window.ENV && window.ENV.API_BASE_URL) ? window.ENV.API_BASE_URL : '';

  // 2233076 13주차 추가: JWT 토큰 만료 시 로그인 페이지 리다이렉트
  // 공통 fetch 래퍼
  function apiFetch(path, options = {}) {
    const finalOptions = {
      method: options.method || 'GET',
      credentials: 'include', // 쿠키 자동 포함
      headers: {
        'Accept': 'application/json',
        ...(options.headers || {})
      },
      body: options.body
    };

    return fetch(apiBase + path, finalOptions)
      .then(function (res) {
        // 인증 안 된 상태(토큰 만료 포함) → 공통으로 로그인 페이지로 돌려보냄
        if (res.status === 401 || res.status === 403) {
          // 현재 로그인 페이지가 아닐 때만 알림 표시
          if (!window.location.pathname.includes('/auth/login')) {
            alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
          }
          window.location.href = '/auth/login';
          return Promise.reject(new Error('unauthorized'));
        }
        return res;
      });
  }

  // 현재 로그인한 사용자 정보 가져오기
  function fetchMe() {
    return apiFetch('/api/users/me')
      .then(function (res) { return res.json(); })
      .then(function (body) {
        if (!body || !body.success || !body.data) {
          return null;
        }
        return body.data; // { name, email, ... }
      });
  }

  // 로그아웃
  function logout() {
    return apiFetch('/auth/logout', { method: 'POST' });
  }

  // 전역으로 노출
  window.Auth = {
    apiBase,
    apiFetch,
    fetchMe,
    logout
  };
})();
