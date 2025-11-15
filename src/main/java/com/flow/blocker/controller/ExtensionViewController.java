package com.flow.blocker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExtensionViewController {

    /**
     * 메인 화면
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
