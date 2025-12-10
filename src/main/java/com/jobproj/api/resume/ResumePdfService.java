package com.jobproj.api.resume;

import com.jobproj.api.dto.ResumeDto;
import com.jobproj.api.dto.ResumeDto.Response;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
// 융합프로젝트 김태형 12주차 : Thymeleaf 템플릿 엔진
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
// 융합프로젝트 김태형 12주차 : HTML → PDF 변환(Flying Saucer)
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * 융합프로젝트 김태형 12주차 :
 *  - 이력서 HTML 템플릿을 PDF로 변환하는 서비스
 *  - 템플릿 번호(1~6)에 따라 다른 HTML 파일을 사용할 수 있게 설계.
 *  - 현재는 pdf-template-1.html만 구현되어 있고, 나머지는 1번으로 강제한다.
 */
@Service
public class ResumePdfService {

  // 융합프로젝트 김태형 12주차 : 이력서 기본 정보 조회용 서비스
  private final ResumeService resumeService;

  // 융합프로젝트 김태형 12주차 : Thymeleaf 템플릿 엔진
  private final SpringTemplateEngine templateEngine;

  public ResumePdfService(ResumeService resumeService, SpringTemplateEngine templateEngine) {
    this.resumeService = resumeService;
    this.templateEngine = templateEngine;
  }

  /**
   * 융합프로젝트 김태형 12주차 :
   *  - 이력서 ID + 사용자 ID 로 소유권을 검사하고,
   *    해당 이력서를 HTML 템플릿으로 렌더링한 뒤 PDF 바이트로 변환한다.
   *  - templateIndex 에 따라 "resume/pdf-template-{번호}.html" 을 사용.
   *
   * @param resumeId      이력서 ID
   * @param usersId       로그인 사용자 ID (소유권 체크용)
   * @param templateIndex 선택된 템플릿 번호 (1~6)
   * @return 생성된 PDF 바이트 (이력서가 없거나 소유자가 아니면 null)
   */
  public byte[] generateResumePdf(Long resumeId, Long usersId, int templateIndex) {
    // 소유권 검사 + 이력서 조회
    Optional<Response> resumeOpt = resumeService.get(resumeId, usersId);
    if (resumeOpt.isEmpty()) {
      // 이력서가 없거나 소유자가 아닌 경우 → 컨트롤러에서 404 로 응답
      return null;
    }

    Response resume = resumeOpt.get(); // public 필드(title, summary 등)를 그대로 사용

    try {
      // --------------------------------------------------------
      // 융합프로젝트 김태형 12주차 :
      //  1) 사용할 템플릿 이름 결정
      //     - 아직은 1번만 구현되어 있어서 1~6 넘어와도 전부 1번으로 고정.
      // --------------------------------------------------------
      int idx = (templateIndex < 1 || templateIndex > 6) ? 1 : templateIndex;
      String templateName = "resume/pdf-template-" + idx;

      // --------------------------------------------------------
      // 융합프로젝트 김태형 12주차 :
      //  2) Thymeleaf 로 HTML 렌더링
      // --------------------------------------------------------
      Context ctx = new Context(Locale.KOREA);
      ctx.setVariable("resume", resume);   // 템플릿에서 ${resume.title}, ${resume.summary} 등으로 사용
      ctx.setVariable("templateIndex", idx);

      String html = templateEngine.process(templateName, ctx);

      // --------------------------------------------------------
      // 융합프로젝트 김태형 12주차 :
      //  3) Flying Saucer 로 HTML → PDF 변환
      // --------------------------------------------------------
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html);
      renderer.layout();
      renderer.createPDF(baos);
      renderer.finishPDF();

      return baos.toByteArray();

    } catch (Exception e) {
      // PDF 생성에 실패하면 null 반환 → 컨트롤러에서 500 등으로 처리 가능
      return null;
    }
  }
}
