package com.lumina.clinic.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/booking/{id}", "/staff"})
    String angularRoute() {
        return "forward:/index.html";
    }
}
