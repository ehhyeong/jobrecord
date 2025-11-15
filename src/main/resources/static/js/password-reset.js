// 2233076 10주차 추가 (login.js 양식 적용)
// (전제) env.js가 먼저 로드되어 window.ENV.API_BASE_URL이 존재해야 함
const API_BASE_URL = window.ENV.API_BASE_URL;

// 1. 필요한 HTML 요소(태그)를 모두 찾습니다.
const formRequest = document.getElementById('form-request');
const formConfirm = document.getElementById('form-confirm');
const messageDiv = document.getElementById('message');

// 1단계 폼 요소
const reqEmailInput = document.getElementById('req-email');
const reqButton = formRequest.querySelector('button'); // 1단계 '인증번호 받기' 버튼

// 2단계 폼 요소
const confEmailInput = document.getElementById('conf-email');
const confCodeInput = document.getElementById('conf-code');
const confPasswordInput = document.getElementById('conf-password');
const confButton = formConfirm.querySelector('button'); // 2단계 '비밀번호 재설정' 버튼

/**
 * 2. 입력칸 변경 감지 (1단계 폼 - 이메일 유효성)
 */
function validateRequestForm() {
    const emailValue = reqEmailInput.value;

    // login.js와 유사한 로직: 이메일이 비어있지 않고 '@'를 포함하면 버튼 활성화
    if (emailValue.length > 0 && emailValue.includes('@')) {
        reqButton.disabled = false;
        reqButton.classList.add('active'); // (CSS 클래스가 있다면)
    } else {
        reqButton.disabled = true;
        reqButton.classList.remove('active');
    }
}

/**
 * 2-2. 입력칸 변경 감지 (2단계 폼 - 모든 필드 유효성)
 */
/**
 * 2-2. 입력칸 변경 감지 (2단계 폼 - 모든 필드 유효성)
 */
function validateConfirmForm() {
    const codeValue = confCodeInput.value;
    const passwordValue = confPasswordInput.value;

    // 2233076 10주차 수정: (특수문자 검증 정규식 추가)
    const specialCharRegex = /[^A-Za-z0-9]/; // (영숫자가 아닌 문자가 1개 이상)

    // 2단계 폼: 코드가 6자리이고, 비번이 8자리 이상이고, 특수문자를 포함할 때 활성화
    if (codeValue.length === 6 &&
        passwordValue.length >= 8 &&
        specialCharRegex.test(passwordValue)) {

        confButton.disabled = false;
        confButton.classList.add('active');
    } else {
        confButton.disabled = true;
        confButton.classList.remove('active');
    }
}

// 3. 1단계 폼 입력칸에 이벤트 리스너 연결
reqEmailInput.addEventListener('input', validateRequestForm);

// 3-2. 2단계 폼 입력칸에 이벤트 리스너 연결
confCodeInput.addEventListener('input', validateConfirmForm);
confPasswordInput.addEventListener('input', validateConfirmForm);


/**
 * 4. (1단계) 인증번호 발송 폼 제출 (fetch API)
 */
formRequest.addEventListener('submit', async (e) => {
    e.preventDefault(); // 폼 기본 제출 방지

    const email = reqEmailInput.value;
    messageDiv.textContent = '서버에 요청 중...';

    try {
        // env.js의 API_BASE_URL 사용
        const response = await fetch(`${API_BASE_URL}/auth/password-reset/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || '알 수 없는 에러');
        }

        // 성공 시
        messageDiv.style.color = 'blue';
        messageDiv.textContent = data.message; // "인증번호가 발송되었습니다..."

        // 2단계 폼 보이기
        formConfirm.style.display = 'block';
        confEmailInput.value = email; // 이메일 자동 채우기
        formRequest.style.display = 'none'; // 1단계 폼 숨기기

        // 2단계 폼 유효성 검사 초기 실행
        validateConfirmForm();

    } catch (error) {
        messageDiv.style.color = 'red';
        messageDiv.textContent = '요청 실패: ' + (error.message || '서버 응답 없음');
    }
});

/**
 * 5. (2단계) 비밀번호 재설정 폼 제출 (fetch API)
 */
formConfirm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = confEmailInput.value;
    const code = confCodeInput.value;
    const newPassword = confPasswordInput.value;

    messageDiv.textContent = '서버에 요청 중...';

    try {
        // env.js의 API_BASE_URL 사용
        const response = await fetch(`${API_BASE_URL}/auth/password-reset/confirm`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email: email,
                code: code,
                newPassword: newPassword
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || '재설정 실패');
        }

        // 최종 성공
        messageDiv.style.color = 'green';
        messageDiv.textContent = data.message; // "비밀번호가 재설정되었습니다."
        formConfirm.style.display = 'none'; // 폼 숨기기

    } catch (error) {
        messageDiv.style.color = 'red';
        messageDiv.textContent = '재설정 실패: ' + (error.message || '서버 응답 없음');
    }
});

// 6. 페이지 로드 시, 버튼 비활성화 (초기 상태)
validateRequestForm();
validateConfirmForm();