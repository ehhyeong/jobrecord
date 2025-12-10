document.addEventListener('DOMContentLoaded', () => {
  // 회원가입 API 호출, 이메일 중복/형식 검사, 오류 메시지 처리 로직 추가

  // 백엔드 API Base URL (env.js에 정의되어 있으면 사용, 없으면 현재 도메인 기준 호출)
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
  const emailError = document.getElementById('email-error');    // 이메일 형식/중복 에러 출력용
  const phoneInput = document.getElementById('phone');          // 전화번호 입력 요소
  const phoneError = document.getElementById('phone-error');    // 전화번호 에러 메시지 출력용

  // 비밀번호 표시/숨김 요소
  const passwordToggle = document.querySelector('.password-toggle');
  const passwordToggleConfirm = document.querySelector('.password-toggle-confirm');
  const capsLockWarning = document.querySelector('.caps-lock-warning');
  const capsLockWarningConfirm = document.querySelector('.caps-lock-warning-confirm');

  // 2233076 13주차 추가: 비밀번호 표시/숨김 토글 기능
  // 비밀번호 표시/숨김 토글
  if (passwordToggle && passwordInput) {
    passwordToggle.addEventListener('click', function() {
      const type = passwordInput.type === 'password' ? 'text' : 'password';
      passwordInput.type = type;
      
      const icon = passwordToggle.querySelector('i');
      if (type === 'password') {
        icon.className = 'fa-regular fa-eye';
      } else {
        icon.className = 'fa-regular fa-eye-slash';
      }
    });
  }

  if (passwordToggleConfirm && passwordConfirmInput) {
    passwordToggleConfirm.addEventListener('click', function() {
      const type = passwordConfirmInput.type === 'password' ? 'text' : 'password';
      passwordConfirmInput.type = type;
      
      const icon = passwordToggleConfirm.querySelector('i');
      if (type === 'password') {
        icon.className = 'fa-regular fa-eye';
      } else {
        icon.className = 'fa-regular fa-eye-slash';
      }
    });
  }

  // 2233076 13주차 추가: Caps Lock 감지 기능
  // Caps Lock 감지 - 비밀번호 필드
  if (passwordInput && capsLockWarning) {
    passwordInput.addEventListener('keydown', function(e) {
      const capsLockOn = e.getModifierState && e.getModifierState('CapsLock');
      capsLockWarning.style.display = capsLockOn ? 'flex' : 'none';
    });
    
    passwordInput.addEventListener('keyup', function(e) {
      const capsLockOn = e.getModifierState && e.getModifierState('CapsLock');
      capsLockWarning.style.display = capsLockOn ? 'flex' : 'none';
    });
    
    passwordInput.addEventListener('blur', function() {
      capsLockWarning.style.display = 'none';
    });
  }

  // Caps Lock 감지 - 비밀번호 확인 필드
  if (passwordConfirmInput && capsLockWarningConfirm) {
    passwordConfirmInput.addEventListener('keydown', function(e) {
      const capsLockOn = e.getModifierState && e.getModifierState('CapsLock');
      capsLockWarningConfirm.style.display = capsLockOn ? 'flex' : 'none';
    });
    
    passwordConfirmInput.addEventListener('keyup', function(e) {
      const capsLockOn = e.getModifierState && e.getModifierState('CapsLock');
      capsLockWarningConfirm.style.display = capsLockOn ? 'flex' : 'none';
    });
    
    passwordConfirmInput.addEventListener('blur', function() {
      capsLockWarningConfirm.style.display = 'none';
    });
  }

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
    emailError,
    phoneInput,
    phoneError
  };

  for (const [k, v] of Object.entries(requiredEls)) {
    if (!v) console.warn(`[signup.js] Missing element: ${k}`);
  }

  // 규칙: 8자 이상 + 특수문자 1개 이상
  function isStrongPassword(pw = '') {
    const hasMinLen = pw.length >= 8;
    const hasSpecial = /[!@#$%^&*()_\-+=[\]{};:'"\\|,.<>/?`~]/.test(pw);
    return hasMinLen && hasSpecial;
  }

  // 이메일 형식 대략 검사
  function isValidEmail(email = '') {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  // 전화번호 숫자만 받아서 010-1234-5678 형식으로 포맷팅
  function formatPhone(value = '') {
    const digits = value.replace(/\D/g, ''); // 숫자 외 제거

    if (digits.length <= 3) {
      return digits;
    }
    if (digits.length <= 7) {
      // 010-1234
      return digits.slice(0, 3) + '-' + digits.slice(3);
    }
    // 010-1234-5678 (최대 11자리까지만)
    return (
      digits.slice(0, 3) +
      '-' +
      digits.slice(3, 7) +
      '-' +
      digits.slice(7, 11)
    );
  }

  // 한국 휴대폰 대략 형식 체크: 01X-XXX(X)-XXXX
  function isValidPhone(phone = '') {
    return /^01[0-9]-\d{3,4}-\d{4}$/.test(phone);
  }

  // 이메일/전화번호 중복 여부 상태 + 디바운스 타이머
  let emailDuplicate = false;   // 이메일 중복이면 true
  let emailCheckTimer = null;
  let phoneDuplicate = false;   // 전화번호 중복이면 true
  let phoneCheckTimer = null;

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
    const phone = phoneInput ? phoneInput.value.trim() : '';

    // 사용자가 뭔가 입력을 하기 시작했는지 여부 (전화번호 에러 표시 기준)
    const anyUserInput =
      !!email || !!name || !!phone || pw.length > 0 || pw2.length > 0 || isTermsChecked;

    // 전화번호 필수 + 형식 검사 (중복일 때는 중복 문구를 유지)
    if (phoneError) {
      if (phoneDuplicate) {
        // 중복 체크에서 이미 "이미 존재하는 전화번호입니다."를 넣어둔 상태면 건드리지 않음
      } else if (!phone && anyUserInput) {
        phoneError.textContent = '전화번호를 입력해주세요.';
      } else if (phone && !isValidPhone(phone)) {
        phoneError.textContent = '올바른 전화번호 형식이 아닙니다.';
      } else {
        phoneError.textContent = '';
      }
    }

    // 이메일 형식도 맞고, 중복도 아니어야 이메일 OK
    const emailOk =
      !!email &&
      isValidEmail(email) &&
      !emailDuplicate;

    // 전화번호: 값 + 형식 + 중복 X
    const phoneOk = !!phone && isValidPhone(phone) && !phoneDuplicate;

    const enable = isTermsChecked && strong && isMatch && emailOk && !!name && phoneOk;
    if (signupButton) {
      signupButton.disabled = !enable;
      signupButton.classList.toggle('active', enable);
    }
  }

  // 이메일 중복/형식 체크 (디바운스 후 서버 호출)
  function checkEmailStatus() {
    if (!emailInput || !emailError) return;

    const email = emailInput.value.trim();
    emailDuplicate = false;

    // 비어 있으면 상태/메시지 초기화
    if (!email) {
      emailError.textContent = '';
      validateForm();
      return;
    }

    // 형식이 잘못된 경우 프론트에서 먼저 막기
    if (!isValidEmail(email)) {
      emailError.textContent = '올바른 이메일 형식이 아닙니다.';
      emailDuplicate = true;   // 버튼 비활성화 위해 true 처리
      validateForm();
      return;
    }

    // 서버 확인 중 메시지 
    emailError.textContent = '이메일을 확인 중입니다...';

    // 디바운스: 마지막 입력 후 300ms 뒤에만 실제 서버 요청
    if (emailCheckTimer) {
      clearTimeout(emailCheckTimer);
    }
    emailCheckTimer = setTimeout(() => {
      fetch(apiBase + '/auth/check-email?email=' + encodeURIComponent(email), {
        method: 'GET',
        credentials: 'include'
      })
        .then((res) => res.json())
        .then((body) => {
          emailDuplicate = !!body.exists;
          if (emailDuplicate) {
            emailError.textContent = '중복된 이메일입니다.'; // 빨간색으로 표시
          } else {
            emailError.textContent = ''; // 사용 가능
          }
          validateForm();
        })
        .catch((err) => {
          console.error(err);
          emailError.textContent = '이메일 중복 확인 중 오류가 발생했습니다.';
          // 안전하게 가입 버튼 막아두기
          emailDuplicate = true;
          validateForm();
        });
    }, 300);
  }

  // 전화번호 중복 체크 (디바운스 + 서버 호출)
  function checkPhoneStatus() {
    if (!phoneInput || !phoneError) return;

    const phone = phoneInput.value.trim();
    phoneDuplicate = false;

    // 비어 있으면: "입력해주세요" 같은 건 validateForm에서 처리
    if (!phone) {
      validateForm();
      return;
    }

    // 형식이 잘못되면 서버 안 타고 프론트에서만 막기
    if (!isValidPhone(phone)) {
      validateForm();
      return;
    }

    phoneError.textContent = '전화번호를 확인 중입니다...';

    if (phoneCheckTimer) {
      clearTimeout(phoneCheckTimer);
    }
    phoneCheckTimer = setTimeout(() => {
      fetch(apiBase + '/auth/check-phone?phone=' + encodeURIComponent(phone), {
        method: 'GET',
        credentials: 'include'
      })
        .then((res) => res.json())
        .then((body) => {
          phoneDuplicate = !!body.exists;
          if (phoneDuplicate) {
            phoneError.textContent = '이미 존재하는 전화번호입니다.'; // ← 요구사항
          } else {
            phoneError.textContent = '';
          }
          validateForm();
        })
        .catch((err) => {
          console.error(err);
          phoneError.textContent = '전화번호 중복 확인 중 오류가 발생했습니다.';
          // 안전하게 가입 버튼 막아두기
          phoneDuplicate = true;
          validateForm();
        });
    }, 300);
  }

  // 입력 변화마다 검사
  termsCheckbox && termsCheckbox.addEventListener('change', validateForm);
  passwordInput && passwordInput.addEventListener('input', validateForm);
  passwordConfirmInput && passwordConfirmInput.addEventListener('input', validateForm);

  // 이메일 입력 시: 기존 validateForm 대신, 중복/형식 체크 + 폼 재검증
  emailInput && emailInput.addEventListener('input', () => {
    if (signupError) signupError.textContent = '';           // 서버 에러 초기화
    checkEmailStatus();
  });

  // 이름은 그냥 비어있는지만 체크
  nameInput && nameInput.addEventListener('input', validateForm);

  // 전화번호 입력 시: 자동 하이픈 + 중복/형식 체크
  phoneInput && phoneInput.addEventListener('input', (e) => {
    const formatted = formatPhone(e.target.value);
    e.target.value = formatted;          // 01012341234 → 010-1234-1234 로 자동 변환
    if (signupError) signupError.textContent = '';
    checkPhoneStatus();
  });

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

    if (!emailInput || !passwordInput || !nameInput || !phoneInput) {
      alert('필수 입력 항목을 찾을 수 없습니다.');
      return;
    }

    // 서버로 보낼 데이터 (전화번호 포함)
    const payload = {
      email: emailInput.value.trim(),
      password: passwordInput.value,
      name: nameInput.value.trim(),
      phone: phoneInput.value.trim()
    };

    // 이전 에러 메시지 초기화
    if (signupError) {
      signupError.textContent = '';
    }

    // /auth/signup 엔드포인트 호출
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
