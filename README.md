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

## ✅ 개선 사항


### 1) 대량 **데이터 삽입** 성능 개선
- **문제**
  - 예약 슬롯은 한 달 기준 약 **4,987,200건** 생성 필요.
  - 초기 구현(`JPA + IDENTITY`)은 **개별 INSERT** 방식이라 대량 삽입 시 병목 발생.  
    → 실제 실행 결과: **2,010,952ms (약 33분 31초 소요)**

    <img width="429" height="496" alt="image" src="https://github.com/user-attachments/assets/403cd829-45e0-4148-9c8a-59dde95ab22c" />


- **해결**
  - `JdbcTemplate` 기반으로 전환하고 **Batch(3000)** 적용.

- **결과**
  | 방식 | 실행 시간 | 개선율 |
  |------|-----------|--------|
  | JPA + IDENTITY | 2,010,952 ms | - |
  | JdbcTemplate | 73,243 ms | 약 96.36% 개선 |
  | JdbcTemplate + Batch(3000) | 48,092 ms | 약 97.61% 개선 |

  - 추가로 **멀티스레드 병렬 처리 / 체크포인트 기반 롤백** 설계안을 마련해 대규모 트래픽 대비.

- **실행 결과 캡처**
  <img width="224" height="114" alt="image" src="https://github.com/user-attachments/assets/a9d38660-5bf1-462f-b894-d2acf541a149" />
  <img width="236" height="110" alt="image" src="https://github.com/user-attachments/assets/bffc59de-f5f3-4ebc-95a9-4683b004d179" />

- **추가 계획**
  - **Spring Batch / Quartz**로 삽입 스케줄링 최적화.  
  - 초대용량 환경에서는 **Kafka 기반 비동기 처리** 도입 검토.

---

### 2) **예약 동시성** & 트랜잭션 범위 최적화

- **문제**
  - 동시 요청 **150명**, 수용 인원 **100명**, 스레드풀 **32** 환경에서  
    동일 슬롯에 대해 공유락(S) → 배타락(X) 전환 시 충돌로 **데드락** 발생.
  - `saveAndFlush()` 적용으로 데드락은 해소했지만, **예약 수 불일치(정합성)** 문제는 남아있었음.

  **데드락 에러 로그**  
  <img width="459" height="130" alt="image" src="https://github.com/user-attachments/assets/2cfcfc34-5e58-4a1d-b944-9e555b27af17" />

  **데드락 해소 후에도 남은 정합성 이슈(예약 수 불일치)**  
  <img width="271" height="121" alt="image" src="https://github.com/user-attachments/assets/5f7d21e4-d4ae-42e3-b5b7-9ce82cd99aec" />

- - **해결**
  - **락 전략 적용**
    - 인기 슬롯: **비관적 락(Pessimistic Lock)**
    - 비인기 슬롯: **낙관적 락(Optimistic Lock)**

    **성능 비교 결과**
    | 방식       | 실행 시간 |
    |------------|-----------|
    | 낙관적 락  | 3071ms    |
    | 비관적 락  | 909ms     |

    <img width="351" height="154" alt="image" src="https://github.com/user-attachments/assets/71a6e385-5a28-4f0c-a685-f6fae615ff89" />
    <img width="351" height="157" alt="image" src="https://github.com/user-attachments/assets/287b422c-bf53-4df7-a92e-455fec947f31" />

    → 비관적 락이 낙관적 락 대비 **3.38배 성능 개선**


  3. **데드락 제거**  
     - 공유락(S Lock)과 배타락(X Lock) 충돌 원인 분석  
     - 락 획득 순서 및 범위 재설계로 무한 대기 상태 제거  

  4. **트랜잭션 범위 최소화**  
     - 예약 저장은 트랜잭션 내에서 처리  
     - 외부 결제 API 호출은 트랜잭션 밖으로 분리  

     <img width="694" height="303" alt="image" src="https://github.com/user-attachments/assets/ba490e25-72d7-413a-b218-4ae277ccc302" />

- **결과**
  - **락 전략**:  
    - 낙관적 락 실행 시간: **3071ms**  
    - 비관적 락 실행 시간: **909ms** → **3.38배 개선**  
  - **트랜잭션 범위 축소**:  
    - 대기 시간 평균 **40% 개선**, 최대 **50% 개선** (동시요청 20 기준)

- **장애 대비**
  - 결제 API 장애 시 전체 롤백 대신  
    **3분 주기 보정 스케줄러**를 통해 결제 ID 누락을 자동 보정 → **결과적 일관성 확보**

- **추가 계획**
  - **락 경합 모니터링**을 Grafana 대시보드에 시각화하여 SLA 관점에서 추적  
  - **분산 락(Redis 기반)** 검토로 DB 락 부담 완화  
  - **CQRS 패턴**을 적용해 조회 부하와 예약/결제 트랜잭션 분리  

---

### 3) 결제 서비스 분리 & Kafka 기반 비동기화

- **배경**
  - 결제 장애가 예약 흐름 전체로 **전파**되는 문제가 있어, **관심사 분리(결제 서버 분리)** 및 **확장성 확보**가 필요했습니다.

- **설계**
  - 예약 서비스 ↔ 결제 서비스 **분리 배치**.
  - 동기 REST 호출 중심 흐름을 **Saga(보상 트랜잭션)** + **Kafka 이벤트**로 전환해 결제 지연/장애의 영향 범위를 축소.
  - 핵심 이벤트
    - `ReservationCreated` → 결제 요청
    - `PaymentSucceeded/Failed` → 예약 확정/보상(취소) 처리
  - **멱등성 키**(reservationId)로 중복 소비 방지, **파티션 키**로 순서 보장(동일 예약 단위).

## 📌 예약/결제 아키텍처 (Saga 패턴)

본 시스템은 예약과 결제가 분리된 구조로, **Saga 패턴(보상 트랜잭션)**을 적용하여 장애 전파를 최소화했습니다.  

### 1) 정상 플로우
예약 요청 → 예약 서비스(DB 저장) → 결제 이벤트 발행 → 결제 서비스 → PG사 승인/취소 처리

<img width="574" height="283" alt="image" src="https://github.com/user-attachments/assets/435b7f6d-cdfd-4631-add5-6c46cf0ca23b" />

### 2) 실패 시 보상 플로우
결제 실패 이벤트 발생 시 → 예약 서비스에서 결제 취소/롤백 처리 → 데이터 정합성 보장

<img width="576" height="189" alt="image" src="https://github.com/user-attachments/assets/63a53426-0489-41b4-8193-bcec2c2022fc" />
 
### 📊 결과 (k6 부하 테스트, VU=5000)
#### 🔹 분리 전
- 처리량(Request Rate): **126.8 r/s**  
- 평균 지연시간: **5초**  
- GitHub Actions 빌드 시간: **2분 41초**

<img width="471" height="156" alt="image" src="https://github.com/user-attachments/assets/e7f39452-a932-4111-8317-06322bd4af8c" />

---

#### 🔹 분리 후 (동기 호출)
- 처리량(Request Rate): **93.2 r/s**  
- 평균 지연시간: **9초**  
- GitHub Actions 빌드 시간: **1분 46초**

<img width="501" height="155" alt="image" src="https://github.com/user-attachments/assets/ea5bf62d-4d39-4456-998a-07bb561337cf" />

---

#### 🔹 분리 후 (Kafka 비동기)
- 처리량(Request Rate): **285.39 r/s** (약 **2.9배 증가**)  
- 평균 지연시간: **1~2초**  

<img width="545" height="196" alt="image" src="https://github.com/user-attachments/assets/87ece0be-9ef2-49f8-8704-859fae261c54" />

#### 🔹 최종 비교

<img width="947" height="351" alt="image" src="https://github.com/user-attachments/assets/1395ea03-7256-4fcf-a6c8-c6e87aceecde" />


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
