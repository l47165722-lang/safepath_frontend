# SafePath

SafePath는 Android와 Wear OS를 위한 **안전 귀가 경로 탐색 프로토타입**입니다. 현재 위치와 목적지 사이의 도보 경로를 비교하고, 앱에 포함된 CCTV·가로등 데이터를 바탕으로 안전도를 반영한 경로를 제시합니다.

> 현재는 데모/개발 단계입니다. SOS 신고, 보호자 자동 알림, 위험지역 자동 회피 등은 운영 기능으로 완성되어 있지 않습니다.

## 주요 기능

- **Google 로그인 또는 데모 진입**: Firebase Authentication 기반 Google 로그인과 로그인 없이 화면을 확인하는 데모 진입을 제공합니다.
- **현재 위치와 지도**: 위치 권한을 받아 GPS·네트워크 위치를 갱신하고 Kakao Maps SDK v2로 지도, 현재 위치, 목적지, 안전시설, 경로를 표시합니다.
- **장소 검색**: Kakao Local REST API로 출발지·목적지를 검색합니다.
- **경로 비교**: Mapbox Directions API의 도보 경로 후보를 받아 `안전`, `최단`, `추천` 경로를 비교합니다.
- **안전도 산정**: 경로 주변의 CCTV와 가로등 밀도를 점수화하고, 안전 경로는 시설 비중을, 추천 경로는 시설과 거리의 균형을 더 크게 반영합니다.
- **안전지역 분석**: 현재 위치 주변 100m~1,000m 범위의 CCTV·가로등 수와 4단계 안전도를 확인합니다.
- **보호자와 위치 공유**: 보호자 최대 9명을 기기에 저장하고, 현재 위치를 Google Maps 링크로 시스템 공유 시트에 전달합니다.
- **Wear OS 상태 표시**: 연결된 시계에 경로 탐색 상태와 찾은 경로 정보를 Wearable Data Layer(`DataClient`)로 동기화합니다.

## 구현 범위와 유의사항

| 항목 | 현재 동작 |
| --- | --- |
| Google 로그인 | Firebase 및 Google Cloud 설정이 올바르면 작동합니다. 로그인 화면의 데모 진입은 인증을 건너뜁니다. |
| SOS | 확인 대화상자 뒤 시스템 공유 시트를 열어 긴급 문구와 현재 위치를 사용자가 선택한 앱에 전달합니다. 자동 신고·SMS·보호자 자동 전송은 하지 않습니다. |
| 보호자 | `SharedPreferences`에 로컬 저장됩니다. 계정 간 동기화나 자동 알림은 하지 않습니다. |
| 위치 공유 | 사용자가 Android 공유 시트에서 수신 앱과 대상을 직접 선택합니다. |
| 안전시설 | `app/src/main/assets`의 CCTV·가로등 CSV를 사용하며, 현재 위치 주변 데이터만 지도에 제한적으로 표시합니다. |
| 경로 제공자 | 지도/장소 검색은 Kakao, 도보 경로 후보는 Mapbox Directions API를 사용합니다. 따라서 두 서비스의 키가 모두 필요합니다. |
| Wear OS | Firebase가 아닌 Google Play services Wearable Data Layer를 사용합니다. 호환 기기 페어링과 같은 앱 ID가 필요합니다. |

## 기술 구성

- Kotlin, Jetpack Compose, Material 3
- Kakao Maps SDK v2 `2.15.2`
- Kakao Local REST API
- Mapbox Directions API
- Firebase Authentication, Credential Manager, Google ID
- Google Play services Wearable Data Layer
- Gradle Kotlin DSL / Version Catalog

| 구분 | 값 |
| --- | --- |
| 휴대폰 앱 ID | `com.example.safepath_test1` |
| Wear OS 앱 ID | `com.example.safepath_test1` (Data Layer 노드 검색을 위해 휴대폰과 동일) |
| 최소 SDK | Android 7.0 (API 24) / Wear OS API 30 |
| compile / target SDK | 37 |
| JVM 타깃 | Java 11 |
| Gradle 실행 JDK | JDK 17 권장 |
| Gradle / AGP | 9.5.0 / 9.3.2 |

## 시작하기

### 1. 저장소 복제

```bash
git clone https://github.com/l47165722-lang/safepath_frontend.git
cd safepath_frontend
```

Android Studio에서 프로젝트 루트를 열고 Gradle 동기화를 진행합니다. Windows에서는 다음 명령으로 빌드할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :wear:assembleDebug
```

### 2. API 키 설정

프로젝트 루트에 `local.properties`를 만들고 각 서비스의 키를 설정합니다. 이 파일은 Git에서 제외됩니다.

```properties
# Mapbox Directions API용 public access token
MAPBOX_ACCESS_TOKEN=pk.your_mapbox_public_access_token

# Kakao Local REST API용 REST API 키
KAKAO_REST_API_KEY=your_kakao_rest_api_key

# Kakao Maps SDK 초기화용 네이티브 앱 키
KAKAO_NATIVE_APP_KEY=your_kakao_native_app_key
```

`KAKAO_NATIVE_APP_KEY`는 앱 시작 시 Kakao Maps SDK 초기화에 사용되고, `KAKAO_REST_API_KEY`는 장소 검색 요청의 `KakaoAK` 인증 헤더에 사용됩니다. `MAPBOX_ACCESS_TOKEN`이 없으면 도보 경로를 조회할 수 없습니다.

### 3. Google 로그인 설정

Google 로그인에는 Firebase 프로젝트 설정이 필요합니다.

1. Firebase Console에서 Android 앱 패키지 `com.example.safepath_test1`을 등록합니다.
2. Firebase Authentication에서 Google 로그인 제공자를 활성화합니다.
3. Firebase 설정 파일 `google-services.json`을 내려받아 `app/src/google-services.json`에 둡니다.
4. Firebase 프로젝트의 지원 이메일, SHA-1/SHA-256 인증서 지문, OAuth 동의 화면과 Android OAuth 클라이언트를 현재 앱의 서명키에 맞춰 설정합니다.

저장소의 `app/src/google-services.json`은 현재 프로젝트 설정 파일입니다. 다른 Firebase 프로젝트를 사용하거나 배포할 때에는 자신의 설정 파일로 교체하고 키를 공개 저장소에 커밋하지 않도록 주의하세요.

### 4. Wear OS 연동

`wear` 모듈은 휴대폰 앱과 같은 application ID를 사용하며, `DataClient`로 `/safepath/status` 데이터를 전송합니다. 휴대폰과 Wear OS 기기에 같은 빌드 계열의 앱을 설치하고 Google Play services가 동작하는 상태에서 페어링해야 합니다. 시계는 대기·탐색 중·경로 발견 상태와 마지막 업데이트 시각을 표시합니다.

## 실행 방법

1. 휴대폰 앱을 Android 7.0(API 24) 이상 기기 또는 에뮬레이터에 설치합니다.
2. Google 로그인 또는 데모 진입으로 메인 화면을 엽니다.
3. 위치 권한을 허용합니다.
4. 장소 검색 결과를 선택하거나 현재 위치를 출발지로 설정하고 목적지를 지정합니다.
5. 경로를 조회한 뒤 안전·최단·추천 탭을 전환해 비교합니다.
6. 선택 사항으로 Wear OS API 30 이상 기기에 `wear` 모듈을 설치해 상태 동기화를 확인합니다.

## 프로젝트 구조

```text
app/
  src/main/
    assets/                         # CCTV·가로등 CSV 데이터
    java/auth/                      # Firebase Google 로그인
    java/com/example/safepath_test1/
      location/                     # Kakao 장소 검색, Mapbox 경로, 안전도 분석, 위치 공유
      ui/                            # 로그인, 홈, 안전지도, 보호자, 프로필 UI
      wear/WearMessenger.kt          # 휴대폰 → Wear OS DataClient 상태 전송
      SafePathApplication.kt         # Kakao Maps SDK 초기화
      SafePathApp.kt                 # 앱 상태, 권한, 탭 구성
wear/
  src/main/java/.../presentation/    # Wear OS 상태 표시·DataClient 수신
```

## 테스트

```powershell
# 휴대폰 모듈 단위 테스트
.\gradlew.bat :app:testDebugUnitTest

# Wear OS 모듈 단위 테스트
.\gradlew.bat :wear:testDebugUnitTest

# 연결된 Android 기기/에뮬레이터에서 기기 테스트
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 권한과 데이터

- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`: 현재 위치 표시와 출발지 설정
- `INTERNET`: Kakao 지도/장소 검색, Mapbox 경로 요청, Google 로그인, Wearable 서비스 연결
- 보호자와 일부 설정: 기기의 `SharedPreferences`에 로컬 저장
- 위치 공유: 사용자가 직접 실행한 Android 공유 Intent로만 전달

운영 배포 전에는 SOS 처리 흐름, 보호자 알림, 서버 접근 규칙, 개인정보 처리방침, 안전시설 데이터의 출처·최신성 검증을 보완해야 합니다.
