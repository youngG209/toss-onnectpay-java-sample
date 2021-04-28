package com.tosspayments.sample.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosspayments.sample.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class APIIntegrationController {
    private static final String TERMS_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/terms";
    private static final String CARD_REGISTER_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments/methods/card";
    private static final String METHOD_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments/methods";
    private static final String PAYMENT_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments";
    private static final String ACCESS_TOKEN_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/authorizations/access-token";

    private static final String TEST_SECRET_KEY = "test_ak_ZORzdMaqN3wQd5k6ygr5AkYXQGwy";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MerchantDataStore merchantDataStore;

    @Autowired
    public APIIntegrationController(RestTemplate restTemplate, ObjectMapper objectMapper, MerchantDataStore merchantDataStore) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.merchantDataStore = merchantDataStore;
    }

    @GetMapping("/api-terms")
    public String getTerms(Model model, HttpServletRequest servletRequest) {
        UserData userData = merchantDataStore.getUserData(servletRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(TEST_SECRET_KEY, "");

        try {
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    TERMS_ENDPOINT + "?customerKey=" + userData.getCustomerKey(), HttpMethod.GET, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, String>> terms = StreamSupport.stream(Objects.requireNonNull(response.getBody())
                        .spliterator(), false)
                        .map(n -> {
                            Map<String, String> termsData = new HashMap<>();
                            termsData.put("id", n.get("id").asText());
                            termsData.put("title", n.get("title").asText());
                            termsData.put("url", n.get("url").asText());
                            return termsData;
                        }).collect(Collectors.toList());

                model.addAttribute("terms", terms);
            } else {
                model.addAttribute("terms", Collections.emptyList());
            }
        } catch (Exception e) {
            model.addAttribute("terms", Collections.emptyList());
        }

        return "api_terms";
    }

    @PostMapping("/api-terms")
    public String postTerms(TermsData termsData, Model model,
                            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        UserData userData = merchantDataStore.getUserData(servletRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(TEST_SECRET_KEY, "");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("customerKey", userData.getCustomerKey());
        payloadMap.put("termsId", termsData.getTermsId());

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(TERMS_ENDPOINT + "/agree", entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String authorizationCode = Objects.requireNonNull(response.getBody()).get("code").asText();

                payloadMap = new HashMap<>();
                payloadMap.put("code", authorizationCode);
                payloadMap.put("customerKey", userData.getCustomerKey()); // 가맹점 유저 식별자

                // 가맹점이 본인 인증 정보를 보유하고 있을때 선택적으로 연동
                Map<String, String> identityMap = new HashMap<>();
                identityMap.put("ci", userData.getCi()); // CI: 연계정보 (88byte)
                identityMap.put("name", userData.getName()); // 이용자 실명
                identityMap.put("rrn", userData.getRrn()); // 이용자의 주민등록번호 앞 7자리 (생년월일 + 성별식별자)

                payloadMap.put("customerIdentity", identityMap);
                //

                entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

                try {
                    response = restTemplate.postForEntity(ACCESS_TOKEN_ENDPOINT, entity, JsonNode.class);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        merchantDataStore.saveAccessToken(Objects.requireNonNull(response.getBody()).get("accessToken").asText(), servletResponse);

                        model.addAttribute("message", "정상 연결 되었습니다.");

                    } else {
                        model.addAttribute("message", Objects.requireNonNull(response.getBody()).get("message").asText());
                    }
                } catch (RestClientResponseException e) {
                    model.addAttribute("message", e.getMessage());
                }
            } else {
                model.addAttribute("message", Objects.requireNonNull(response.getBody()).get("message").asText());
            }
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
        }

        return "api_terms_result";
    }

    @GetMapping("/api-register")
    public String getRegister(Model model, HttpServletRequest servletRequest) {
        model.addAttribute("userData", merchantDataStore.getUserData(servletRequest));

        return "api_register";
    }

    @PostMapping("/api-register")
    public String postRegister(CardData cardData, Model model, HttpServletRequest servletRequest) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(merchantDataStore.getAccessToken(servletRequest));

        Map<String, Object> payloadMap = new HashMap<>();
        String cardNumber = cardData.getCardNum1() + cardData.getCardNum2() + cardData.getCardNum3() + cardData.getCardNum4();
        payloadMap.put("cardNumber", cardNumber);
        payloadMap.put("cardExpirationMonth", cardData.getExp().substring(0, 2));
        payloadMap.put("cardExpirationYear", cardData.getExp().substring(2, 4));
        payloadMap.put("cardCvc", cardData.getCvc());
        payloadMap.put("cardPassword", cardData.getPassword());

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(CARD_REGISTER_ENDPOINT, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                model.addAttribute("message", "카드 등록에 성공 했습니다.");
                model.addAttribute("response", Objects.requireNonNull(response.getBody()).toPrettyString());
            } else {
                model.addAttribute("message", Objects.requireNonNull(response.getBody()).get("message").asText());
            }
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
        }

        return "api_register_result";
    }

    @GetMapping("/api-pay")
    public String getPay(Model model, HttpServletRequest servletRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(merchantDataStore.getAccessToken(servletRequest));

        try {
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(METHOD_ENDPOINT, HttpMethod.GET, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, String>> cards = StreamSupport.stream(Objects.requireNonNull(response.getBody())
                        .get("cards").spliterator(), false)
                        .filter(n -> n.get("status").asText().equals("ENABLED")).map(n -> {

                            merchantDataStore.saveMethodKey(n.get("id").asText(), n.get("methodKey").asText());

                            Map<String, String> cardData = new HashMap<>();
                            cardData.put("id", n.get("id").asText());
                            cardData.put("issueCompany", n.get("issueCompany").asText());
                            cardData.put("cardNumber", n.get("cardNumber").asText());
                            cardData.put("cardName", n.get("cardName").asText());
                            return cardData;
                        }).collect(Collectors.toList());

                model.addAttribute("cards", cards);

            } else {
                model.addAttribute("cards", Collections.emptyList());
            }
        } catch (Exception e) {
            model.addAttribute("cards", Collections.emptyList());
        }

        return "api_pay";
    }

    @PostMapping("/api-pay")
    public String postPay(PayData payData, Model model, HttpServletRequest servletRequest) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(TEST_SECRET_KEY, "");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("methodKey", merchantDataStore.getMethodKey(payData.getMethodId()));
        payloadMap.put("amount", payData.getAmount());
        payloadMap.put("orderId", payData.getOrderId());
        payloadMap.put("orderName", payData.getOrderName());

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payloadMap), headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(PAYMENT_ENDPOINT, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                model.addAttribute("message", "결제에 성공 했습니다.");
                model.addAttribute("response", Objects.requireNonNull(response.getBody()).toPrettyString());
            } else {
                model.addAttribute("message", Objects.requireNonNull(response.getBody()).get("message").asText());
            }
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
        }

        return "api_pay_result";
    }
}
