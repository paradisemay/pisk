package ru.ifmo.se.pisk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.ifmo.se.pisk.security.Sha384PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/storage/**"
                        ).permitAll()


                        .requestMatchers("/error", "/default-ui.css").permitAll()


                        .requestMatchers(
                                "/login", "/register", "/forgot-password", "/reset-password",
                                "/swagger-ui/**", "/v3/api-docs/**", "/docs/**", "/actuator/health"
                        ).permitAll()


                        .requestMatchers(HttpMethod.GET, "/", "/catalog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tracks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/authors/**").permitAll()


                        .requestMatchers("/upload", "/newupload").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")


                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tracks/*/like").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tracks/*/like").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tracks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tracks/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/tracks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/complaints").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/complaints/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/subscriptions/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .defaultSuccessUrl("/catalog", true)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .exceptionHandling(exceptions -> exceptions

                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )

                        .accessDeniedHandler(new AccessDeniedHandlerImpl())
                )
                .requestCache(cache -> cache.requestCache(new NullRequestCache()));


        http.addFilterBefore((request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String path = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            
            log.debug("Security Filter: {} {} - checking authorization", method, path);
            
            chain.doFilter(request, response);
            
            log.debug("Security Filter: {} {} - response status: {}", method, path, httpResponse.getStatus());
        }, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Sha384PasswordEncoder passwordEncoder(@Value("${security.password.pepper}") String pepper) {
        return new Sha384PasswordEncoder(pepper);
    }
}
