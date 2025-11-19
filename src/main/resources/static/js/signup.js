// /static/js/signup.js
document.addEventListener('DOMContentLoaded', () => {
  // 융합프로젝트 김태형 11주차 Auth 연동 + 이메일 중복체크 :
  // 회원가입 API 호출, 이메일 중복/형식 검사, 오류 메시지 처리 로직 추가 (추가)

  // 백엔드 API Base URL (env.js에 정의되어 있으면 사용, 없으면 현재 도메인 기준 호출) (추가)
  const apiBase =
    (window.ENV && window.ENV.API_BASE_URL) ? window.ENV.API_BASE_URL : '';

  // 요소
  const termsCheckbox = document.getElementById('terms');
  const signupButton = document.querySelector('.signup-button');
  const passwordInput = document.getElementById('password');
  const passwordConfirmInput = document.getElementById('password-confirm');
  const errorMessage = document.getElementById('password-error');
  const signupForm = document.getElementById('signup-form');

  // 입력값 추가 요소
  const emailInput = document.getElementById('email');          // (기존 + 중복체크에 사용)
  const nameInput = document.getElementById('name');            // (기존)
  const signupError = document.getElementById('signup-error');  // 서버 응답 에러 출력용 (기존)
  const emailError = document.getElementById('email-error');    // 이메일 형식/중복 에러 출력용 (추가)

  // 필수 요소 체크
  const requiredEls = {
    termsCheckbox,
    signupButton,
    passwordInput,
    passwordConfirmInput,
    errorMessage,
    signupForm,
    emailInput,
    nameInput,
    signupError,
    emailError  // (추가)
  }; // (수정)

  for (const [k, v] of Object.entries(requiredEls)) {
    if (!v) console.warn(`[signup.js] Missing element: ${k}`);
  }

  // 규칙: 8자 이상 + 특수문자 1개 이상
  function isStrongPassword(pw = '') {
    const hasMinLen = pw.length >= 8;
    const hasSpecial = /[!@#$%^&*()_\-+=[\]{};:'"\\|,.<>/?`~]/.test(pw);
    return hasMinLen && hasSpecial;
  }

  // 이메일 형식 대략 검사 (추가)
  function isValidEmail(email = '') { // (추가)
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email); // (추가)
  }

  // 이메일 중복 여부 상태 + 디바운스 타이머 (추가)
  let emailDuplicate = false;  // 중복이면 true (추가)
  let emailCheckTimer = null;  // 마지막 입력 후 잠시 기다렸다가 서버 호출 (추가)

  // 폼 전체 유효성 검사
  function validateForm() {
    const isTermsChecked = !!(termsCheckbox && termsCheckbox.checked);
    const pw  = passwordInput ? passwordInput.value : '';
    const pw2 = passwordConfirmInput ? passwordConfirmInput.value : '';

    const strong = isStrongPassword(pw);
    const isMatch = pw === pw2;

    if (errorMessage) {
      if (pw.length > 0 && !strong) {
        errorMessage.textContent =
          '비밀번호는 8자 이상이며 특수문자 1개 이상을 포함해야 합니다.';
      } else if (pw2.length > 0 && !isMatch) {
        errorMessage.textContent = '비밀번호가 일치하지 않습니다.';
      } else {
        errorMessage.textContent = '';
      }
    }

    const email = emailInput ? emailInput.value.trim() : '';
    const name  = nameInput ? nameInput.value.trim() : '';

    // 이메일 형식도 맞고, 중복도 아니어야 이메일 OK (추가)
    const emailOk =
      !!email &&
      isValidEmail(email) &&
      !emailDuplicate; // (추가)

    const enable = isTermsChecked && strong && isMatch && emailOk && !!name; // (수정)
    if (signupButton) {
      signupButton.disabled = !enable;
      signupButton.classList.toggle('active', enable);
    }
  }

  // 이메일 중복/형식 체크 (디바운스 후 서버 호출) (추가)
  function checkEmailStatus() { // (추가)
    if (!emailInput || !emailError) return; // (추가)

    const email = emailInput.value.trim();  // (추가)
    emailDuplicate = false;                 // 기본값 (추가)

    // 비어 있으면 상태/메시지 초기화 (추가)
    if (!email) {
      emailError.textContent = ''; // (추가)
      validateForm();              // (추가)
      return;                      // (추가)
    }

    // 형식이 잘못된 경우 프론트에서 먼저 막기 (추가)
    if (!isValidEmail(email)) { // (추가)
      emailError.textContent = '올바른 이메일 형식이 아닙니다.'; // (추가)
      emailDuplicate = true;   // 버튼 비활성화 위해 true 처리 (추가)
      validateForm();          // (추가)
      return;                  // (추가)
    }

    // 서버 확인 중 메시지 (선택 사항) (추가)
    emailError.textContent = '이메일을 확인 중입니다...'; // (추가)

    // 디바운스: 마지막 입력 후 300ms 뒤에만 실제 서버 요청 (추가)
    if (emailCheckTimer) {
      clearTimeout(emailCheckTimer); // (추가)
    }
    emailCheckTimer = setTimeout(() => { // (추가)
      fetch(apiBase + '/auth/check-email?email=' + encodeURIComponent(email), {
        method: 'GET',
        credentials: 'include'
      })
        .then((res) => res.json())
        .then((body) => {
          emailDuplicate = !!body.exists;
          if (emailDuplicate) {
            emailError.textContent = '중복된 이메일입니다.'; // 빨간색으로 표시 (추가)
          } else {
            emailError.textContent = ''; // 사용 가능 (추가)
          }
          validateForm();
        })
        .catch((err) => {
          console.error(err);
          emailError.textContent = '이메일 중복 확인 중 오류가 발생했습니다.'; // (추가)
          // 안전하게 가입 버튼 막아두기
          emailDuplicate = true; // (추가)
          validateForm();
        });
    }, 300);
  }

  // 입력 변화마다 검사
  termsCheckbox && termsCheckbox.addEventListener('change', validateForm);
  passwordInput && passwordInput.addEventListener('input', validateForm);
  passwordConfirmInput && passwordConfirmInput.addEventListener('input', validateForm);

  // 이메일 입력 시: 기존 validateForm 대신, 중복/형식 체크 + 폼 재검증 (수정)
  emailInput && emailInput.addEventListener('input', () => { // (수정)
    if (signupError) signupError.textContent = '';           // 서버 에러 초기화 (추가)
    checkEmailStatus();                                     // (추가)
  });

  nameInput && nameInput.addEventListener('input', validateForm);

  // 제출 + 실제 회원가입 API 호출
  signupForm && signupForm.addEventListener('submit', (e) => {
    // 기존에는 단순 가드만 있었는데, 실제 API 호출하도록 로직 확장 (기존 설명 유지)
    e.preventDefault(); // 기본 form submit 막기

    // 기존 검증 먼저
    validateForm();
    if (signupButton && signupButton.disabled) {
      if (errorMessage && !errorMessage.textContent) {
        errorMessage.textContent = '비밀번호 조건을 확인해 주세요.';
      }
      return;
    }

    if (!emailInput || !passwordInput || !nameInput) {
      alert('필수 입력 항목을 찾을 수 없습니다.');
      return;
    }

    // 서버로 보낼 데이터
    const payload = {
      email: emailInput.value.trim(),
      password: passwordInput.value,
      name: nameInput.value.trim()
    };

    // 이전 에러 메시지 초기화
    if (signupError) {
      signupError.textContent = '';
    }

    // 융합프로젝트 김태형 11주차 Auth 연동 : /auth/signup 엔드포인트 호출 (기존)
    fetch(apiBase + '/auth/signup', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(payload)
    })
      .then((res) => {
        const contentType = res.headers.get('content-type') || '';
        const isJson = contentType.includes('application/json');

        if (isJson) {
          return res.json().then((body) => ({
            ok: res.ok,
            status: res.status,
            body
          }));
        }
        return { ok: res.ok, status: res.status, body: null };
      })
      .then((res) => {
        if (!res.ok) {
          let msg = '회원가입에 실패했습니다.';

          if (res.body) {
            if (res.body.message) {
              msg = res.body.message;
            } else if (res.body.errorDescription) {
              msg = res.body.errorDescription;
            }
          }

          if (signupError) {
            signupError.textContent = msg;
          } else {
            alert(msg);
          }
          return;
        }

        // 성공 시 알림 후 로그인 페이지로 이동
        alert('회원가입이 완료되었습니다. 이제 로그인해 주세요.');
        window.location.href = '/auth/login'; // 로그인 페이지 경로에 맞게 조정 가능
      })
      .catch((err) => {
        console.error(err);
        const msg = '서버와 통신 중 오류가 발생했습니다.';
        if (signupError) {
          signupError.textContent = msg;
        } else {
          alert(msg);
        }
      });
  });

  // 초기 1회
  validateForm();
});
