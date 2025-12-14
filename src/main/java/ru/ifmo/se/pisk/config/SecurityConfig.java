package ru.ifmo.se.pisk.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import ru.ifmo.se.pisk.security.Sha384PasswordEncoder;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize
                        // Static resources - public
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        // Public HTML pages
                        .requestMatchers(HttpMethod.GET, "/", "/catalog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tracks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/authors/**").permitAll()
                        // Protected HTML pages
                        .requestMatchers("/upload", "/admin/**").authenticated()
                        // API endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers("/login", "/swagger-ui/**", "/v3/api-docs/**", "/docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tracks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tracks/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/tracks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/complaints/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/subscriptions/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HtmlRedirectAuthenticationEntryPoint())
                        .accessDeniedHandler(new AccessDeniedHandlerImpl())
                )
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                }));
        return http.build();
    }

    /**
     * Authentication entry point that redirects HTML requests to /login
     * and returns 401 for API requests.
     */
    private static class HtmlRedirectAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
        private final HttpStatusEntryPoint apiEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        public HtmlRedirectAuthenticationEntryPoint() {
            super("/login");
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                            AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
            String path = request.getRequestURI();
            // If it's an API request, return 401
            if (path.startsWith("/api/")) {
                apiEntryPoint.commence(request, response, authException);
            } else {
                // For HTML pages, redirect to login
                super.commence(request, response, authException);
            }
        }
    }

    @Bean
    public Sha384PasswordEncoder passwordEncoder(@Value("${security.password.pepper}") String pepper) {
        return new Sha384PasswordEncoder(pepper);
    }
}
