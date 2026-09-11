# SafePath (세이프패스)

> 오늘도, 안전한 길로.

SafePath는 현재 위치, CCTV·가로등 데이터, Mapbox 경로를 활용해 안전한 이동을 돕는 Android 애플리케이션입니다.

## 현재 구현된 기능

- Mapbox 지도와 현재 위치 표시
- 출발지·도착지 지도 선택 및 도보 경로 조회
- 안전 / 최단 / 추천 경로 선택
- CCTV와 가로등 안전시설 표시 및 반경 기반 안전지수 계산
- 보호자 정보 로컬 등록·삭제
- 현재 위치를 외부 앱으로 공유하는 메시지 생성
- 위치·안전 관련 설정 화면

> SOS, 위험지역 회피, 위험 알림, 경찰시설·비상벨 연동은 화면 또는 설정 항목은 있으나 실제 자동 전달/데이터 연동 기능은 아직 구현 중입니다. 구현 전에는 완료된 기능처럼 안내하지 않습니다.

## 프로젝트 구조

```text
safepath_frontend/
├── README.md
├── build.gradle.kts                    # 최상위 Gradle 설정
├── settings.gradle.kts                 # 모듈·저장소 설정
├── gradle/
│   ├── libs.versions.toml              # 의존성·플러그인 버전 관리
│   └── wrapper/                        # Gradle Wrapper
└── app/
    ├── build.gradle.kts                # Android 앱 설정, Mapbox 토큰 주입
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     # 앱·위치·인터넷 권한
        │   ├── assets/
        │   │   ├── cctv.csv            # CCTV 좌표 데이터 (2,900건)
        │   │   └── streetlight.csv     # 가로등 좌표 데이터 (74,269건)
        │   ├── java/com/example/safepath_test1/
        │   │   ├── MainActivity.kt     # Android 진입 Activity
        │   │   ├── SafePathApp.kt      # 앱 상태, 권한, 탭 전환
        │   │   ├── model/
        │   │   │   ├── GeoPoint.kt     # 위도·경도 모델
        │   │   │   └── PlaceSelection.kt # 출발지·도착지 모델
        │   │   ├── location/
        │   │   │   ├── NavigationRepository.kt   # Mapbox 경로 조회·안전 점수
        │   │   │   ├── SafetyRepository.kt       # 시설 CSV 로드·분석
        │   │   │   ├── LocationSharing.kt        # Android 공유 Intent
        │   │   │   └── LocationShareFormatter.kt # 공유 메시지 형식
        │   │   └── ui/
        │   │       ├── SafePathTab.kt            # 하단 탭 정의
        │   │       ├── components/                # 공통 헤더·하단 바
        │   │       ├── home/HomeScreen.kt         # 지도·경로 탐색 화면
        │   │       ├── map/SafePathMapboxView.kt  # Mapbox 레이어·마커 렌더링
        │   │       ├── safetymap/SafetyMapScreen.kt # 반경 안전지수 화면
        │   │       ├── guardian/GuardianScreen.kt # 보호자 관리 화면
        │   │       ├── profile/ProfileScreen.kt   # 내 정보 화면
        │   │       ├── settings/SettingsScreen.kt # 설정 화면
        │   │       └── theme/SafePathTheme.kt     # 색상·타이포그래피·테마
        │   ├── res/                               # 문자열, 테마, 앱 아이콘
        │   └── keepRules/                         # 빌드 keep 규칙
        ├── test/                                  # 로컬 단위 테스트
        └── androidTest/                           # 기기/에뮬레이터 테스트
```

## 시작하기

### 1. 저장소 복제

```bash
git clone https://github.com/l47165722-lang/safepath_frontend.git
cd safepath_frontend
```

### 2. Mapbox 토큰 설정

프로젝트 루트에 `local.properties` 파일을 만들고 본인 토큰을 넣습니다.

```properties
MAPBOX_ACCESS_TOKEN=your_mapbox_access_token_here
```

`local.properties`와 실제 토큰은 절대 커밋하거나 PR에 올리지 않습니다.

### 3. 실행

Android Studio에서 프로젝트 폴더를 연 뒤 Gradle 동기화가 끝나면 에뮬레이터 또는 연결된 기기에서 실행합니다.

## PR 협업 가이드

이 프로젝트는 `main`에 직접 푸시하지 않고, 모든 변경을 Pull Request(PR)로 합칩니다. PR은 “내 작업을 메인 코드에 합쳐도 되는지” 서로 확인하는 페이지입니다.

### 작업 시작 전: 최신 코드 받기

```bash
git switch main
git pull origin main
```

### 기능별 브랜치 만들기

한 브랜치에는 한 가지 목적만 담습니다. 브랜치 이름은 아래 형식을 사용합니다.

```bash
git switch -c feat/guardian-edit
```

- `feat/기능명`: 새 기능
- `fix/문제명`: 버그 수정
- `docs/내용`: README·문서 수정
- `refactor/내용`: 동작을 바꾸지 않는 구조 개선

### 변경 저장 및 원격에 올리기

```bash
git status
git add app/src/main/java/com/example/safepath_test1/ui/guardian/GuardianScreen.kt
git commit -m "feat: 보호자 정보 수정 기능 추가"
git push -u origin feat/guardian-edit
```

처음에는 `git add .`보다 바뀐 파일을 직접 지정하는 편이 실수를 줄입니다. 특히 `local.properties`, 토큰, 개인 파일은 올리지 않습니다.

### GitHub에서 PR 만들기

1. GitHub 저장소를 열고 **Compare & pull request**를 누릅니다.
2. `base`가 `main`, `compare`가 내 브랜치인지 확인합니다.
3. 제목은 한 줄로 작업 내용을 씁니다. 예: `feat: 보호자 정보 수정 기능 추가`
4. 설명에는 아래 템플릿을 붙여 넣습니다.
5. 상대가 리뷰를 마친 뒤 **Merge pull request**로 합칩니다.

```md
## 변경 내용
-

## 테스트 방법
-

## 확인이 필요한 점
- 없음
```

### 리뷰할 때 약속

- 작성자는 PR을 올린 뒤 본인이 바꾼 화면을 직접 실행해 봅니다.
- 리뷰어는 이해가 안 되는 부분도 편하게 질문합니다. 질문은 비난이 아니라 코드 공유를 위한 과정입니다.
- 리뷰가 끝난 PR만 `main`에 합칩니다.
- PR을 합친 뒤에는 각자 `main`으로 이동해 최신 코드를 받습니다.

```bash
git switch main
git pull origin main
git branch -d feat/guardian-edit
```

### 충돌이 났을 때

같은 줄을 두 사람이 수정하면 Git이 자동으로 합치지 못할 수 있습니다. 당황하지 말고 PR에서 충돌 난 파일을 확인한 뒤, 먼저 작업한 사람에게 의도를 물어보고 함께 해결합니다. 해결 후에는 다시 커밋하고 같은 브랜치에 푸시하면 PR이 자동으로 갱신됩니다.

## 버전 정보

### Android SDK 및 Java 버전

- minSdk: 24 (Android 7.0 Nougat 이상)
- compileSdk: 37 (Android API Level 37)
- targetSdk: 37 (Android API Level 37)
- Java Compatibility: Java 11 (JavaVersion.VERSION_11)
- 실행 JDK (Launcher JVM): Java 17 (17.0.19 Microsoft OpenJDK)

### 빌드 도구 및 언어 버전

- Android Gradle Plugin (AGP): 9.3.2
- Gradle: 9.5.0
- Kotlin: 2.0.21
- Jetpack Compose BOM: 2025.03.00
- Activity Compose: 1.10.1

### 주요 라이브러리 버전

- Mapbox Maps SDK: 11.30.0 (`com.mapbox.maps:android-ndk27:11.30.0`)
- Mapbox Extension Compose: 11.30.0 (`com.mapbox.extension:maps-compose-ndk27:11.30.0`)
- AndroidX Core KTX: 1.19.0
- AndroidX AppCompat: 1.6.1
- Google Material: 1.10.0
