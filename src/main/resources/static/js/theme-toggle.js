// 2233076 13주차 추가: 공통 다크모드 토글 스크립트

// 페이지 로드 즉시 테마 적용 (깜빡임 방지)
(function() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

// DOM 로드 후 토글 버튼 이벤트 설정
document.addEventListener('DOMContentLoaded', function() {
    const themeToggle = document.getElementById('theme-toggle');
    const themeSlider = document.querySelector('.theme-toggle-slider');
    const currentTheme = localStorage.getItem('theme') || 'light';

    // 슬라이더 아이콘 초기화
    if (themeSlider) {
        themeSlider.textContent = currentTheme === 'dark' ? '☀️' : '🌙';
    }

    // 토글 버튼 클릭 이벤트
    if (themeToggle) {
        themeToggle.addEventListener('click', function() {
            const theme = document.documentElement.getAttribute('data-theme');
            const newTheme = theme === 'dark' ? 'light' : 'dark';
            
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            
            if (themeSlider) {
                themeSlider.textContent = newTheme === 'dark' ? '☀️' : '🌙';
            }
        });
    }
});
