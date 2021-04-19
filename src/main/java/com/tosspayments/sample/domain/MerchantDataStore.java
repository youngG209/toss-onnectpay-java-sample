package com.tosspayments.sample.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;


// 가맹점 서버에서 DB에 저장하는 로직을 테스트 용도로 시뮬레이션 하는 클래스 입니다.
// 사용자 정보나 Access Token 이 해당 형태로 쿠키에 노출되는 방식 및
// 결제수단 정보들이 Map 에 저장되는 내용은 실제로는 사용되지 않아야 합니다.

@Component
public class MerchantDataStore {

    private final ObjectMapper objectMapper;

    private static final String SESSION_COOKIE_NAME = "merchantDataSession";
    private static final String TOKEN_COOKIE_NAME = "merchantDataToken";

    private static final Map<String, String> methodKeyStore = new HashMap<>();

    @Autowired
    public MerchantDataStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setUserData(UserData userData, HttpServletResponse servletResponse) {
        try {
            Cookie cookie = new Cookie(SESSION_COOKIE_NAME,
                    Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(userData).getBytes(StandardCharsets.UTF_8)));
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            servletResponse.addCookie(cookie);
        } catch (JsonProcessingException ignore) {
        }
    }

    public UserData getUserData(HttpServletRequest servletRequest) {
        if (servletRequest.getCookies() == null) {
            return null;
        }

        Optional<Cookie> optional = Arrays.stream(servletRequest.getCookies())
                .filter(c -> c.getName().equals(SESSION_COOKIE_NAME)).findFirst();

        UserData userData = null;

        if(optional.isPresent()) {
            try {
                userData = objectMapper.readValue(
                        new String(Base64.getDecoder().decode(optional.get().getValue())), UserData.class);
            } catch (JsonProcessingException ignore) {
            }
        }

        return userData;
    }

    public void saveAccessToken(String accessToken, HttpServletResponse servletResponse) {
        Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, accessToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        servletResponse.addCookie(cookie);
    }

    public String getAccessToken(HttpServletRequest servletRequest) {
        if (servletRequest.getCookies() == null) {
            return null;
        }

        Optional<Cookie> optional = Arrays.stream(servletRequest.getCookies())
                .filter(c -> c.getName().equals(TOKEN_COOKIE_NAME)).findFirst();

        return optional.map(Cookie::getValue).orElse(null);
    }


    public void saveMethodKey(String methodId, String methodKey) {
        methodKeyStore.put(methodId, methodKey);
    }

    public String getMethodKey(String methodId) {
        return methodKeyStore.get(methodId);
    }
}
