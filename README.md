# connectpay-java-sample

TossPayments 커넥트페이 연동 개발을 위한 Java 샘플 코드입니다. Spring Boot로 구성되었습니다.

## 테스트하기

```sh
$ git clone https://github.com/tosspayments/connectpay-java-sample.git
$ cd conectpay-java-sample
$ ./gradlew bootRun
```

서버가 실행 된 후, `localhost:8080` 경로에 접속해서 테스트할 수 있습니다.

- http://localhost:8080/setup: 사용자 데이터 설정 페이지 입니다. 아래 테스트 페이지 실행을 위해서 반드시 데이터 설정이 필요합니다.
- http://localhost:8080/callback_auth: 테스트 인증 정보 연결 처리용 Redirect URL 입니다.
- http://localhost:8080/ui: JS SDK를 사용한 UI 연동 테스트 페이지 입니다.
- http://localhost:8080/api-register: API형 연동을 할때 카드 등록 테스트 페이지 입니다.
- http://localhost:8080/api-pay: API형 연동을 할때 결제 실행 테스트 페이지 입니다.
