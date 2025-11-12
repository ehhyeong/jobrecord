// 요소
const termsCheckbox = document.getElementById('terms');
const signupButton = document.querySelector('.signup-button');
const passwordInput = document.getElementById('password');
const passwordConfirmInput = document.getElementById('password-confirm');
const errorMessage = document.getElementById('password-error');
const signupForm = document.getElementById('signup-form');

// 규칙: 8자 이상 + 특수문자 1개 이상
function isStrongPassword(pw) {
  const hasMinLen = pw.length >= 8;
  const hasSpecial = /[!@#$%^&*()_\-+=[\]{};:'"\\|,.<>/?`~]/.test(pw);
  return hasMinLen && hasSpecial;
}

function validateForm() {
  const isTermsChecked = termsCheckbox?.checked ?? false;
  const pw = passwordInput?.value ?? '';
  const pw2 = passwordConfirmInput?.value ?? '';

  const strong = isStrongPassword(pw);
  const isMatch = pw === pw2;

  // 메시지: 규칙위반 > 불일치 > 정상
  if (pw.length > 0 && !strong) {
    errorMessage.textContent = '비밀번호는 8자 이상이며 특수문자 1개 이상을 포함해야 합니다.';
  } else if (pw2.length > 0 && !isMatch) {
    errorMessage.textContent = '비밀번호가 일치하지 않습니다.';
  } else {
    errorMessage.textContent = '';
  }

  const enable = isTermsChecked && strong && isMatch;
  signupButton.disabled = !enable;
  signupButton.classList.toggle('active', enable);
}

// 입력 변화마다 검사
termsCheckbox.addEventListener('change', validateForm);
passwordInput.addEventListener('input', validateForm);
passwordConfirmInput.addEventListener('input', validateForm);

// 제출 가드(그냥 넘어가는 현상 방지)
signupForm.addEventListener('submit', (e) => {
  validateForm();
  if (signupButton.disabled) {
    e.preventDefault();          // 넘어가지 않게 막기
    if (!errorMessage.textContent) {
      errorMessage.textContent = '비밀번호 조건을 확인해 주세요.';
    }
  }
});

// 초기 1회 검사
validateForm();
