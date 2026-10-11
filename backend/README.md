# 카카오톡 친구 요청

앱의 **카카오톡으로 요청 보내기** 버튼은 서버에서 링크를 만든 뒤 Android ACTION_SEND를 카카오톡 패키지에 지정하여 받는 친구·채팅방 선택 화면을 엽니다. 사용자가 메시지를 전송합니다. 알림톡 채널, 승인 템플릿, 발송 서비스 계정은 필요하지 않습니다.

## 서버 연결

기존 Firebase 프로젝트 `sidepj-ba5ab`를 사용합니다. Firebase Hosting과 Functions(asia-northeast3), Firestore 및 Google Authentication을 활성화하고 Hosting 도메인을 인증 허용 도메인에 등록합니다. Functions 사용에는 해당 프로젝트의 결제 플랜과 배포 권한이 필요합니다.

1. `backend/functions`에서 `npm ci`, `npm test`를 실행합니다.
2. Firebase CLI로 프로젝트에 로그인 후 `backend`에서 `firebase deploy --only functions:friends,hosting --project sidepj-ba5ab`을 실행합니다. 이 프로젝트는 `friends` 코드베이스에 `friendsApi` 함수를 정의합니다.
3. Firestore 규칙은 `firestore.rules`의 친구 관련 범위를 기존 프로젝트 규칙에 병합하여 배포합니다. 기존 전체 규칙을 덮어쓰지 않습니다.
4. 프로젝트 `local.properties`에 `FRIENDS_API_BASE_URL=https://sidepj-ba5ab.web.app/api`를 설정하고 앱을 다시 빌드합니다. 인증키는 추가로 필요하지 않습니다.

2026-10-11에 `friendsApi`와 Hosting을 실제 배포하고 서울 리전의 기본 Firestore 데이터베이스를 생성했습니다. 앱의 `local.properties`에도 위 API 주소가 설정되어 있습니다. 수락 페이지의 HTTP 200과 비로그인 API의 HTTP 401을 확인했습니다. 실제 로그인 계정으로 카카오톡 공유·수락하는 검증은 별도로 필요합니다.

## 링크와 수락

`POST /api/requests/share`는 인증된 요청자의 프로필과 재시도 식별자로 7일 유효 링크를 만듭니다. 하루 20개 한도와 동일 식별자 재사용을 보장합니다. 생성 상태 `ready`는 메시지 전송 완료를 의미하지 않습니다. 카카오톡 취소·미설치 시 같은 링크로 다시 열 수 있습니다.

받는 사람은 링크에서 요청자 이름과 마스킹된 연락처를 확인하고 앱과 동일한 Google 계정으로 로그인합니다. 로그인한 링크 확인 계정에 받은 요청이 표시되며 수락은 양쪽 친구 관계를 한 트랜잭션으로 저장합니다. 공유 링크에는 전화번호 수신자 지정이 없으므로 먼저 수락하는 계정 한 명과 연결됩니다. 다른 사람에게 링크를 전달하면 그 사람도 확인·수락할 수 있습니다. 한 친구에게 보내고 앱 알림에서 불필요한 요청을 취소하세요.

카카오톡 공유는 선택한 친구의 계정·전화번호나 실제 전송 결과를 앱에 반환하지 않습니다. 수락 전 보낸 목록은 ‘카카오톡 친구’로 표시합니다. 수락자의 Firebase 이름을 사용하며 인증된 전화번호가 없는 계정의 연락처는 비워 둡니다. 기존 전화번호 지정 요청은 기존 본인 인증 조건을 유지합니다.

실제 검증: Android 앱 빌드·단위 테스트·Lint 및 서버 테스트 결과는 `../docs/ui-review/REPORT.md`에 기록합니다. 실기기 카카오톡 메시지 전송과 배포된 링크 수락은 별도로 확인해야 합니다.
