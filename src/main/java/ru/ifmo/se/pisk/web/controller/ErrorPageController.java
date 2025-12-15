package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping("/error")
public class ErrorPageController implements ErrorController {

    @RequestMapping
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        int statusCode = resolveStatusCode(request);
        HttpStatus status = HttpStatus.resolve(statusCode);

        model.addAttribute("status", statusCode);
        model.addAttribute("error", status != null ? status.getReasonPhrase() : "Error");
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        model.addAttribute("langLinks", LanguageLinks.build(request));
        response.setStatus(statusCode);

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "error/403";
        }
        return "error/500";
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return code;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
