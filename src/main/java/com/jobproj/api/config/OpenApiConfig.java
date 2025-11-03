package com.jobproj.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    final String bearer = "BearerAuth";
    return new OpenAPI()
        .info(new Info()
            .title("JobRecord API")
            .description("이력서/인증/파일 API 문서")
            .version("v1"))
        // 🔽 Swagger 상단 Servers 드롭다운에 두 개의 서로 다른 오리진을 노출
        .servers(List.of(
            new Server().url("http://localhost:8080").description("localhost"),
            new Server().url("http://127.0.0.1:8080").description("127.0.0.1")
        ))
        .addSecurityItem(new SecurityRequirement().addList(bearer))
        .components(new Components().addSecuritySchemes(
            bearer,
            new SecurityScheme()
                .name(bearer)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        ));
  }

  // ✅ 1) 인증
  @Bean
  public GroupedOpenApi authGroup() {
    return GroupedOpenApi.builder()
        .group("1. 인증")
        .pathsToMatch("/auth/**")
        .build();
  }

  // ✅ 2) 이력서/섹션 (파일/인증은 제외)
  @Bean
  public GroupedOpenApi resumeGroup() {
    return GroupedOpenApi.builder()
        .group("2. 이력서")
        .pathsToMatch("/api/**")
        .pathsToExclude("/api/attachments/**", "/auth/**")
        .build();
  }

  // ✅ 3) 파일(첨부)
  @Bean
  public GroupedOpenApi fileGroup() {
    return GroupedOpenApi.builder()
        .group("3. 파일")
        .pathsToMatch("/api/attachments/**", "/attachments/**")
        .build();
  }
}
