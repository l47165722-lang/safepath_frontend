# 🛡️ SafePath (세이프패스)

> **"오늘도, 안전한 길로"**  
> 실시간 사용자 위치 기반의 안전 경로 안내 및 보호자 안심 연결 안드로이드 애플리케이션입니다.

---

## 🌟 주요 기능 (Key Features)

- 🗺️ **실시간 지도 내비게이션**: Mapbox Maps SDK 기반으로 사용자의 현재 위치 및 지도를 실시간으로 제공합니다.
- 🛣️ **맞춤형 경로 안내**: 안전 경로(CCTV/비상벨/지킴이집 우선), 최단 경로, 추천 경로 선택 기능을 제공합니다.
- 🛡️ **보호자 안심 서비스**: 긴급 상황 발생 시 등록된 보호자에게 현재 위경도 위치 메시지를 간편하게 전송 및 공유합니다.
- ⚙️ **권한 및 안심 설정**: GPS 위치 서비스 권한 관리 및 개인정보 안심 설정을 제공합니다.

---

## 🛠️ 기술 스택 (Tech Stack)

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Map SDK**: Mapbox Maps SDK for Android Compose (`11.30.0`)
- **Architecture**: Package by Feature / Layer Architecture
- **Build Tool**: Gradle (Kotlin DSL, `libs.versions.toml`)
- **Compatibility**: Android Min SDK 24 / Target SDK 37 (Java 11)

---

## 📂 프로젝트 폴더 구조 (Project Architecture)

```text
safepathtest1/
├── 📂 app/
│   ├── 📄 build.gradle.kts
│   └── 📂 src/
│       └── 📂 main/
│           ├── 📄 AndroidManifest.xml
│           └── 📂 java/com/example/safepath_test1/
│               ├── 📱 MainActivity.kt             # 메인 진입점 Activity
│               ├── 🧭 SafePathApp.kt              # 최상위 앱 Scaffold & 탭 내비게이션
│               │
│               ├── 📂 location/                  # 위치 유틸리티 패키지
│               │   ├── LocationShareFormatter.kt # 위치 공유 메시지 생성
│               │   └── LocationSharing.kt        # 외부 앱 위치 공유 연동
│               │
│               ├── 📂 model/                     # 데이터 데이터 모델
│               │   └── GeoPoint.kt               # 위경도 좌표 구조체
│               │
│               └── 📂 ui/                        # UI 레이어
│                   ├── SafePathTab.kt            # 하단 탭 내비게이션 모델
│                   │
│                   ├── 📂 components/            # 공통 UI 컴포넌트
│                   │   ├── PageHeader.kt         # 공통 타이틀 헤더
│                   │   └── SafePathBottomBar.kt  # 하단 플로팅 네비게이션 바
│                   │
│                   ├── 📂 home/                  # 홈 화면 (경로 검색 및 메인 UI)
│                   │   └── HomeScreen.kt
│                   │
│                   ├── 📂 map/                   # 지도 연동 패키지
│                   │   └── SafePathMapboxView.kt # Mapbox Compose 뷰
│                   │
│                   ├── 📂 guardian/              # 보호자 안심 화면
│                   │   └── GuardianScreen.kt
│                   │
│                   ├── 📂 settings/              # 설정 화면
│                   │   └── SettingsScreen.kt
│                   │
│                   └── 📂 theme/                 # 테마 및 컬러
│                       └── SafePathTheme.kt
└── 📄 README.md
---
📱 Android SDK 및 자바 버전

minSdk: 24 (Android 7.0 Nougat 이상)
compileSdk: 37 (Android API Level 37)
targetSdk: 37 (Android API Level 37)
Java Compatibility: Java 11 (JavaVersion.VERSION_11)
실행 JDK (Launcher JVM): Java 17 (17.0.19 Microsoft OpenJDK)

⚙️ 빌드 도구 및 언어 버전

Android Gradle Plugin (AGP): 9.3.2
Gradle: 9.5.0
Kotlin: 2.0.21
Jetpack Compose BOM: 2025.03.00
Activity Compose: 1.10.1

📦 주요 라이브러리 버전

Mapbox Maps SDK: 11.30.0 (com.mapbox.maps:android-ndk27:11.30.0)
Mapbox Extension Compose: 11.30.0 (com.mapbox.extension:maps-compose-ndk27:11.30.0)
AndroidX Core KTX: 1.19.0
AndroidX AppCompat: 1.6.1
Google Material: 1.10.0
---

## 🚀 시작하기 (Getting Started)

1. 리포지토리를 클론합니다.
   ```bash
   git clone https://github.com/your-username/safepathtest1.git
   ```
2. 프로젝트 루트 폴더의 `local.properties` 파일에 Mapbox Access Token을 설정합니다.
   ```properties
   MAPBOX_ACCESS_TOKEN=your_mapbox_access_token_here
   ```
3. Android Studio에서 프로젝트를 열고 실행합니다.
