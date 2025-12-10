document.addEventListener('DOMContentLoaded', function() {
    // ==========================================
    // 0. 공통: Auth 객체 체크 + API 래퍼
    // ==========================================
    const hasAuth = (window.Auth && typeof window.Auth.fetchMe === 'function');
    // 융합프로젝트 김태형 12주차 : Auth.apiFetch 사용 여부
    const hasAuthApi = (window.Auth && typeof window.Auth.apiFetch === 'function');

    // 융합프로젝트 김태형 12주차 :
    //  공통 JSON 래퍼 (성공시 body.data 반환, 실패시 Error)
    async function apiJson(path, options = {}) {
        if (!hasAuthApi) {
            throw new Error('Auth.apiFetch를 사용할 수 없습니다.');
        }
        const res = await window.Auth.apiFetch(path, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            }
        });
        const body = await res.json();
        if (!body || !body.success) {
            const msg = (body && body.message) ? body.message : 'API 호출 실패';
            throw new Error(msg);
        }
        return body.data;
    }

    // ==========================================
    // 1. 프로필(우측 상단) 메뉴 로직
    // ==========================================
    const profileBtn = document.getElementById('user-profile-btn');
    const profileDropdown = document.getElementById('user-profile-dropdown');

    if (profileBtn && profileDropdown) {
        profileBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            profileDropdown.classList.toggle('active');
        });
    }

    // ==========================================
    // 2. 공통: 화면의 빈 곳 클릭 시 모든 메뉴 닫기
    // ==========================================
    function closeAllDraftMenus() {
        const menus = document.querySelectorAll('.draft-dropdown-menu.active');
        menus.forEach(function(menu) {
            menu.classList.remove('active');
        });
    }

    window.addEventListener('click', function(e) {
        // 프로필 메뉴 닫기
        if (profileDropdown && profileBtn) {
            if (!profileDropdown.contains(e.target) && !profileBtn.contains(e.target)) {
                profileDropdown.classList.remove('active');
            }
        }

        // 이력서 카드의 ... 메뉴 닫기
        closeAllDraftMenus();
    });

    // ==========================================
    // 3. 로그인한 사용자 정보(/api/users/me)로
    //    우측 상단 이름/이메일 채우기
    // ==========================================
    const nameEl = document.querySelector('.user-name-small');
    const emailEl = document.querySelector('.user-email-small');

    if (hasAuth && nameEl && emailEl) {
        window.Auth.fetchMe()
            .then(function(user) {
                if (!user) {
                    console.warn('사용자 정보를 가져오지 못했습니다.');
                    return;
                }
                nameEl.textContent = user.name || '';
                emailEl.textContent = user.email || '';
            })
            .catch(function(err) {
                console.error('현재 로그인한 사용자 정보 조회 실패:', err);
            });
    } else {
        console.warn('Auth.fetchMe 또는 .user-name-small / .user-email-small 요소를 찾을 수 없습니다.');
    }

    // ==========================================
    // 4. 로그아웃 버튼(.logout-option) 처리
    // ==========================================
    const logoutLink = document.querySelector('.logout-option');

    if (hasAuth && logoutLink) {
        logoutLink.addEventListener('click', function(e) {
            e.preventDefault();
            window.Auth.logout()
                .catch(function(err) {
                    console.warn('로그아웃 요청 실패(그래도 이동):', err);
                })
                .finally(function() {
                    window.location.href = '/auth/login';
                });
        });
    } else {
        console.warn('Auth.logout 또는 .logout-option 요소를 찾을 수 없습니다.');
    }

    // ==========================================
    // 5. 새 이력서 생성 + 편집 화면 이동
    // ==========================================
    // 2233073 김용하 12주차 : /api/resumes POST로 새 이력서 생성 후 /resume/edit?resumeId=... 로 이동
    // 2233076 12주차 추가: 선택한 템플릿 ID 포함
    // 융합프로젝트 김태형 12주차 :
    //  - 이력서 기본 정보 필드(name/phone/email/birthDate)를 포함해 생성
    const newResumeBtn = document.getElementById('new-resume-btn');

    if (newResumeBtn && hasAuthApi) {
        newResumeBtn.addEventListener('click', async function(e) {
            e.preventDefault();

            if (newResumeBtn.dataset.loading === 'true') return;
            newResumeBtn.dataset.loading = 'true';

            const originalHtml = newResumeBtn.innerHTML;
            newResumeBtn.innerHTML = '<span class="plus-icon">+</span> 생성 중...';

            try {
                const payload = {
                    title: '새 이력서',
                    summary: '',
                    isPublic: false,
                    // 융합프로젝트 김태형 12주차 : 기본 정보는 초기에는 비워서 생성
                    name: null,
                    phone: null,
                    email: null,
                    birthDate: null,
                    // 2233076 12주차 추가: 선택한 템플릿 ID 포함
                    templateId: selectedTemplateIndex || 1
                };

                const newId = await apiJson('/api/resumes', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });

                window.location.href = '/resume/edit?resumeId=' + encodeURIComponent(newId);
            } catch (err) {
                console.error('새 이력서 생성 중 오류:', err);
                alert('이력서 생성에 실패했습니다.\n' + (err.message || '알 수 없는 오류'));
            } finally {
                newResumeBtn.dataset.loading = 'false';
                newResumeBtn.innerHTML = originalHtml;
            }
        });
    } else {
        console.warn('new-resume-btn 또는 Auth.apiFetch를 찾을 수 없습니다.');
    }

    // ==========================================
    // 6. 내 이력서 목록 동적 렌더링
    // ==========================================
    // 2233073 김용하 12주차 : /api/resumes 목록을 조회해서 카드 + ... 옵션(이름 변경/삭제/PDF 내보내기)까지 그리기
    const draftsContainer = document.querySelector('.drafts-container');

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // ==========================================
    // 7. 템플릿 선택 상태 관리 (PDF 내보내기 연동)
    // ==========================================
    // 2233073 김용하 12주차 :
    //  - 템플릿 카드 클릭 시 선택 상태를 저장하고,
    //    PDF 내보내기 시 쿼리 파라미터로 템플릿 번호를 전달한다.
    // 2233076 13주차 추가: 템플릿 이미지 클릭 시 모달 팝업
    let selectedTemplateIndex = 1;  // 기본 1번 템플릿

    const templateCards = document.querySelectorAll('.template-card');
    templateCards.forEach(function(card) {
        const index = parseInt(card.dataset.templateIndex || '0', 10);
        if (!index) return;

        // (HTML에서 1번 카드에 이미 selected 클래스를 줬지만,
        //  JS에서도 현재 선택값과 동기화해 둔다.)
        if (index === selectedTemplateIndex) {
            card.classList.add('selected');
        }

        card.addEventListener('click', function(e) {
            e.preventDefault();

            selectedTemplateIndex = index;

            // 모든 카드에서 selected 제거 후 클릭된 카드만 표시
            templateCards.forEach(function(c) {
                c.classList.remove('selected');
            });
            card.classList.add('selected');
        });

        // 2233076 13주차 추가: 템플릿 이미지 클릭 시 모달 팝업
        const img = card.querySelector('.template-image');
        if (img) {
            img.addEventListener('click', function(e) {
                e.stopPropagation();
                const modal = document.getElementById('template-preview-modal');
                const modalImg = document.getElementById('template-modal-image');
                if (modal && modalImg) {
                    modalImg.src = img.src;
                    modal.classList.add('active');
                }
            });
        }
    });

    // 2233076 13주차 추가: 모달 닫기 이벤트
    const modal = document.getElementById('template-preview-modal');
    const modalClose = document.querySelector('.template-modal-close');
    const modalOverlay = document.querySelector('.template-modal-overlay');

    if (modalClose) {
        modalClose.addEventListener('click', function() {
            modal.classList.remove('active');
        });
    }

    if (modalOverlay) {
        modalOverlay.addEventListener('click', function() {
            modal.classList.remove('active');
        });
    }

    // 융합프로젝트 김태형 12주차 :
    //  내 이력서 목록을 불러와 카드로 그리는 함수
    async function loadMyResumes() {
        if (!draftsContainer) return;
        if (!hasAuthApi) {
            console.warn('Auth.apiFetch가 없어 이력서 목록을 불러오지 않습니다.');
            return;
        }

        draftsContainer.innerHTML = '';

        try {
            // 정렬 키는 서버의 sortMap(created_at 등)에 맞춰서 사용
            const page = await apiJson('/api/resumes?page=0&size=50&sort=created_at,desc');
            const items = Array.isArray(page.content) ? page.content : [];

            if (items.length === 0) {
                draftsContainer.innerHTML =
                    '<p class="empty-drafts-text">아직 저장된 이력서가 없습니다. ' +
                    '상단의 "새 이력서" 버튼을 눌러 첫 이력서를 만들어 보세요.</p>';
                return;
            }

            items.forEach(function(resume) {
                const card = document.createElement('div');
                card.className = 'draft-card';
                card.dataset.resumeId = resume.resumeId;

                const title = resume.title || '제목 없음';

                card.innerHTML = `
                    <div class="draft-card-preview"></div> 
                    <div class="draft-card-info">
                        <span class="draft-name">${escapeHtml(title)}</span>
                        <div class="draft-options-wrapper" style="position: relative;">
                            <button type="button" class="draft-options-button">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                     viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                                     class="w-4 h-4">
                                    <circle cx="12" cy="12" r="1"></circle>
                                    <circle cx="19" cy="12" r="1"></circle>
                                    <circle cx="5" cy="12" r="1"></circle>
                                </svg>
                            </button>
                            <div class="draft-dropdown-menu">
                                <ul>
                                    <li><a href="#" class="rename-option">이름 변경</a></li>
                                    <li><a href="#" class="delete-option">삭제</a></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                `;

                // 카드 전체를 클릭하면 편집 화면으로 이동
                card.addEventListener('click', function(e) {
                    e.preventDefault();
                    const id = card.dataset.resumeId;
                    if (!id) return;
                    window.location.href = '/resume/edit?resumeId=' + encodeURIComponent(id);
                });

                draftsContainer.appendChild(card);

                const optionsBtn = card.querySelector('.draft-options-button');
                const dropdown = card.querySelector('.draft-dropdown-menu');
                const renameBtn = card.querySelector('.rename-option');
                const deleteBtn = card.querySelector('.delete-option');

                if (optionsBtn && dropdown) {
                    optionsBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        closeAllDraftMenus();
                        dropdown.classList.toggle('active');
                    });
                }

                // 2233073 김용하 12주차 : 카드에서 바로 제목 수정(PATCH /api/resumes/{id})
                // 2233076 12주차 추가: 템플릿 ID 유지
                // 융합프로젝트 김태형 12주차 :
                //  - 제목 변경 시, DB에 저장된 기본 정보(name/phone/email/birthDate)까지 유지하도록
                if (renameBtn) {
                    renameBtn.addEventListener('click', async function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        closeAllDraftMenus();

                        const nameSpan = card.querySelector('.draft-name');
                        const currentTitle = (nameSpan?.textContent || '새 이력서').trim();
                        const next = window.prompt('새 이력서 이름을 입력하세요.', currentTitle);
                        if (next === null) return;
                        const trimmed = next.trim();
                        if (!trimmed) {
                            alert('이력서 이름을 비울 수 없습니다.');
                            return;
                        }

                        try {
                            const id = card.dataset.resumeId;
                            if (!id) throw new Error('이력서 ID가 없습니다.');

                            // 융합프로젝트 김태형 12주차 :
                            //  현재 이력서의 summary / isPublic / 기본 정보를 단건 조회
                            const detail = await apiJson('/api/resumes/' + encodeURIComponent(id));

                            const payload = {
                                title: trimmed,
                                isPublic: (typeof detail.isPublic === 'boolean')
                                    ? detail.isPublic
                                    : false,
                                summary: detail.summary || '',
                                name: detail.name || null,
                                phone: detail.phone || null,
                                email: detail.email || null,
                                birthDate: detail.birthDate || null,
                                // 2233076 12주차 추가: 템플릿 ID 유지
                                templateId: detail.templateId || 1
                            };

                            await apiJson('/api/resumes/' + encodeURIComponent(id), {
                                method: 'PATCH',
                                body: JSON.stringify(payload)
                            });

                            if (nameSpan) {
                                nameSpan.textContent = trimmed;
                            }
                            resume.title = trimmed;

                            alert('이력서 이름이 변경되었습니다.');
                        } catch (err) {
                            console.error('이력서 이름 변경 실패:', err);
                            alert('이름 변경 중 오류가 발생했습니다.\n' + (err.message || '알 수 없는 오류'));
                        }
                    });
                }

                // 2233073 김용하 12주차 : 카드에서 바로 이력서 삭제(DELETE /api/resumes/{id})
                if (deleteBtn) {
                    deleteBtn.addEventListener('click', async function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        closeAllDraftMenus();

                        const confirmed = window.confirm('이 이력서를 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.');
                        if (!confirmed) return;

                        try {
                            const id = card.dataset.resumeId;
                            if (!id) throw new Error('이력서 ID가 없습니다.');

                            await window.Auth.apiFetch('/api/resumes/' + encodeURIComponent(id), {
                                method: 'DELETE'
                            });

                            card.remove();

                            if (draftsContainer.children.length === 0) {
                                draftsContainer.innerHTML =
                                    '<p class="empty-drafts-text">아직 저장된 이력서가 없습니다. ' +
                                    '상단의 "새 이력서" 버튼을 눌러 첫 이력서를 만들어 보세요.</p>';
                            }
                        } catch (err) {
                            console.error('이력서 삭제 실패:', err);
                            alert('이력서 삭제 중 오류가 발생했습니다.\n' + (err.message || '알 수 없는 오류'));
                        }
                    });
                }
            });
        } catch (err) {
            console.error('이력서 목록 조회 실패:', err);
            draftsContainer.innerHTML =
                '<p class="empty-drafts-text">이력서 목록을 불러오지 못했습니다.</p>';
        }
    }

    if (draftsContainer) {
        loadMyResumes();
    }
});
