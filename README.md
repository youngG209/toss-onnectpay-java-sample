# connectpay-java-sample

TossPayments 커넥트페이 연동 개발을 위한 Java 샘플 코드입니다. Spring Boot로 구성되었습니다.

## 테스트하기

```sh
$ git clone https://github.com/tosspayments/connectpay-java-sample.git
$ cd conectpay-java-sample
$ ./gradlew bootRun
```

서버가 실행 된 후, `localhost:8080` 경로에 접속해서 테스트할 수 있습니다.

Local 테스트용 기본 인증 Redirect URL은 `http://localhost:8080/callback_auth` 입니다.
