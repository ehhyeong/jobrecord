package com.jobproj.api.attachment;

import com.jobproj.api.attachment.AttachmentDto.CreateRequest;
import com.jobproj.api.attachment.AttachmentDto.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

  private final AttachmentRepository repo;

  public AttachmentService(AttachmentRepository repo) {
    this.repo = repo;
  }

  private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "pdf");
  private static final Set<String> ALLOWED_CT =
      Set.of("image/png", "image/jpeg", "application/pdf");

  private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

  private static String extOf(String filename) {
    if (filename == null) return "";
    String base = Path.of(filename).getFileName().toString();
    int dot = base.lastIndexOf('.');
    return (dot < 0) ? "" : base.substring(dot + 1).toLowerCase();
  }

  private static boolean looksLikeExe(byte[] head) {
    return head != null && head.length >= 2 && head[0] == 'M' && head[1] == 'Z';
  }

  /** 메타데이터 직접 생성 */
  public long create(CreateRequest r) {
    return repo.create(r);
  }

  /** 실제 파일 업로드 + 메타 등록 */
  @Transactional
  public long upload(MultipartFile file, Long resumeId) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("file is empty");
    }
    if (file.getSize() > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException("file too large (max 10MB)");
    }

    String originalName = file.getOriginalFilename();
    String ext = extOf(originalName);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    if (!ALLOWED_EXT.contains(ext) || !ALLOWED_CT.contains(contentType)) {
      throw new IllegalArgumentException("unsupported file type");
    }

    try (var is = file.getInputStream()) {
      byte[] head = is.readNBytes(4);
      if (looksLikeExe(head)) {
        throw new IllegalArgumentException("executable file blocked");
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read file head", e);
    }

    // 1) 로컬 디스크에 저장 (프로젝트 루트 /uploads)
    Path uploadDir = Paths.get("uploads");
    try {
      Files.createDirectories(uploadDir);
    } catch (IOException e) {
      throw new RuntimeException("failed to create upload dir", e);
    }

    // 저장 파일명: UUID_원본파일명
    String safeName =
        (originalName == null) ? ("file." + ext) : Path.of(originalName).getFileName().toString();
    String storedName = UUID.randomUUID() + "_" + safeName;
    Path dest = uploadDir.resolve(storedName);

    try {
      Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("failed to save file", e);
    }

    // 2) 스토리지 키(혹은 경로) — 로컬에선 파일명만 보관
    //    (S3로 확장 시 이 자리에 S3 object key를 저장)
    String storageKey = storedName;

    // 3) 메타데이터 저장 (profile 이미지는 기본 false)
    CreateRequest meta =
        new CreateRequest(
            resumeId == null ? 0L : resumeId, // FK 제약 쓰는 경우 실제 존재하는 resumeId를 넣어야 함 (기존)
            safeName,
            contentType,
            file.getSize(),
            storageKey,
            false);

    return repo.create(meta);
  }

  public List<Response> listByResume(long resumeId) {
    return repo.listByResume(resumeId);
  }

  public boolean delete(long id) {
    var metaOpt = repo.findById(id);
    int rows = repo.delete(id);
    if (rows > 0 && metaOpt.isPresent()) {
      try {
        Path p = Paths.get("uploads").resolve(metaOpt.get().storageKey());
        Files.deleteIfExists(p);
      } catch (IOException e) {
        // 실제 파일 삭제 실패는 비치명적이므로 무시 또는 로깅
        // log.warn("failed to delete file: {}", metaOpt.get().storageKey(), e);
      }
      return true;
    }
    return false;
  }

  /** 프로필 이미지 토글은 트랜잭션으로 감싸 안전하게 */
  @Transactional
  public boolean setProfile(long resumeId, long attachmentId) {
    return repo.setProfile(resumeId, attachmentId) > 0;
  }

  public DownloadPayload prepareDownload(long attachmentId) {
    var meta =
        repo.findById(attachmentId)
            .orElseThrow(() -> new IllegalArgumentException("attachment not found"));

    Path path = Paths.get("uploads").resolve(meta.storageKey());
    if (!Files.exists(path)) {
      throw new IllegalStateException("stored file not found");
    }

    Resource resource = new FileSystemResource(path);

    // 안전 기본값은 application/octet-stream, 이미지/PDF면 원래 타입 유지
    String ct =
        (meta.contentType() == null || meta.contentType().isBlank())
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : meta.contentType();

    var cd =
        ContentDisposition.attachment().filename(meta.filename(), StandardCharsets.UTF_8).build();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(cd);
    headers.setContentType(MediaType.parseMediaType(ct));
    try {
      headers.setContentLength(Files.size(path));
    } catch (IOException e) {
      // 길이 계산 실패 시 생략
    }

    return new DownloadPayload(resource, headers);
  }

  public record DownloadPayload(Resource resource, HttpHeaders headers) {}
}
