// 비밀번호 찾기(모달) - 인증번호 발송/검증 + 3분 타이머 + 비밀번호 재설정

const API_BASE_URL =
  (window.ENV && window.ENV.API_BASE_URL) ? window.ENV.API_BASE_URL : "";

// 1단계 요소
const formRequest = document.getElementById("form-request");
const reqEmailInput = document.getElementById("req-email");
const requestCodeButton =
  document.getElementById("request-code-button") ||
  (formRequest ? formRequest.querySelector("button") : null);
const resetEmailError = document.getElementById("reset-email-error");

// 2단계 요소
const formConfirm = document.getElementById("form-confirm");
const confCodeInput = document.getElementById("conf-code");
const confPasswordInput = document.getElementById("conf-password");
const verifyCodeButton = document.getElementById("verify-code-button");
const confirmButton = document.getElementById("confirm-reset-password-button");
const codeMessage = document.getElementById("code-message");
// 융합프로젝트 김태형 11주차 비밀번호 재설정(OpenAPI 스펙 확정) : 새 비밀번호 에러 메시지 요소 (추가)
const confPasswordError = document.getElementById("conf-password-error"); // (추가)
const resendCodeButton = document.getElementById("resend-code-button");   // (추가)

// 공통 메시지(파란 글씨 + 타이머 같이 사용)
const messageDiv = document.getElementById("message");

// 상태값
let resetTimerId = null;
let resetRemainingSec = 0;
let isCodeVerified = false;
let baseTimerMessage = ""; // "인증번호가 이메일로 발송되었습니다." 같은 기본 문구

// ===================== 타이머 관련 =====================

function renderResetTimerMessage() {
  if (!messageDiv || !baseTimerMessage) return;

  if (resetRemainingSec <= 0) {
    // 시간 끝나면 기본 문구만 남기거나, 이후 로직에서 덮어씀
    messageDiv.style.color = "blue";
    messageDiv.textContent = baseTimerMessage;
    return;
  }

  const m = Math.floor(resetRemainingSec / 60);
  const s = resetRemainingSec % 60;
  const mmss = `${m}:${s.toString().padStart(2, "0")}`;

  messageDiv.style.color = "blue";
  messageDiv.textContent = `${baseTimerMessage} 남은 시간 ${mmss}`;
}

function startResetTimer(sec) {
  if (resetTimerId !== null) {
    clearInterval(resetTimerId);
    resetTimerId = null;
  }

  resetRemainingSec = sec;
  renderResetTimerMessage();

  resetTimerId = setInterval(() => {
    resetRemainingSec -= 1;

    if (resetRemainingSec <= 0) {
      clearInterval(resetTimerId);
      resetTimerId = null;
      isCodeVerified = false;
      baseTimerMessage = "";

      if (messageDiv) {
        messageDiv.style.color = "#d32f2f";
        messageDiv.textContent =
          "인증번호 유효 시간이 만료되었습니다. 다시 요청해 주세요.";
      }

      if (verifyCodeButton) verifyCodeButton.disabled = true;
      if (confCodeInput) confCodeInput.disabled = false;

      validateConfirmForm();
      return;
    }

    renderResetTimerMessage();
  }, 1000);
}

// ===================== 모달 상태 초기화 =====================

function resetPasswordModalState() {
  // 타이머 정리
  if (resetTimerId !== null) {
    clearInterval(resetTimerId);
    resetTimerId = null;
  }
  resetRemainingSec = 0;
  isCodeVerified = false;
  baseTimerMessage = "";
  renderResetTimerMessage();

  // 1단계 초기화
  if (formRequest) {
    formRequest.style.display = "flex";
  }
  if (reqEmailInput) reqEmailInput.value = "";
  if (resetEmailError) resetEmailError.textContent = "";

  // 2단계 초기화
  if (formConfirm) formConfirm.style.display = "none";
  if (confCodeInput) {
    confCodeInput.value = "";
    confCodeInput.disabled = false;
  }
  if (confPasswordInput) confPasswordInput.value = "";

  if (verifyCodeButton) {
    verifyCodeButton.disabled = true;
    verifyCodeButton.classList.remove("active");
  }
  if (resendCodeButton) {                           // (추가)
    resendCodeButton.disabled = true;              // (추가)
    resendCodeButton.classList.remove("active");   // (추가)
  }
  if (confirmButton) {
    confirmButton.disabled = true;           // 2단계 열릴 때 다시 켜줌 (아래에서) (수정)
    confirmButton.classList.remove("active");
  }

  if (codeMessage) {
    codeMessage.textContent = "";
    codeMessage.style.color = "#d32f2f";
  }
  if (confPasswordError) {
    confPasswordError.textContent = "";
    confPasswordError.style.color = "#d32f2f";
  }
  if (messageDiv) {
    messageDiv.textContent = "";
    messageDiv.style.color = "#d32f2f";
  }
}

// login.js 에서 호출할 수 있게 전역에 노출
window.resetPasswordModalState = resetPasswordModalState;

// ===================== 유효성 검사 =====================

// 융합프로젝트 김태형 11주차 비밀번호 재설정(OpenAPI 스펙 확정) : 버튼 활성/비활성은 입력 여부만 사용 (수정)
function validateConfirmForm() {
  if (!confCodeInput || !confPasswordInput || !confirmButton) return;

  const code = confCodeInput.value.trim();
  const password = confPasswordInput.value;

  const hasInput = code.length > 0 || password.length > 0;
  // 버튼은 항상 클릭 가능, 단 2단계가 열릴 때만 enabled (submit에서 검증) (수정)
  confirmButton.classList.toggle("active", hasInput);
}

// ===================== 이벤트 바인딩 =====================

// 1단계: 이메일 입력시 에러 문구 제거
reqEmailInput &&
  reqEmailInput.addEventListener("input", () => {
    if (resetEmailError) resetEmailError.textContent = "";
  });

// 1단계: 인증번호 받기 (버튼 클릭)
requestCodeButton &&
  requestCodeButton.addEventListener("click", async () => {
    if (!reqEmailInput) return;
    const email = reqEmailInput.value.trim();

    if (!email || !email.includes("@")) {
      if (resetEmailError) {
        resetEmailError.textContent = "올바른 이메일 형식을 입력해 주세요.";
      }
      return;
    }

    if (messageDiv) {
      messageDiv.style.color = "#d32f2f";
      messageDiv.textContent = "서버에 요청 중...";
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/password-reset/request`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const msg = data.message || "요청이 실패했습니다.";
        throw new Error(msg);
      }

      // 2단계 표시
      if (formConfirm) formConfirm.style.display = "flex";
      if (formRequest) formRequest.style.display = "none";
      if (resendCodeButton) resendCodeButton.disabled = false; // (추가)

      // 2단계 진입 시 재설정 버튼 클릭 가능하게 변경 (여기서만 enable) (추가)
      if (confirmButton) confirmButton.disabled = false; // (추가)

      // 파란 문구 + 타이머 같이
      baseTimerMessage = "인증번호가 이메일로 발송되었습니다.";
      renderResetTimerMessage();   // 기본 문구 먼저 찍고
      startResetTimer(180);        // 타이머 시작

      if (verifyCodeButton) verifyCodeButton.disabled = false;
    } catch (err) {
      baseTimerMessage = "";
      if (messageDiv) {
        messageDiv.style.color = "#d32f2f";
        messageDiv.textContent =
          "요청 실패: " + (err.message || "서버 응답 없음");
      }
    }
  });

// 2단계: 인증번호 입력
confCodeInput &&
  confCodeInput.addEventListener("input", () => {
    isCodeVerified = false;

    if (codeMessage) {
      codeMessage.textContent = "";
      codeMessage.style.color = "#d32f2f";
    }
    if (confPasswordError) {
      confPasswordError.textContent = "";
    }

    if (verifyCodeButton) {
      verifyCodeButton.disabled = !(resetTimerId !== null && resetRemainingSec > 0);
    }

    validateConfirmForm();
  });

// 2단계: 새 비밀번호 입력
confPasswordInput &&
  confPasswordInput.addEventListener("input", () => {
    if (confPasswordError) confPasswordError.textContent = "";
    validateConfirmForm();
  });

// 2단계: 인증번호 확인 버튼 클릭
verifyCodeButton &&
  verifyCodeButton.addEventListener("click", async () => {
    if (!reqEmailInput || !confCodeInput) return;

    const email = reqEmailInput.value.trim();
    const code = confCodeInput.value.trim();

    if (code.length !== 6) {
      if (codeMessage) {
        codeMessage.style.color = "#d32f2f";
        codeMessage.textContent = "6자리 인증번호를 입력해 주세요.";
      }
      isCodeVerified = false;
      validateConfirmForm();
      return;
    }

    if (resetTimerId === null || resetRemainingSec <= 0) {
      if (codeMessage) {
        codeMessage.style.color = "#d32f2f";
        codeMessage.textContent =
          "인증번호 유효 시간이 만료되었습니다. 다시 요청해 주세요.";
      }
      isCodeVerified = false;
      validateConfirmForm();
      return;
    }

    if (codeMessage) {
      codeMessage.style.color = "#d32f2f";
      codeMessage.textContent = "인증번호 확인 중입니다...";
    }
    if (confPasswordError) {
      confPasswordError.textContent = "";
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/password-reset/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const msg = data.message || "인증 실패";
        throw new Error(msg);
      }

      isCodeVerified = true;

      if (codeMessage) {
        codeMessage.style.color = "green";
        codeMessage.textContent = "인증번호가 확인되었습니다.";
      }
      if (confCodeInput) confCodeInput.disabled = true;
      if (verifyCodeButton) verifyCodeButton.disabled = true;

      validateConfirmForm();
    } catch (err) {
      isCodeVerified = false;
      if (codeMessage) {
        codeMessage.style.color = "#d32f2f";
        codeMessage.textContent = "인증번호가 일치하지 않습니다.";
      }
      validateConfirmForm();
    }
  });

// 2단계: 인증번호 재발급 버튼 클릭
// 융합프로젝트 김태형 11주차 비밀번호 재설정(OpenAPI 스펙 확정) : 인증번호 재발급 로직 (추가)
resendCodeButton &&
  resendCodeButton.addEventListener("click", async () => {        // (추가)
    if (!reqEmailInput) return;                                   // (추가)
    const email = reqEmailInput.value.trim();                     // (추가)

    if (!email || !email.includes("@")) {                         // (추가)
      if (messageDiv) {                                           // (추가)
        messageDiv.style.color = "#d32f2f";                       // (추가)
        messageDiv.textContent = "올바른 이메일 형식을 입력해 주세요."; // (추가)
      }
      return;                                                     // (추가)
    }

    // 이전 인증 상태/에러 초기화 (추가)
    isCodeVerified = false;                                       // (추가)
    if (confCodeInput) {                                          // (추가)
      confCodeInput.disabled = false;                             // (추가)
      confCodeInput.value = "";                                   // (추가)
    }
    if (verifyCodeButton) verifyCodeButton.disabled = false;      // (추가)
    if (codeMessage) {                                            // (추가)
      codeMessage.textContent = "";                               // (추가)
      codeMessage.style.color = "#d32f2f";                        // (추가)
    }
    if (confPasswordError) confPasswordError.textContent = "";    // (추가)

    if (messageDiv) {                                             // (추가)
      messageDiv.style.color = "#d32f2f";                         // (추가)
      messageDiv.textContent = "인증번호를 다시 발송 중입니다..."; // (추가)
    }

    try {                                                         // (추가)
      const response = await fetch(`${API_BASE_URL}/auth/password-reset/request`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const msg = data.message || "요청이 실패했습니다.";
        throw new Error(msg);
      }

      // 타이머/메시지 다시 시작 (추가)
      baseTimerMessage = "인증번호가 이메일로 다시 발송되었습니다."; // (추가)
      startResetTimer(180);                                       // (추가)
    } catch (err) {                                               // (추가)
      baseTimerMessage = "";                                      // (추가)
      if (messageDiv) {                                           // (추가)
        messageDiv.style.color = "#d32f2f";                       // (추가)
        messageDiv.textContent =
          "재발급 실패: " + (err.message || "서버 응답 없음");    // (추가)
      }
    }
  });

// 2단계: 비밀번호 재설정 폼 제출
formConfirm &&
  formConfirm.addEventListener("submit", async (e) => {
    e.preventDefault();

    if (!reqEmailInput || !confCodeInput || !confPasswordInput) return;

    const email = reqEmailInput.value.trim();
    const code = confCodeInput.value.trim();
    const newPassword = confPasswordInput.value;
    const specialCharRegex = /[^A-Za-z0-9]/; // 특수문자 1개 이상

    // 이전 에러 문구 초기화
    if (codeMessage) {
      codeMessage.textContent = "";
      codeMessage.style.color = "#d32f2f";
    }
    if (confPasswordError) {
      confPasswordError.textContent = "";
      confPasswordError.style.color = "#d32f2f";
    }

    // 1) 인증번호 6자리 체크
    if (code.length !== 6) {
      if (codeMessage) {
        codeMessage.textContent = "6자리 인증번호를 입력해 주세요.";
      }
      return;
    }

    // 2) 비밀번호 형식 체크
    if (newPassword.length < 8 || !specialCharRegex.test(newPassword)) {
      if (confPasswordError) {
        confPasswordError.textContent =
          "비밀번호는 8자 이상이며, 특수문자를 1개 이상 포함해야 합니다.";
      }
      return;
    }

    // 3) 인증번호 확인 여부 체크 (핵심 요구사항)
    if (!isCodeVerified) {
      if (confPasswordError) {
        confPasswordError.style.color = "#d32f2f";
        confPasswordError.textContent =
          "이메일 인증번호를 먼저 확인해 주세요.";
      } else if (codeMessage) {
        codeMessage.style.color = "#d32f2f";
        codeMessage.textContent = "먼저 인증번호 확인을 완료해 주세요.";
      }
      return;
    }

    if (messageDiv) {
      messageDiv.style.color = "#d32f2f";
      messageDiv.textContent = "서버에 요청 중...";
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/password-reset/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code, newPassword })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const msg = data.message || "재설정 실패";
        throw new Error(msg);
      }

      if (resetTimerId !== null) {
        clearInterval(resetTimerId);
        resetTimerId = null;
      }
      resetRemainingSec = 0;
      baseTimerMessage = "";
      renderResetTimerMessage();

      if (messageDiv) {
        messageDiv.style.color = "green";
        messageDiv.textContent =
          data.message || "비밀번호가 재설정되었습니다.";
      }

      if (formConfirm) formConfirm.style.display = "none";
    } catch (err) {
      if (messageDiv) {
        messageDiv.style.color = "#d32f2f";
        messageDiv.textContent =
          "재설정 실패: " + (err.message || "서버 응답 없음");
      }
    }
  });

// 초기 상태
renderResetTimerMessage();
validateConfirmForm();
