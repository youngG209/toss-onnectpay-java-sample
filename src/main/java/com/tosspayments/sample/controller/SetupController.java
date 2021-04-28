package com.tosspayments.sample.controller;

import com.tosspayments.sample.domain.MerchantDataStore;
import com.tosspayments.sample.domain.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SetupController {
    private final MerchantDataStore merchantDataStore;

    @Autowired
    public SetupController(MerchantDataStore merchantDataStore) {
        this.merchantDataStore = merchantDataStore;
    }

    @GetMapping("/setup")
    public String getSetup(Model model, HttpServletRequest servletRequest) {
        UserData userData = merchantDataStore.getUserData(servletRequest);
        model.addAttribute("userData", userData);

        return "setup";
    }

    @PostMapping("/setup")
    public RedirectView postSetup(UserData userData, HttpServletResponse servletResponse) {
        merchantDataStore.setUserData(userData, servletResponse);

        return new RedirectView("/");
    }
}
