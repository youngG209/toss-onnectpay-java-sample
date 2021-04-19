package com.tosspayments.sample.controller;

import com.tosspayments.sample.domain.MerchantDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class UIIntegrationController {
    private final MerchantDataStore merchantDataStore;

    @Autowired
    public UIIntegrationController(MerchantDataStore merchantDataStore) {
        this.merchantDataStore = merchantDataStore;
    }

    @GetMapping("/ui")
    public String testUi(Model model, HttpServletRequest servletRequest) {
        model.addAttribute("userData", merchantDataStore.getUserData(servletRequest));

        return "ui_test";
    }
}
