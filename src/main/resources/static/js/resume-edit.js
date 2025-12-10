document.addEventListener('DOMContentLoaded', () => {
    // 융합프로젝트 김태형 12주차 :
    //  - 좌측 입력 폼과 우측 미리보기를 실시간으로 동기화
    //  - 경력/프로젝트 동적 추가, 템플릿 캐러셀
    //  - 내 초안 페이지 이동, 로그인 사용자 정보 표시
    //  - 이력서(학력/경력/프로젝트/스킬) 저장 + DB 값 다시 불러오기 + 요약(summary) 저장
    //  - 이력서 기본 정보(이름/전화번호/이메일/생년월일)를 PATCH 시 항상 함께 전송
    //  - 프로필 사진(증명사진) 업로드 + DB URL 저장 + 미리보기 표시

    // ------------------------------------------------------------
    // 2233076 12주차 추가: 토스트 메시지 함수
    // ------------------------------------------------------------
    function showToast(message, type = 'info', duration = 2000) {
        // 기존 토스트가 있으면 제거
        const existing = document.querySelector('.toast-message');
        if (existing) {
            existing.remove();
        }

        // 새 토스트 생성
        const toast = document.createElement('div');
        toast.className = `toast-message ${type}`;
        toast.textContent = message;
        document.body.appendChild(toast);

        // 표시
        setTimeout(() => {
            toast.classList.add('show');
        }, 10);

        // 자동 제거
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, duration);
    }

    // ------------------------------------------------------------
    // 이력서 ID 파라미터 추출 + API 공통 유틸
    // ------------------------------------------------------------
    const hasAuth = (window.Auth && typeof window.Auth.apiFetch === 'function');

    function getResumeIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const id = params.get('resumeId');
        return id ? Number(id) : null;
    }

    const resumeId = getResumeIdFromUrl();

    // 이력서 메타데이터(공개 여부, 요약)를 임시로 들고 있는 캐시
    let resumeMeta = {
        isPublic: false,
        summary: ''
    };

    // ------------------------------------------------------------
    // 기본 정보 입력값 → PATCH/POST payload에 공통으로 넣기 위한 헬퍼
    // ------------------------------------------------------------
    function buildProfilePayload() {
        const nameEl = document.getElementById('input-name');
        const phoneEl = document.getElementById('input-phone');
        const emailEl = document.getElementById('input-email');
        const birthEl = document.getElementById('input-birth');

        const name = nameEl ? nameEl.value.trim() : '';
        const phone = phoneEl ? phoneEl.value.trim() : '';
        const email = emailEl ? emailEl.value.trim() : '';
        const birthDate = birthEl && birthEl.value ? birthEl.value : null;

        return {
            name,
            phone,
            email,
            birthDate
        };
    }

    // 2233076 12주차 수정: 템플릿 ID 포함
    // 제목(파일 이름) + 요약 + 공개 여부 + 기본 정보 + 템플릿ID까지 한 번에 묶어 payload 생성
    function buildResumePatchPayload(explicitTitle) {
        const nameSpan = document.getElementById('file-name-display');
        const summaryInput = document.getElementById('input-summary');

        const title =
            explicitTitle != null && explicitTitle !== undefined
                ? explicitTitle
                : (nameSpan ? (nameSpan.textContent || '새 이력서').trim() : '새 이력서');

        const summary = summaryInput ? summaryInput.value.trim() : '';
        resumeMeta.summary = summary;

        const profile = buildProfilePayload();

        return {
            title,
            isPublic: resumeMeta.isPublic ?? false,
            summary,
            ...profile,
            // 2233076 12주차 추가: 현재 선택된 템플릿 ID 포함 (1-based)
            templateId: currentTemplateIndex + 1
        };
    }

    // ------------------------------------------------------------
    // "2024.01 ~ 2024.06 / 재직" ⇄ 날짜 파싱/포맷
    // ------------------------------------------------------------
    function parsePeriod(text) {
        if (!text) {
            return { startDate: null, endDate: null, isCurrent: false };
        }

        const parts = text.split('~').map((p) => p.trim());
        const startRaw = parts[0] || '';
        const endRaw = parts[1] || '';

        function toDateStr(raw) {
            const m = raw.match(/(\d{4})[.\-\/](\d{1,2})(?:[.\-\/](\d{1,2}))?/);
            if (!m) return null;
            const y = m[1];
            const mm = m[2].padStart(2, '0');
            const dd = (m[3] || '01').padStart(2, '0');
            return `${y}-${mm}-${dd}`;
        }

        const startDate = toDateStr(startRaw);
        let endDate = null;
        let isCurrent = false;

        if (endRaw) {
            if (/재학|재직|현재/.test(endRaw)) {
                isCurrent = true;
            } else {
                endDate = toDateStr(endRaw);
            }
        }

        return { startDate, endDate, isCurrent };
    }

    // DB에 있는 startDate/endDate를 "YYYY.MM ~ 재직" 형식으로 변환
    function formatPeriod(startDate, endDate, isCurrent) {
        function toYm(dateStr) {
            if (!dateStr) return '';
            const parts = dateStr.split('-');
            if (parts.length < 2) return dateStr;
            return `${parts[0]}.${parts[1]}`;
        }

        const startText = toYm(startDate);
        let endText = '';

        if (isCurrent) {
            endText = '재학';
        } else {
            endText = toYm(endDate);
        }

        if (!startText && !endText) return '';
        if (!startText) return endText;
        if (!endText) return startText;
        return `${startText} ~ ${endText}`;
    }

    // ------------------------------------------------------------
    // 공통 API JSON 래퍼
    // ------------------------------------------------------------
    async function apiJson(path, options = {}) {
        if (!hasAuth) {
            throw new Error('Auth 유틸이 없습니다. /js/auth.js 로드 여부를 확인하세요.');
        }

        const res = await Auth.apiFetch(path, {
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

    // ------------------------------------------------------------
    // 이력서 메타데이터(파일명, 공개 여부, 요약, 기본 정보, 사진, 템플릿ID)를 조회해서
    // 상단/요약 입력칸 + 기본 정보 입력칸 + 미리보기에 반영
    // 2233076 12주차 추가: 템플릿 ID 로드
    // ------------------------------------------------------------
    async function loadResumeMeta(resumeId) {
        const nameSpan = document.getElementById('file-name-display');
        const summaryInput = document.getElementById('input-summary');
        const previewSummary = document.getElementById('preview-summary');

        const nameInput = document.getElementById('input-name');
        const phoneInput = document.getElementById('input-phone');
        const emailInput = document.getElementById('input-email');
        const birthInput = document.getElementById('input-birth');
        const photoBox = document.getElementById('preview-photo-box');

        if (!resumeId || !hasAuth) return;

        try {
            const data = await apiJson(`/api/resumes/${resumeId}`);
            if (!data) return;

            if (typeof data.isPublic === 'boolean') {
                resumeMeta.isPublic = data.isPublic;
            }
            if (typeof data.summary === 'string') {
                resumeMeta.summary = data.summary;
                if (summaryInput) {
                    summaryInput.value = data.summary;
                    summaryInput.dispatchEvent(new Event('input'));
                } else if (previewSummary) {
                    previewSummary.textContent = data.summary;
                }
            }
            if (data.title && nameSpan) {
                nameSpan.textContent = data.title;
            }

            // 기본 정보 채우기
            if (nameInput && data.name) {
                nameInput.value = data.name;
                nameInput.dispatchEvent(new Event('input'));
            }
            if (phoneInput && data.phone) {
                phoneInput.value = data.phone;
                phoneInput.dispatchEvent(new Event('input'));
            }
            if (emailInput && data.email) {
                emailInput.value = data.email;
                emailInput.dispatchEvent(new Event('input'));
            }
            if (birthInput && data.birthDate) {
                birthInput.value = data.birthDate;
                birthInput.dispatchEvent(new Event('input'));
            }

            // 프로필 사진
            if (photoBox) {
                if (data.profileImageUrl) {
                    photoBox.innerHTML =
                        `<img src="${data.profileImageUrl}" alt="사진">`;
                } else {
                    photoBox.textContent = '사진 영역';
                }
            }

            // 2233076 12주차 추가: 템플릿 ID 로드하여 템플릿 적용
            if (typeof data.templateId === 'number' && data.templateId >= 1 && data.templateId <= 6) {
                const templateIndex = data.templateId - 1; // 0-based index
                applyTemplate(templateIndex);
            }
        } catch (err) {
            console.warn('이력서 메타정보 조회 실패(새 이력서일 수 있음):', err);
        }
    }

    // ------------------------------------------------------------
    // 학력/경력/프로젝트/스킬 저장 로직
    // ------------------------------------------------------------
    async function saveEducationSection(resumeId) {
        const school =
            document.getElementById('input-edu-school')?.value.trim() || '';
        const major =
            document.getElementById('input-edu-major')?.value.trim() || '';
        const degree =
            document.getElementById('input-edu-degree')?.value.trim() || '';
        const period =
            document.getElementById('input-edu-period')?.value.trim() || '';

        const page =
            await apiJson(`/api/resumes/${resumeId}/educations?page=0&size=50`);
        if (page && Array.isArray(page.content)) {
            await Promise.all(
                page.content.map((edu) =>
                    apiJson(`/api/educations/${edu.educationId}`, {
                        method: 'DELETE'
                    })
                )
            );
        }

        if (!school && !major && !degree && !period) {
            return;
        }

        const parsed = parsePeriod(period);

        await apiJson(`/api/resumes/${resumeId}/educations`, {
            method: 'POST',
            body: JSON.stringify({
                schoolName: school || null,
                major: major || null,
                degree: degree || null,
                startDate: parsed.startDate,
                endDate: parsed.endDate,
                current: parsed.isCurrent
            })
        });
    }

    async function saveExperienceSections(resumeId) {
        const existing = await apiJson(`/api/resumes/${resumeId}/experiences`);
        if (Array.isArray(existing)) {
            await Promise.all(
                existing.map((exp) =>
                    apiJson(`/api/experiences/${exp.experienceId}`, {
                        method: 'DELETE'
                    })
                )
            );
        }

        const groups = document.querySelectorAll('.experience-group');
        const tasks = [];

        groups.forEach((group) => {
            const idx = group.dataset.expIndex;

            const company =
                document.getElementById(`input-exp${idx}-company`)
                    ?.value.trim() || '';
            const position =
                document.getElementById(`input-exp${idx}-position`)
                    ?.value.trim() || '';
            const period =
                document.getElementById(`input-exp${idx}-period`)
                    ?.value.trim() || '';
            const desc =
                document.getElementById(`input-exp${idx}-desc`)
                    ?.value.trim() || '';

            if (!company && !position && !period && !desc) return;

            const parsed = parsePeriod(period);

            tasks.push(
                apiJson(`/api/resumes/${resumeId}/experiences`, {
                    method: 'POST',
                    body: JSON.stringify({
                        companyName: company || null,
                        positionTitle: position || null,
                        startDate: parsed.startDate,
                        endDate: parsed.endDate,
                        isCurrent: parsed.isCurrent,
                        description: desc || null
                    })
                })
            );
        });

        await Promise.all(tasks);
    }

    async function saveProjectSections(resumeId) {
        const existing = await apiJson(`/api/resumes/${resumeId}/projects`);
        if (Array.isArray(existing)) {
            await Promise.all(
                existing.map((proj) =>
                    apiJson(`/api/projects/${proj.projectId}`, {
                        method: 'DELETE'
                    })
                )
            );
        }

        const groups = document.querySelectorAll('.project-group');
        const tasks = [];

        groups.forEach((group) => {
            const idx = group.dataset.projIndex;

            const name =
                document.getElementById(`input-proj${idx}-name`)
                    ?.value.trim() || '';
            const role =
                document.getElementById(`input-proj${idx}-role`)
                    ?.value.trim() || '';
            const period =
                document.getElementById(`input-proj${idx}-period`)
                    ?.value.trim() || '';
            const tech =
                document.getElementById(`input-proj${idx}-tech`)
                    ?.value.trim() || '';
            const desc =
                document.getElementById(`input-proj${idx}-desc`)
                    ?.value.trim() || '';

            if (!name && !role && !period && !tech && !desc) return;

            const parsed = parsePeriod(period);

            tasks.push(
                apiJson(`/api/resumes/${resumeId}/projects`, {
                    method: 'POST',
                    body: JSON.stringify({
                        name: name || null,
                        role: role || null,
                        startDate: parsed.startDate,
                        endDate: parsed.endDate,
                        isCurrent: parsed.isCurrent,
                        summary: desc || null,
                        techStack: tech || null,
                        url: null
                    })
                })
            );
        });

        await Promise.all(tasks);
    }

    async function saveSkillSections(resumeId) {
        const inputEl = document.getElementById('input-skill-list');
        const raw = inputEl ? inputEl.value : '';

        const existing = await apiJson(`/api/resumes/${resumeId}/skills`);
        if (Array.isArray(existing)) {
            await Promise.all(
                existing.map((rs) =>
                    apiJson(`/api/resumes/${resumeId}/skills/${rs.skillId}`, {
                        method: 'DELETE'
                    })
                )
            );
        }

        const names = raw
            .split(',')
            .map((s) => s.trim())
            .filter((s) => s.length > 0);

        for (const name of names) {
            const searchRes =
                await apiJson(`/api/skills?q=${encodeURIComponent(name)}&limit=50`);
            let skillId = null;

            if (Array.isArray(searchRes) && searchRes.length > 0) {
                const found = searchRes.find(
                    (s) =>
                        s.name &&
                        s.name.toLowerCase() === name.toLowerCase()
                );
                if (found) {
                    skillId = found.skillId;
                }
            }

            if (!skillId) {
                const newId = await apiJson('/api/skills', {
                    method: 'POST',
                    body: JSON.stringify({ name })
                });
                skillId = newId;
            }

            await apiJson(`/api/resumes/${resumeId}/skills/${skillId}`, {
                method: 'PUT',
                body: JSON.stringify({ proficiency: 0 })
            });
        }
    }

    async function saveAllSections(resumeId) {
        await saveEducationSection(resumeId);
        await saveExperienceSections(resumeId);
        await saveProjectSections(resumeId);
        await saveSkillSections(resumeId);
    }

    // ------------------------------------------------------------
    // DB에서 학력/경력/프로젝트/스킬 읽어와서 입력칸 + 미리보기에 채우기
    // ------------------------------------------------------------
    async function loadEducationSectionFromApi(resumeId) {
        try {
            const page =
                await apiJson(`/api/resumes/${resumeId}/educations?page=0&size=50`);
            if (!page || !Array.isArray(page.content) || page.content.length === 0) {
                return;
            }

            const edu = page.content[0];

            const schoolInput = document.getElementById('input-edu-school');
            const majorInput = document.getElementById('input-edu-major');
            const degreeInput = document.getElementById('input-edu-degree');
            const periodInput = document.getElementById('input-edu-period');

            if (schoolInput && edu.schoolName) {
                schoolInput.value = edu.schoolName;
                schoolInput.dispatchEvent(new Event('input'));
            }
            if (majorInput && edu.major) {
                majorInput.value = edu.major;
                majorInput.dispatchEvent(new Event('input'));
            }
            if (degreeInput && edu.degree) {
                degreeInput.value = edu.degree;
                degreeInput.dispatchEvent(new Event('input'));
            }

            const periodText =
                formatPeriod(edu.startDate, edu.endDate, edu.current === true);
            if (periodInput && periodText) {
                periodInput.value = periodText;
                periodInput.dispatchEvent(new Event('input'));
            }
        } catch (err) {
            console.warn('학력 섹션 로딩 실패:', err);
        }
    }

    async function loadExperienceSectionsFromApi(resumeId) {
        try {
            const list = await apiJson(`/api/resumes/${resumeId}/experiences`);
            if (!Array.isArray(list) || list.length === 0) return;

            for (let i = 2; i <= list.length; i++) {
                if (window.addExperienceBtnClick) {
                    window.addExperienceBtnClick();
                }
            }

            list.forEach((exp, index) => {
                const idx = index + 1;

                const companyInput =
                    document.getElementById(`input-exp${idx}-company`);
                const positionInput =
                    document.getElementById(`input-exp${idx}-position`);
                const periodInput =
                    document.getElementById(`input-exp${idx}-period`);
                const descInput =
                    document.getElementById(`input-exp${idx}-desc`);

                const periodText = formatPeriod(
                    exp.startDate,
                    exp.endDate,
                    (exp.isCurrent ?? exp.current) === true
                );

                if (companyInput && exp.companyName) {
                    companyInput.value = exp.companyName;
                    companyInput.dispatchEvent(new Event('input'));
                }
                if (positionInput && exp.positionTitle) {
                    positionInput.value = exp.positionTitle;
                    positionInput.dispatchEvent(new Event('input'));
                }
                if (periodInput && periodText) {
                    periodInput.value = periodText;
                    periodInput.dispatchEvent(new Event('input'));
                }
                if (descInput && exp.description) {
                    descInput.value = exp.description;
                    descInput.dispatchEvent(new Event('input'));
                }
            });
        } catch (err) {
            console.warn('경력 섹션 로딩 실패:', err);
        }
    }

    async function loadProjectSectionsFromApi(resumeId) {
        try {
            const list = await apiJson(`/api/resumes/${resumeId}/projects`);
            if (!Array.isArray(list) || list.length === 0) return;

            for (let i = 2; i <= list.length; i++) {
                if (window.addProjectBtnClick) {
                    window.addProjectBtnClick();
                }
            }

            list.forEach((proj, index) => {
                const idx = index + 1;

                const nameInput =
                    document.getElementById(`input-proj${idx}-name`);
                const roleInput =
                    document.getElementById(`input-proj${idx}-role`);
                const periodInput =
                    document.getElementById(`input-proj${idx}-period`);
                const techInput =
                    document.getElementById(`input-proj${idx}-tech`);
                const descInput =
                    document.getElementById(`input-proj${idx}-desc`);

                const periodText = formatPeriod(
                    proj.startDate,
                    proj.endDate,
                    (proj.isCurrent ?? proj.current) === true
                );

                if (nameInput && proj.name) {
                    nameInput.value = proj.name;
                    nameInput.dispatchEvent(new Event('input'));
                }
                if (roleInput && proj.role) {
                    roleInput.value = proj.role;
                    roleInput.dispatchEvent(new Event('input'));
                }
                if (periodInput && periodText) {
                    periodInput.value = periodText;
                    periodInput.dispatchEvent(new Event('input'));
                }
                if (techInput && proj.techStack) {
                    techInput.value = proj.techStack;
                    techInput.dispatchEvent(new Event('input'));
                }
                if (descInput && proj.summary) {
                    descInput.value = proj.summary;
                    descInput.dispatchEvent(new Event('input'));
                }
            });
        } catch (err) {
            console.warn('프로젝트 섹션 로딩 실패:', err);
        }
    }

    async function loadSkillSectionsFromApi(resumeId) {
        try {
            const inputEl = document.getElementById('input-skill-list');
            if (!inputEl) return;

            const list = await apiJson(`/api/resumes/${resumeId}/skills`);
            if (!Array.isArray(list) || list.length === 0) return;

            const names = list
                .map((s) => s.name || s.skillName || '')
                .filter((s) => s && s.length > 0);

            if (names.length > 0) {
                inputEl.value = names.join(', ');
                inputEl.dispatchEvent(new Event('input'));
            }
        } catch (err) {
            console.warn('스킬 섹션 로딩 실패:', err);
        }
    }

    // ------------------------------------------------------------
    // 공통: 입력값 ↔ 미리보기 바인딩
    // ------------------------------------------------------------
    function bindField(inputId, previewId) {
        const inputEl = document.getElementById(inputId);
        const previewEl = document.getElementById(previewId);
        if (!inputEl || !previewEl) return;

        const handler = (e) => {
            previewEl.textContent = e.target.value || '';
        };

        inputEl.addEventListener('input', handler);

        if (inputEl.value) {
            previewEl.textContent = inputEl.value;
        }
    }

    // 기본 정보
    bindField('input-name', 'preview-name');
    bindField('input-phone', 'preview-phone');
    bindField('input-email', 'preview-email');
    bindField('input-birth', 'preview-birth');
    bindField('input-summary', 'preview-summary');

    // 요약 입력 시 resumeMeta.summary도 동기화
    const summaryInputEl = document.getElementById('input-summary');
    if (summaryInputEl) {
        summaryInputEl.addEventListener('input', (e) => {
            resumeMeta.summary = e.target.value || '';
        });
    }

    // 학력
    bindField('input-edu-school', 'preview-edu-school');
    bindField('input-edu-period', 'preview-edu-period');

    // 경력 1
    bindField('input-exp1-company', 'preview-exp1-company');
    bindField('input-exp1-position', 'preview-exp1-position');
    bindField('input-exp1-period', 'preview-exp1-period');
    bindField('input-exp1-desc', 'preview-exp1-desc');

    // 프로젝트 1
    bindField('input-proj1-name', 'preview-proj1-name');
    bindField('input-proj1-period', 'preview-proj1-period');
    bindField('input-proj1-desc', 'preview-proj1-desc');

    // ------------------------------------------------------------
    // 학력 (전공 + 학위) 병합
    // ------------------------------------------------------------
    function updateEducation() {
        const majorEl = document.getElementById('input-edu-major');
        const degreeEl = document.getElementById('input-edu-degree');
        const previewEl = document.getElementById('preview-edu-major');

        if (!majorEl || !degreeEl || !previewEl) return;

        const major = majorEl.value.trim();
        const degree = degreeEl.value.trim();

        let text = major;
        if (degree) {
            text += (major ? ' / ' : '') + degree;
        }
        previewEl.textContent = text;
    }

    const eduMajorInput = document.getElementById('input-edu-major');
    const eduDegreeInput = document.getElementById('input-edu-degree');
    if (eduMajorInput) eduMajorInput.addEventListener('input', updateEducation);
    if (eduDegreeInput) eduDegreeInput.addEventListener('input', updateEducation);
    updateEducation();

    // ------------------------------------------------------------
    // 프로젝트 (역할 + 기술) 병합
    // ------------------------------------------------------------
    function updateProjectSub(num) {
        const roleEl = document.getElementById(`input-proj${num}-role`);
        const techEl = document.getElementById(`input-proj${num}-tech`);
        const previewEl = document.getElementById(`preview-proj${num}-role`);

        if (!roleEl || !techEl || !previewEl) return;

        const role = roleEl.value.trim();
        const tech = techEl.value.trim();

        let text = role;
        if (tech) {
            text += (role ? ' · ' : '') + tech;
        }
        previewEl.textContent = text;
    }

    function setupProjectRoleTechBinding(num) {
        const roleInput = document.getElementById(`input-proj${num}-role`);
        const techInput = document.getElementById(`input-proj${num}-tech`);

        if (roleInput) {
            roleInput.addEventListener('input', () => updateProjectSub(num));
        }
        if (techInput) {
            techInput.addEventListener('input', () => updateProjectSub(num));
        }

        updateProjectSub(num);
    }

    setupProjectRoleTechBinding(1);

    // ------------------------------------------------------------
    // 프로필 사진 업로드 API 호출 + 미리보기 반영
    // ------------------------------------------------------------
    async function uploadProfileImageToServer(resumeId, file, photoBox) {
        if (!resumeId) {
            alert('이력서 ID가 없습니다. URL에 ?resumeId=가 포함되어야 합니다.');
            return;
        }
        if (!hasAuth) {
            alert('로그인이 필요합니다.');
            return;
        }
        if (!file) {
            alert('업로드할 파일이 없습니다.');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            const res = await Auth.apiFetch(
                `/api/resumes/${resumeId}/profile-image`,
                {
                    method: 'POST',
                    body: formData
                }
            );

            const body = await res.json();
            if (!body || !body.success) {
                const msg = (body && body.message) ? body.message : '업로드 실패';
                throw new Error(msg);
            }

            const data = body.data || {};
            const url = data.profileImageUrl;

            if (url && photoBox) {
                photoBox.innerHTML = `<img src="${url}" alt="사진">`;
            }

            alert('프로필 사진이 저장되었습니다.');
        } catch (err) {
            console.error('프로필 사진 업로드 실패:', err);
            alert('프로필 사진 업로드 중 오류가 발생했습니다.\n' + err.message);
        }
    }

    // 사진 업로드 (로컬 미리보기 + 서버 업로드)
    const photoInput = document.getElementById('input-photo');
    const photoBox = document.getElementById('preview-photo-box');

    if (photoInput && photoBox) {
        photoInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            // 1) 로컬 미리보기
            const reader = new FileReader();
            reader.onload = function(event) {
                photoBox.innerHTML =
                    `<img src="${event.target.result}" alt="사진">`;
            };
            reader.readAsDataURL(file);

            // 2) 서버 업로드
            if (resumeId && hasAuth) {
                await uploadProfileImageToServer(resumeId, file, photoBox);
            }
        });
    }

    // ------------------------------------------------------------
    // 기술 스택 (쉼표 → 태그)
    // ------------------------------------------------------------
    const skillInput = document.getElementById('input-skill-list');
    const skillListPreview = document.getElementById('preview-skill-list');

    if (skillInput && skillListPreview) {
        const renderSkills = (text) => {
            const skills = text
                .split(',')
                .map((s) => s.trim())
                .filter((s) => s.length > 0);

            skillListPreview.innerHTML = '';

            skills.forEach((skill) => {
                const span = document.createElement('span');
                span.className = 'resume-tag';
                span.textContent = skill;
                skillListPreview.appendChild(span);
            });
        };

        skillInput.addEventListener('input', (e) => {
            renderSkills(e.target.value);
        });

        if (skillInput.value) {
            renderSkills(skillInput.value);
        }
    }

    // ------------------------------------------------------------
    // 로그인 정보 자동 채우기 (실제 유저 정보)
    // ------------------------------------------------------------
    const fillBtn = document.getElementById('fill-from-user-btn');
    if (fillBtn && window.Auth && typeof window.Auth.fetchMe === 'function') {
        fillBtn.addEventListener('click', () => {
            Auth.fetchMe()
                .then((user) => {
                    if (!user) return;

                    const nameEl = document.getElementById('input-name');
                    const emailEl = document.getElementById('input-email');
                    const phoneEl = document.getElementById('input-phone');

                    if (nameEl && user.name) {
                        nameEl.value = user.name;
                        nameEl.dispatchEvent(new Event('input'));
                    }
                    if (emailEl && user.email) {
                        emailEl.value = user.email;
                        emailEl.dispatchEvent(new Event('input'));
                    }
                    if (phoneEl && user.phone) {
                        phoneEl.value = user.phone;
                        phoneEl.dispatchEvent(new Event('input'));
                    }
                })
                .catch((err) => {
                    console.error('로그인 정보 자동 채우기 실패:', err);
                    alert('로그인 정보를 가져오지 못했습니다.');
                });
        });
    }

    // ------------------------------------------------------------
    // 경력 추가 동적 UI (+ 전역 함수로 노출)
    // ------------------------------------------------------------
    const MAX_EXP = 5;
    let expCount = 1;

    const experienceContainer = document.getElementById('experience-container');
    const previewExperienceList =
        document.getElementById('preview-experience-list');
    const addExperienceBtn = document.getElementById('add-experience-btn');

    if (experienceContainer && previewExperienceList && addExperienceBtn) {
        const addExperience = () => {
            if (expCount >= MAX_EXP) {
                alert(`경력은 최대 ${MAX_EXP}개까지 추가할 수 있습니다.`);
                return;
            }

            const newIndex = expCount + 1;

            const baseGroup =
                experienceContainer.querySelector(
                    '.experience-group[data-exp-index="1"]'
                );
            const basePreview =
                previewExperienceList.querySelector(
                    '.resume-item[data-exp-index="1"]'
                );
            if (!baseGroup || !basePreview) return;

            const groupClone = baseGroup.cloneNode(true);
            groupClone.dataset.expIndex = String(newIndex);

            const labelEl = groupClone.querySelector('.sub-section-label');
            if (labelEl) labelEl.textContent = `경력 ${newIndex}`;

            const inputs = groupClone.querySelectorAll('input, textarea');
            inputs.forEach((input) => {
                const oldId = input.id;
                if (!oldId) return;

                const newId = oldId.replace('exp1', `exp${newIndex}`);
                input.id = newId;
                input.value = '';

                const label =
                    groupClone.querySelector(`label[for="${oldId}"]`);
                if (label) {
                    label.setAttribute('for', newId);
                }
            });

            experienceContainer.appendChild(groupClone);

            const previewClone = basePreview.cloneNode(true);
            previewClone.dataset.expIndex = String(newIndex);

            const previewElements = previewClone.querySelectorAll('[id]');
            previewElements.forEach((el) => {
                const oldId = el.id;
                if (oldId.startsWith('preview-exp1-')) {
                    const newId =
                        oldId.replace('preview-exp1-', `preview-exp${newIndex}-`);
                    el.id = newId;
                    el.textContent = '';
                }
            });

            previewExperienceList.appendChild(previewClone);

            bindField(
                `input-exp${newIndex}-company`,
                `preview-exp${newIndex}-company`
            );
            bindField(
                `input-exp${newIndex}-position`,
                `preview-exp${newIndex}-position`
            );
            bindField(
                `input-exp${newIndex}-period`,
                `preview-exp${newIndex}-period`
            );
            bindField(
                `input-exp${newIndex}-desc`,
                `preview-exp${newIndex}-desc`
            );

            expCount = newIndex;
        };

        addExperienceBtn.addEventListener('click', addExperience);

        // 로딩 시 API에서 데이터 개수에 맞게 호출할 수 있도록 전역 함수로 노출
        window.addExperienceBtnClick = addExperience;
    }

    // ------------------------------------------------------------
    // 프로젝트 추가 동적 UI (+ 전역 함수)
    // ------------------------------------------------------------
    const MAX_PROJ = 5;
    let projCount = 1;

    const projectContainer = document.getElementById('project-container');
    const previewProjectList =
        document.getElementById('preview-project-list');
    const addProjectBtn = document.getElementById('add-project-btn');

    if (projectContainer && previewProjectList && addProjectBtn) {
        const addProject = () => {
            if (projCount >= MAX_PROJ) {
                alert(`프로젝트는 최대 ${MAX_PROJ}개까지 추가할 수 있습니다.`);
                return;
            }

            const newIndex = projCount + 1;

            const baseGroup =
                projectContainer.querySelector(
                    '.project-group[data-proj-index="1"]'
                );
            const basePreview =
                previewProjectList.querySelector('.resume-item[data-proj-index="1"]');
            if (!baseGroup || !basePreview) return;

            const groupClone = baseGroup.cloneNode(true);
            groupClone.dataset.projIndex = String(newIndex);

            const labelEl = groupClone.querySelector('.sub-section-label');
            if (labelEl) labelEl.textContent = `프로젝트 ${newIndex}`;

            const inputs = groupClone.querySelectorAll('input, textarea');
            inputs.forEach((input) => {
                const oldId = input.id;
                if (!oldId) return;

                const newId = oldId.replace('proj1', `proj${newIndex}`);
                input.id = newId;
                input.value = '';

                const label =
                    groupClone.querySelector(`label[for="${oldId}"]`);
                if (label) {
                    label.setAttribute('for', newId);
                }
            });

            projectContainer.appendChild(groupClone);

            const previewClone = basePreview.cloneNode(true);
            previewClone.dataset.projIndex = String(newIndex);

            const previewElements = previewClone.querySelectorAll('[id]');
            previewElements.forEach((el) => {
                const oldId = el.id;
                if (oldId.startsWith('preview-proj1-')) {
                    const newId =
                        oldId.replace(
                            'preview-proj1-',
                            `preview-proj${newIndex}-`
                        );
                    el.id = newId;
                    el.textContent = '';
                }
            });

            previewProjectList.appendChild(previewClone);

            bindField(
                `input-proj${newIndex}-name`,
                `preview-proj${newIndex}-name`
            );
            bindField(
                `input-proj${newIndex}-period`,
                `preview-proj${newIndex}-period`
            );
            bindField(
                `input-proj${newIndex}-desc`,
                `preview-proj${newIndex}-desc`
            );
            setupProjectRoleTechBinding(newIndex);

            projCount = newIndex;
        };

        addProjectBtn.addEventListener('click', addProject);

        // 로딩 시 API에서 데이터 개수에 맞게 호출할 수 있도록 전역 함수로 노출
        window.addProjectBtnClick = addProject;
    }

    // ------------------------------------------------------------
    // 템플릿 캐러셀 (<, >, 점 6개)
    // ------------------------------------------------------------
    const TEMPLATE_COUNT = 6;
    let currentTemplateIndex = 0;

    const templateIndicator = document.getElementById('template-indicator');
    const prevTemplateBtn = document.getElementById('prev-template');
    const nextTemplateBtn = document.getElementById('next-template');
    const templateDots = document.querySelectorAll('.template-dot');
    const resumeWrapper = document.querySelector('.resume-wrapper');

    function applyTemplate(index) {
        currentTemplateIndex = index;

        if (templateIndicator) {
            templateIndicator.textContent =
                (index + 1) + ' / ' + TEMPLATE_COUNT;
        }

        templateDots.forEach((dot, i) => {
            dot.classList.toggle('active', i === index);
        });

        if (resumeWrapper) {
            for (let i = 0; i < TEMPLATE_COUNT; i++) {
                resumeWrapper.classList.remove('template-' + (i + 1));
            }
            resumeWrapper.classList.add('template-' + (index + 1));
        }
    }

    if (prevTemplateBtn) {
        prevTemplateBtn.addEventListener('click', () => {
            const nextIndex =
                (currentTemplateIndex - 1 + TEMPLATE_COUNT) % TEMPLATE_COUNT;
            applyTemplate(nextIndex);
        });
    }

    if (nextTemplateBtn) {
        nextTemplateBtn.addEventListener('click', () => {
            const nextIndex =
                (currentTemplateIndex + 1) % TEMPLATE_COUNT;
            applyTemplate(nextIndex);
        });
    }

    templateDots.forEach((dot) => {
        dot.addEventListener('click', () => {
            const idx = parseInt(dot.dataset.index, 10) || 0;
            applyTemplate(idx);
        });
    });

    applyTemplate(0);

    // ------------------------------------------------------------
    // 이력서 목록(내 초안) 페이지로 이동
    // ------------------------------------------------------------
    const backToListBtn = document.getElementById('back-to-list-btn');
    if (backToListBtn) {
        backToListBtn.addEventListener('click', () => {
            window.location.href = '/resume/make';
        });
    }

    // ------------------------------------------------------------
    // 파일 이름 변경 버튼
    // ------------------------------------------------------------
    const renameBtn = document.getElementById('rename-file-btn');
    if (renameBtn && resumeId && hasAuth) {
        renameBtn.addEventListener('click', async () => {
            const nameSpan = document.getElementById('file-name-display');
            const current = (nameSpan?.textContent || '새 이력서').trim();

            const next = window.prompt('새 파일 이름을 입력하세요.', current);
            if (next === null) return;
            const trimmed = next.trim();
            if (!trimmed) {
                alert('파일 이름을 비울 수 없습니다.');
                return;
            }

            try {
                const payload = buildResumePatchPayload(trimmed);

                await apiJson(`/api/resumes/${resumeId}`, {
                    method: 'PATCH',
                    body: JSON.stringify(payload)
                });
                if (nameSpan) {
                    nameSpan.textContent = trimmed;
                }
                alert('파일 이름이 변경되었습니다.');
            } catch (err) {
                console.error('파일 이름 변경 실패:', err);
                alert('파일 이름 변경 중 오류가 발생했습니다.\n' + err.message);
            }
        });
    }

    // ------------------------------------------------------------
    // 상단 "저장하기" 버튼
    // 2233076 12주차 수정: alert를 토스트 메시지로 변경
    // ------------------------------------------------------------
    const saveFileBtn = document.getElementById('save-file-btn');
    if (saveFileBtn) {
        saveFileBtn.addEventListener('click', async () => {
            if (!resumeId) {
                showToast('이력서 ID가 없습니다.', 'error', 3000);
                return;
            }
            if (!hasAuth) {
                showToast('Auth 유틸을 찾을 수 없습니다.', 'error', 3000);
                return;
            }

            const originalText = saveFileBtn.textContent;
            saveFileBtn.disabled = true;
            saveFileBtn.textContent = '저장 중...';

            try {
                const nameSpan = document.getElementById('file-name-display');
                const currentTitle =
                    (nameSpan?.textContent || '새 이력서').trim();

                const payload = buildResumePatchPayload(currentTitle);

                await apiJson(`/api/resumes/${resumeId}`, {
                    method: 'PATCH',
                    body: JSON.stringify(payload)
                });

                await saveAllSections(resumeId);
                showToast('이력서 내용이 저장되었습니다.', 'success', 2000);
            } catch (err) {
                console.error(err);
                showToast('저장 중 오류가 발생했습니다.', 'error', 3000);
            } finally {
                saveFileBtn.disabled = false;
                saveFileBtn.textContent = originalText;
            }
        });
    }

    // ------------------------------------------------------------
    // 우측 하단 "이력서 생성하기" 버튼 (최종 저장 + 목록으로 이동)
    // ------------------------------------------------------------
    const createResumeBtn = document.querySelector('.create-btn');
    if (createResumeBtn) {
        createResumeBtn.addEventListener('click', async () => {
            if (!resumeId) {
                alert('이력서 ID가 없습니다.');
                return;
            }
            if (!hasAuth) {
                alert('Auth 유틸을 찾을 수 없습니다.');
                return;
            }

            const nameSpan = document.getElementById('file-name-display');
            const current = (nameSpan?.textContent || '새 이력서').trim();
            const answer =
                window.prompt('저장할 이력서 이름을 입력하세요.', current);
            if (answer === null) return;
            const title = answer.trim();
            if (!title) {
                alert('파일 이름은 비어 있을 수 없습니다.');
                return;
            }

            const originalText = createResumeBtn.textContent;
            createResumeBtn.disabled = true;
            createResumeBtn.textContent = '저장 중...';

            try {
                const payload = buildResumePatchPayload(title);

                await apiJson(`/api/resumes/${resumeId}`, {
                    method: 'PATCH',
                    body: JSON.stringify(payload)
                });

                if (nameSpan) {
                    nameSpan.textContent = title;
                }

                await saveAllSections(resumeId);

                window.location.href = '/resume/make';
            } catch (err) {
                console.error('이력서 생성(최종 저장) 중 오류:', err);
                alert('이력서 저장 중 오류가 발생했습니다.\n' + err.message);
            } finally {
                createResumeBtn.disabled = false;
                createResumeBtn.textContent = originalText;
            }
        });
    }

    // ------------------------------------------------------------
    // 템플릿 미리보기 그대로 A4 PDF로 다운로드
    //   - html2canvas + jsPDF UMD 버전 모두 대응(window.jspdf 또는 window.jsPDF)
    // ------------------------------------------------------------
    const downloadPdfBtn = document.getElementById('download-pdf-btn');
    if (downloadPdfBtn && resumeWrapper) {
        downloadPdfBtn.addEventListener('click', async () => {
            // html2canvas / jsPDF 전역 체크
            const hasHtml2 = typeof window.html2canvas === 'function' ||
                (window.html2canvas && typeof window.html2canvas.default === 'function');
            const hasJsPdf = !!(window.jspdf || window.jsPDF);

            if (!hasHtml2 || !hasJsPdf) {
                console.error('html2canvas:', window.html2canvas);
                console.error('jspdf(global):', window.jspdf, window.jsPDF);
                alert(
                    'PDF 생성 라이브러리가 로드되지 않았습니다.\n' +
                    '학교/PC에서 CDN을 막는 경우일 수 있습니다.'
                );
                return;
            }

            // html2canvas 함수 가져오기
            let html2c;
            if (typeof window.html2canvas === 'function') {
                html2c = window.html2canvas;
            } else {
                html2c = window.html2canvas.default;
            }

            // jsPDF 생성자 가져오기 (UMD / 구버전 둘 다 대응)
            const JsPdfCtor = window.jspdf ? window.jspdf.jsPDF : window.jsPDF;

            const originalText = downloadPdfBtn.textContent;
            downloadPdfBtn.disabled = true;
            downloadPdfBtn.textContent = 'PDF 생성 중...';

            try {
                // DOM → 캔버스 (화질을 위해 scale 2)
                const canvas = await html2c(resumeWrapper, {
                    scale: 2,
                    useCORS: true,
                    scrollX: 0,
                    scrollY: 0,
                    windowWidth: document.documentElement.clientWidth,
                    windowHeight: document.documentElement.clientHeight
                });

                const imgData = canvas.toDataURL('image/png');

                const pdf = new JsPdfCtor('p', 'mm', 'a4');

                const pdfWidth = pdf.internal.pageSize.getWidth();
                const pdfHeight = pdf.internal.pageSize.getHeight();

                const imgWidth = canvas.width;
                const imgHeight = canvas.height;
                const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);

                const renderWidth = imgWidth * ratio;
                const renderHeight = imgHeight * ratio;

                const marginX = (pdfWidth - renderWidth) / 2;
                const marginY = (pdfHeight - renderHeight) / 2;

                pdf.addImage(imgData, 'PNG', marginX, marginY, renderWidth, renderHeight);

                const filename = resumeId
                    ? `resume-${resumeId}.pdf`
                    : 'resume.pdf';

                pdf.save(filename);
            } catch (err) {
                console.error('PDF 생성 실패:', err);
                alert('PDF 생성 중 오류가 발생했습니다.\n' + err.message);
            } finally {
                downloadPdfBtn.disabled = false;
                downloadPdfBtn.textContent = originalText;
            }
        });
    }

    // ------------------------------------------------------------
    // 로그인한 사용자 정보 우측 상단 표시
    // ------------------------------------------------------------
    function loadCurrentUserInfo() {
        const nameEl = document.getElementById('current-user-name');
        const emailEl = document.getElementById('current-user-email');
        if (!nameEl || !emailEl) return;

        if (window.Auth && typeof window.Auth.fetchMe === 'function') {
            Auth.fetchMe()
                .then((user) => {
                    if (!user) return;
                    if (user.name) {
                        nameEl.textContent = user.name;
                    }
                    if (user.email) {
                        emailEl.textContent = user.email;
                    }
                })
                .catch(() => {
                    // 실패시 기본 더미 텍스트 유지
                });
        }
    }

    loadCurrentUserInfo();

    // ------------------------------------------------------------
    // 페이지 진입 시: 이력서 메타 + 각 섹션을 DB에서 읽어와서 화면에 채우기
    // ------------------------------------------------------------
    if (resumeId) {
        loadResumeMeta(resumeId);
        loadEducationSectionFromApi(resumeId);
        loadExperienceSectionsFromApi(resumeId);
        loadProjectSectionsFromApi(resumeId);
        loadSkillSectionsFromApi(resumeId);
    }
});
