package ru.ifmo.se.pisk.web.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class LanguageLinks {

    private LanguageLinks() {
    }

    public static Map<String, String> build(HttpServletRequest request) {
        String base = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQueryParam("lang")
                .toUriString();
        Map<String, String> links = new HashMap<>();
        links.put("ru", ServletUriComponentsBuilder.fromUriString(base)
                .replaceQueryParam("lang", "ru")
                .toUriString());
        links.put("en", ServletUriComponentsBuilder.fromUriString(base)
                .replaceQueryParam("lang", "en")
                .toUriString());
        return links;
    }
}
