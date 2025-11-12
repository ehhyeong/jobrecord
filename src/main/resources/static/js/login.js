// 1. 필요한 HTML 요소(태그)를 모두 찾습니다.
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginButton = document.querySelector('.login-button');

// 2. 입력칸의 변경을 감지할 함수를 만듭니다.
function validateLoginForm() {
    // 2-1. 현재 값들을 가져옵니다.
    const emailValue = emailInput.value;
    const passwordValue = passwordInput.value;

    // 3. [로그인 버튼]을 처리합니다.
    // (이메일이 비어있지 않고, 비밀번호도 비어있지 않을 때)
    if (emailValue.length > 0 && passwordValue.length > 0) {
        // 모든 조건을 만족하면 버튼 활성화
        loginButton.disabled = false;
        loginButton.classList.add('active');
    } else {
        // 하나라도 비어있으면 버튼 비활성화
        loginButton.disabled = true;
        loginButton.classList.remove('active');
    }
}

// 4. 2개의 입력칸(이메일, 비번) 중 하나라도 글자가 입력되면
//    무조건 2번의 validateLoginForm() 함수를 실행하도록 연결합니다.
emailInput.addEventListener('input', validateLoginForm);
passwordInput.addEventListener('input', validateLoginForm);