package ru.ifmo.se.pisk.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Window> counters = new ConcurrentHashMap<>();
    private final AppProperties properties;

    public RateLimitingFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();
        long windowSeconds = properties.getRateLimit().getWindowSeconds();
        Window window = counters.compute(key, (k, v) -> {
            if (v == null || now - v.start >= windowSeconds) {
                return new Window(now, 1);
            }
            v.count++;
            return v;
        });
        if (window.count > properties.getRateLimit().getMaxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static class Window {
        long start;
        long count;

        Window(long start, long count) {
            this.start = start;
            this.count = count;
        }
    }
}
