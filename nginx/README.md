# Nginx 설정 안내 (nginx 폴더)

이 폴더에는 JobRecord 프로젝트를 위한 Nginx 리버스 프록시 설정이 들어 있습니다.

## 구조

```text
nginx/
└── conf.d/
    └── jobrecord-local.conf
```

## 기본 아이디어

- 외부에서 `http://<서버 IP 또는 도메인>` 으로 접속하면
- Nginx가 백엔드 스프링 애플리케이션(`jobrecord-backend` 컨테이너, 8080 포트)으로 요청을 프록시합니다.

## 사용 시나리오

- 로컬 Docker Compose 환경:
  - `docker-compose.yml`에서 `jobrecord-nginx` 서비스가 이 설정 파일을 마운트해서 사용
- 서버(EC2, OCI 등) 배포:
  - 동일한 설정을 `/etc/nginx/conf.d`에 배치 후 `nginx -s reload` 로 반영

실제 도메인 / SSL(Let's Encrypt) 설정은 서버 환경에 맞게 별도로 추가해야 합니다.
