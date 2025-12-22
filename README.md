# TablePick-BackEnd

> 이 레포는 **팀 프로젝트**로 진행한 식당 예약·결제 플랫폼에서  
> 제가 맡았던 **예약·결제 도메인**을 중심으로 **개인적으로 정리**해 둔 버전입니다.  
> (원본 팀 프로젝트: [링크](https://github.com/orgs/4und-Cloud/repositories))

## 🤝 협업 문서
프로젝트 진행 일정, 와이어프레임, 협업 전략 등은 별도 문서에 정리했습니다.  
👉 [협업 아카이브 바로가기](https://young-shallot-30c.notion.site/27859cec87688005aa10dec9345d90f7?source=copy_link)

---

## 🔗 관련 레포지토리
- [TablePick-Notification-Server](https://github.com/OHEUNSOL/TablePick-Notification)  
  → 예약 서비스와 분리된 **알림 서버**, Kafka 컨슈머 서버(메일 처리 담당)

---  

## 🚀 프로젝트 개요
TablePick은 대규모 트래픽을 처리할 수 있는 **레스토랑 예약·결제 플랫폼**입니다.  
예약 슬롯 관리, 결제 처리, 성능 최적화를 중점적으로 개선했습니다.

---

## 🛠 기술 스택

### 🔹 프론트엔드
![React](https://img.shields.io/badge/React-61DAFB?style=flat&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat&logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-06B6D4?style=flat&logo=tailwindcss&logoColor=white)

### 🔹 백엔드
![Java](https://img.shields.io/badge/Java%2021-FF7800?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.4.5-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL%208.0.41-4479A1?style=flat&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white)
![OAuth](https://img.shields.io/badge/OAuth2-000000?style=flat&logo=openid&logoColor=white)
![FCM](https://img.shields.io/badge/Firebase%20Cloud%20Messaging-FFCA28?style=flat&logo=firebase&logoColor=black)
![JWT](https://img.shields.io/badge/JWT-black?style=flat&logo=jsonwebtokens&logoColor=white)

### 🔹 AI & Data
![Python](https://img.shields.io/badge/Python%203.11.8-3776AB?style=flat&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat&logo=fastapi&logoColor=white)
![scikit-learn](https://img.shields.io/badge/scikit--learn-FF9A00?style=flat&logo=scikitlearn&logoColor=white)
![pandas](https://img.shields.io/badge/pandas-150458?style=flat&logo=pandas&logoColor=white)
![numpy](https://img.shields.io/badge/numpy-013243?style=flat&logo=numpy&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-43B02A?style=flat&logo=selenium&logoColor=white)

### 🔹 협업 도구
![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat&logo=github&logoColor=white)
![Jira](https://img.shields.io/badge/Jira-0052CC?style=flat&logo=jira&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=flat&logo=notion&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=flat&logo=discord&logoColor=white)

---

## 📐 아키텍처

<img width="341" height="423" alt="image" src="https://github.com/user-attachments/assets/18f1bfd1-fb14-4006-a9f6-cb4046f28a08" />

---

## 🗺 ERD

<img width="2820" height="1012" alt="image" src="https://github.com/user-attachments/assets/ac73d8b5-0d06-4a28-a72e-376496e1905b" />
> 원본 ERD : [ERDCloud 보기](https://www.erdcloud.com/d/pKQZMxpT7NpDe9wn9)

---

## 🛠 기술 스택
- **Backend**: Java 21, Spring Boot
- **Infra / Data**: Kafka, MySQL, Redis, Docker
- **Testing / Observability**: k6, Grafana, Prometheus

---

### 🚀 CI/CD 배포 효율 개선 (결제 서버 분리 효과)

- **배경**  
  - 기존 단일 서버 구조에서는 예약/결제 코드가 한 레포에 묶여 있어, 결제 로직 수정 시 전체 서버를 다시 빌드/배포해야 했음.
  - 결제 서버를 별도 마이크로서비스로 분리하면서 **독립 배포 가능**.

- **결과 (GitHub Actions 빌드 시간 비교)**
  - 분리 전: 전체 서버 빌드/배포 **2분 41초**  
  - 분리 후: 결제 서버만 배포 → **1분 46초**  

<img width="319" height="153" alt="image" src="https://github.com/user-attachments/assets/86686b35-1069-4b20-85f3-3e96e7f0ad87" />

> 결제 서버 분리를 통해 **배포 속도가 약 35% 개선**되었으며, 운영 시 장애 영향 범위도 축소.

- **안정성**
  - **재시도(Backoff) + DLQ**: 일시 장애 시 자동 재처리, 영구 실패는 DLQ로 격리.
  - **Outbox 패턴(계획)**: DB 트랜잭션과 이벤트 발행을 분리·안정화(이벤트 유실/중복/순서 보장).

- **운영 포인트**
  - **Topic/Partition**: `payments` 토픽, 예약ID 기반 파티셔닝(동일 예약 순서 보장).
  - **Idempotency**: `reservationId` + 상태머신(READY→PAID/FAILED)로 **중복 처리 차단**.
  - **모니터링**: 소비 지연(consumer lag), 재시도/실패율, DLQ 수치 대시보드화.

- **추가/고도화 계획**
  - Outbox를 **트랜잭션 로그 테이블** 기반으로 구현 + **Outbox Relayer** 배치.
  - **Exactly-once 흐름** 강화: 소비측 멱등키 캐시/테이블, Producer idempotence 설정.
  - **보상 트랜잭션 시나리오** 문서화(부분 실패 케이스별 롤백/보상 단계).
  - **부하 프로파일 확대**: VU/러닝타임/에러율 스윗스팟 산출 후 오토스케일 기준 반영.
---

### 🧪 실험 환경 & 테스트 설정

- **클라우드**: AWS (단일 VPC, 동일 AZ)
- **애플리케이션**
  - 예약 서비스: **EC2 t3.medium** (2 vCPU, 4GiB)
  - 결제 서비스: **EC2 t3.medium** (2 vCPU, 4GiB)
- **데이터베이스**
  - **Amazon RDS for MySQL – db.t3.large** (2 vCPU, 4GiB)
- **메시징**
  - **Apache Kafka** 클러스터 (단일 브로커) – EC2 (동일 VPC, 내부 통신)
- **오토스케일링**: 비활성 (고정 인스턴스, 테스트 간 동일 조건 유지)
- **k6 부하 테스트**
  - VU(가상사용자) = **5000**
  - 요청 패턴: 예약→결제 플로우 단일 엔드포인트 중심
  - 각 시나리오 **사전 워밍업 30s** 후 측정(콜드 스타트 영향 최소화)
 
---

## 📝 남은 작업 (TODO)
- **DB 인덱스 최적화**: 예약 슬롯/식당 데이터 조회 성능 개선
- **Outbox 패턴** 안정화: 이벤트 발행의 신뢰성 보장 (트랜잭션 경계 명확화)
- **데이터 정합성 보강**: 예약 취소/결제 취소 시 트랜잭션 처리
- **에러 핸들링 강화**: 외부 결제 API 장애 상황 시 재시도 & 보상 처리

---

## 🎥 시연 영상
https://drive.google.com/file/d/1ZoBAgl4vPlDY5-KWDxwHWnN-WZnCq-e9/view?usp=sharing
