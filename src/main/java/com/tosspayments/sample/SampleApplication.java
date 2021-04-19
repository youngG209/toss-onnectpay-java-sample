package com.tosspayments.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    // http 요청 및 json 처리를 위한 설정 - 테스트 편의를 위해서 restTemplate이 exception 발생하지 않도록 설정함
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate bean = new RestTemplate();
        bean.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
            }
        });

        return bean;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}