# ZEEBRA

## 📚 프로젝트 소개 <br>

KREAM 같은 명품 **리셀 플랫폼**과 당근 같은 중고거래 **중개 플랫폼**을 분석하여, <br> 대규모 트래픽에서도 **안정정인 성능**을 확보하는 것을 목표로 한 패션 이커머스 플랫폼입니다. <br> 3단계의 고도화 과정을 거쳐서 기능을 개발했습니다.
1. MVP 제작
   - 기획부터 mvp까지 핵심 요구사항을 위주로 빠르게 구현하였습니다.
2. 단위/통합 테스트 및 1차 성능 개선
   - 검색 기능의 쿼리 최적화 등 초기 성능 개선을 진행하였습니다.
   - 핵심 기능에 대한 단위/통합 테스트를 진행했습니다.
3. 부하테스트 및 2차 성능 개선
   - 상품 검색, 채팅, 주문/결제 로직에 대한 부하 테스트를 진행하였습니다.
   - 다양한 동시성 이슈와 병목 현상을 해결하였습니다.
   - 병목 현상의 원인을 분석하고 단계적으로 최적화하여 성능을 개선하였습니다.
<br>


## 기술 스택
**협업** | JIRA, GitHub Issue, Notion
<img width="2400" height="960" alt="2" src="https://github.com/user-attachments/assets/428b9535-676b-4283-aa66-e9ccc6dab712" />
<img width="2400" height="960" alt="3" src="https://github.com/user-attachments/assets/a4147b4f-a937-4fcf-a6a6-a07f9c5a3cc4" />
<br>
<br>

## ERD
<img width="3450" height="1642" alt="KakaoTalk_20251215_170838674" src="https://github.com/user-attachments/assets/1c2f0e46-e8d1-433a-89aa-cfe601cc5365" />
<br>
<br>

## 아키텍처
<img width="525" height="670" alt="image" src="https://github.com/user-attachments/assets/9c133ba0-ec50-45ad-93ff-38b6c835da8a" />
<br>
<br>

## 주요 기능
1. **사용자 인증 (Auth)**: JWT 기반 인증 & 인가 
2. 상품 검색 및 탐색 (Search & Browse)
3. 관심 상품 (Wishlist)
4. 실시간 단체 채팅 (Real-time Chat)
5. 알림 (Notification)
6. **주문 및 결제 (Order & Payment)**: 3가지 주문 방식(장바구니, 즉시 구매, 채팅 거래), Toss Payments 연동

<br>
<br>

## 단위 테스트 및 통합 테스트
- 고도화할 기능 기준으로 테스트 코드를 작성 커버리지를 목표 이상으로 유지
- GitHub Actions를 활용한 자동 테스트 및 빌드 파이프라인 구축
<br>
<br>

## 기술적 도전 과제 및 개선 사항
### 1. 주문 및 결제 처리 속도 개선

<br>
<br>

## 시연 영상
https://www.youtube.com/watch?v=LEjwIThP93Q
<br>
<br>

## 팀원
**FE** 이한음, 홍성경
<br>
**BE** 박가영, 이정민, 윤태우
