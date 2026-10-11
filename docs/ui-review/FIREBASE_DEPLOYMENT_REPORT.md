# 친구 요청 404 수정 — 2026-10-11

앱의 `FRIENDS_API_BASE_URL`은 올바르게 설정되어 있었지만, 해당 Hosting URL이 `Site Not Found` HTML과 HTTP 404를 반환했습니다. Hosting 사이트는 등록되어 있었으며 공개 배포가 없었습니다.

수행 작업:

- 설정된 `friends` 코드베이스로 `friendsApi`를 서울 리전에 생성했습니다.
- 함수 생성 이후 Artifact Registry 정리 정책 미설정으로 합동 배포 명령이 종료되어 Hosting을 별도 배포했습니다. 이미지 정리 정책은 아직 설정하지 않았습니다.
- 기본 Firestore 데이터베이스가 없어서 서울 리전(`asia-northeast3`)에 Standard / Native 데이터베이스를 생성했습니다. 기본 클라이언트 접근 차단 규칙을 유지하며 Admin API가 데이터를 처리합니다.
- Hosting 정적 페이지와 `/api/**` 함수 rewrite를 공개했습니다.
- 설정 안내의 배포 필터를 `functions:friendsApi`에서 `functions:friends`로 수정했습니다.

검증:

- `https://sidepj-ba5ab.web.app/`: HTTP 200.
- `https://sidepj-ba5ab.web.app/invite/`: HTTP 200.
- 비로그인 `POST /api/requests/share`: HTTP 401과 JSON `Google 로그인이 필요합니다.` — 요청 생성 라우트에 정상 도달.
- 비로그인 `GET /api/friends`: HTTP 401과 동일 인증 안내 — 목록 라우트에 정상 도달.

실제 사용자 계정으로 친구 요청 생성·카카오톡 전송·수락은 검증하지 않았습니다. 앱의 서버 주소는 동일하므로 배포 수정에 따른 앱 재설치는 필요하지 않습니다.
