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

<img width="700" height="1000" alt="image" src="https://github.com/user-attachments/assets/df0ed57c-c6a1-421d-80f1-0f59f91cf2db" />

---

## 🗺 ERD

<img width="800" height="590" alt="image" src="https://github.com/user-attachments/assets/fd4d04e3-3d7b-4a36-8e55-c6fff299c0d6" />

---

## 🛠 기술 스택
- **Backend**: Java 21, Spring Boot
- **Infra / Data**: Kafka, MySQL, Redis, Docker
- **Testing / Observability**: k6, Grafana, Prometheus

---

## 🎥 시연 영상
https://drive.google.com/file/d/1ZoBAgl4vPlDY5-KWDxwHWnN-WZnCq-e9/view?usp=sharing
