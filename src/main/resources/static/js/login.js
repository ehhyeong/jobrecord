// 융합프로젝트 김태형 11주차 Auth 연동 : 로그인 API 호출 및 오류 메시지 처리 (수정)

// 백엔드 API Base URL 설정 (env.js에 정의되어 있으면 사용, 없으면 현재 도메인 사용) (수정)
const apiBase =
  (window.ENV && window.ENV.API_BASE_URL) ? window.ENV.API_BASE_URL : '';

// 1. 로그인 관련 요소
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginButton = document.querySelector('.login-button');
const loginForm = document.querySelector('.login-form');
const loginError = document.getElementById('login-error');

// 2. 로그인 폼 유효성 검사
function validateLoginForm() {
  const emailValue = emailInput ? emailInput.value : '';
  const passwordValue = passwordInput ? passwordInput.value : '';

  if (emailValue.length > 0 && passwordValue.length > 0) {
    if (loginButton) {
      loginButton.disabled = false;
      loginButton.classList.add('active');
    }
  } else {
    if (loginButton) {
      loginButton.disabled = true;
      loginButton.classList.remove('active');
    }
  }
}

// 입력 변화 이벤트
emailInput && emailInput.addEventListener('input', validateLoginForm);
passwordInput && passwordInput.addEventListener('input', validateLoginForm);

// 3. 로그인 폼 제출 → /auth/login
if (loginForm) {
  loginForm.addEventListener('submit', function (e) {
    e.preventDefault();

    validateLoginForm();
    if (loginButton && loginButton.disabled) {
      if (loginError && !loginError.textContent) {
        loginError.textContent = '이메일과 비밀번호를 모두 입력해 주세요.';
      }
      return;
    }

    if (loginError) {
      loginError.textContent = '';
    }

    const payload = {
      email: emailInput ? emailInput.value : '',
      password: passwordInput ? passwordInput.value : ''
    };

    fetch(apiBase + '/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload)
    })
      .then(function (res) {
        const contentType = res.headers.get('content-type') || '';
        const isJson = contentType.includes('application/json');

        if (isJson) {
          return res.json().then(function (body) {
            return { ok: res.ok, status: res.status, body: body };
          });
        }
        return { ok: res.ok, status: res.status, body: null };
      })
      .then(function (res) {
        if (!res.ok) {
          // 융합프로젝트 김태형 11주차 Auth 연동 : 401 응답은 고정 문구로 노출 (보안상 이메일/비번 구분 안 함) (수정)
          let msg = '로그인에 실패했습니다.'; // 기본값

          if (res.status === 401) {
            // 잘못된 이메일/비밀번호 → 항상 동일한 문구
            msg = '이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.';
          } else if (res.body) {
            // 그 외 에러는 서버 메시지 사용 (옵션)
            if (res.body.message) {
              msg = res.body.message;
            } else if (res.body.errorDescription) {
              msg = res.body.errorDescription;
            }
          }

          if (loginError) {
            loginError.textContent = msg;
          } else {
            alert(msg);
          }
          return;
        }

        // 로그인 성공 시 이동
        window.location.href = '/user/home';
      })
      .catch(function (err) {
        console.error(err);
        const msg = '서버와 통신 중 오류가 발생했습니다.';
        if (loginError) {
          loginError.textContent = msg;
        } else {
          alert(msg);
        }
      });
  });
}

// 초기 상태 한 번 맞추기
validateLoginForm();


// =======================================================
// 융합프로젝트 김태형 11주차 비밀번호 찾기 모달 : 모달 열기/닫기 + 1/2단계 폼 초기화 (수정)
// (실제 비밀번호 재설정 fetch 로직은 /js/password-reset.js에서 처리)
// =======================================================

// 모달 관련 요소
const resetLink = document.getElementById('reset-password-link');
const resetModal = document.getElementById('reset-modal');
const resetModalClose = document.getElementById('reset-modal-close');

// password-reset.js 에서 제공하는 상태 초기화 함수 래핑
function initResetPasswordModalState() {
  if (window.resetPasswordModalState) {
    window.resetPasswordModalState();
  }
}

// 모달 열기
function openResetModal(e) {
  if (e) e.preventDefault();
  if (!resetModal) return;

  // 타이머/2단계/메시지 모두 초기화
  initResetPasswordModalState();
  resetModal.classList.add('open');

  // 로그인 폼에 입력된 이메일을 1단계 이메일 칸에 미리 채우기(편의)
  const reqEmailInput = document.getElementById('req-email');
  if (reqEmailInput && emailInput) {
    reqEmailInput.value = emailInput.value;
  }
}

// 모달 닫기
function closeResetModal() {
  if (!resetModal) return;
  resetModal.classList.remove('open');

  // 닫을 때도 상태 정리해 두면, 다음에 열 때 항상 깨끗한 1단계 상태로 시작
  initResetPasswordModalState();
}

// 모달 열기
if (resetLink && resetModal) {
  resetLink.addEventListener('click', openResetModal);
}

// 모달 닫기 (X 버튼)
resetModalClose && resetModalClose.addEventListener('click', function () {
  closeResetModal();
});

// 배경 클릭 시 닫기
if (resetModal) {
  resetModal.addEventListener('click', function (e) {
    if (e.target === resetModal) {
      closeResetModal();
    }
  });
}
