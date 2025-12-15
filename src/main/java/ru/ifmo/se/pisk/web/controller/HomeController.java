package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null && !query.isBlank()
                ? "redirect:/catalog?" + query
                : "redirect:/catalog";
    }
}
