package com.tosspayments.sample.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tosspayments.sample.domain.CardData;
import com.tosspayments.sample.domain.MerchantDataStore;
import com.tosspayments.sample.domain.PayData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class APIIntegrationController {
    private static final String CARD_REGISTER_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments/methods/card";
    private static final String METHOD_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments/methods";
    private static final String PAYMENT_ENDPOINT = "https://api.tosspayments.com/v1/connectpay/payments";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MerchantDataStore merchantDataStore;

    @Autowired
    public APIIntegrationController(RestTemplate restTemplate, ObjectMapper objectMapper, MerchantDataStore merchantDataStore) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.merchantDataStore = merchantDataStore;
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
        headers.setBearerAuth(merchantDataStore.getAccessToken(servletRequest));

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
