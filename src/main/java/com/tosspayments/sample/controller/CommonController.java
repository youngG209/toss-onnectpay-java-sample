package com.tosspayments.sample.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosspayments.sample.domain.MerchantDataStore;
import com.tosspayments.sample.domain.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Controller
public class CommonController {
    private static final String ACCESS_TOKEN_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/authorizations/access-token";
    private static final String PAYMENT_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments/";

    private static final String TEST_SECRET_KEY = "test_ak_ZORzdMaqN3wQd5k6ygr5AkYXQGwy";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MerchantDataStore merchantDataStore;

    @Autowired
    public CommonController(RestTemplate restTemplate, ObjectMapper objectMapper, MerchantDataStore merchantDataStore) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.merchantDataStore = merchantDataStore;
    }

    // Redirect URL에 대한 가맹점 서버 구현 부분
    @GetMapping("/callback_auth")
    public String authCallback(Model model, @RequestParam String code, @RequestParam String customerKey,
                               HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(TEST_SECRET_KEY, "");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("code", code);

        UserData userData = merchantDataStore.getUserData(servletRequest);
        if (!userData.getCustomerKey().equals(customerKey)) {
            throw new RuntimeException("invalid customer session");
        }

        payloadMap.put("customerKey", customerKey); // 가맹점 유저 식별자

        // 가맹점이 본인 인증 정보를 보유하고 있을때 선택적으로 연동
        Map<String, String> identityMap = new HashMap<>();
        identityMap.put("ci", userData.getCi()); // CI: 연계정보 (88byte)
        identityMap.put("name", userData.getName()); // 이용자 실명
        identityMap.put("rrn", userData.getRrn()); // 이용자의 주민등록번호 앞 7자리 (생년월일 + 성별식별자)

        payloadMap.put("customerIdentity", identityMap);
        //

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(ACCESS_TOKEN_ENDPOINT, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                merchantDataStore.saveAccessToken(Objects.requireNonNull(response.getBody()).get("accessToken").asText(), servletResponse);

                model.addAttribute("message", "인증 되었습니다.");

            } else {
                model.addAttribute("message", Objects.requireNonNull(response.getBody()).get("message").asText());
            }
        } catch (RestClientResponseException e) {
            model.addAttribute("message", e.getMessage());
        }

        return "auth_callback";
    }

    @GetMapping("/payment_success")
    public String confirm(Model model, @RequestParam String paymentKey, @RequestParam String orderId, @RequestParam Long amount,
                          HttpServletRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(TEST_SECRET_KEY, "");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("orderId", orderId);
        payloadMap.put("amount", amount);
        payloadMap.put("customerKey", merchantDataStore.getUserData(request).getCustomerKey());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(PAYMENT_ENDPOINT + "/" + paymentKey, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                model.addAttribute("orderId", response.getBody().get("orderId").asText());

            } else {
                model.addAttribute("message", response.getBody().get("message").asText());
                model.addAttribute("code", response.getBody().get("code").asText());
            }
        } catch (RestClientResponseException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("code", "");
        }

        return "success";
    }

    @GetMapping("/payment_fail")
    public String fail(Model model, @RequestParam String message, @RequestParam String code) {
        model.addAttribute("message", message);
        model.addAttribute("code", code);

        return "fail";
    }
}
