# TablePick-Refactor

> 이 레포는 **팀 프로젝트**로 시작한 식당 예약·결제 플랫폼 중  
> 제가 맡았던 **예약·결제 도메인**을 기반으로 **개인적으로 리팩토링**한 버전입니다.  
> (원본 팀 프로젝트: [링크](https://github.com/orgs/4und-Cloud/repositories))

---

## 🚀 프로젝트 개요
TablePick은 대규모 트래픽을 처리할 수 있는 **레스토랑 예약·결제 플랫폼**입니다.  
예약 슬롯 관리, 결제 처리, 성능 최적화를 중점적으로 개선했습니다.

---

## 🛠 기술 스택
- **Backend**: Java 21, Spring Boot
- **Infra / Data**: Kafka, MySQL, Redis, Docker
- **Testing / Observability**: k6, Grafana, Prometheus

---

## 📈 주요 성과
- **TPS 2.9배 향상** (Kafka 기반 비동기 구조 전환)  
- 예약 슬롯 삽입 시간 **33분 → 16초** (JDBC Batch + Lock 전략)  
- 결제 서버 분리 → 외부 API 호출 안정성 강화  
- k6 기반 부하 테스트 시나리오 작성 및 모니터링 대시보드 구축  

---

## 🔧 리팩토링 포인트


