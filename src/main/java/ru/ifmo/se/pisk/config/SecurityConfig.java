package ru.ifmo.se.pisk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize
                        // Public static
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // Public login and docs
                        .requestMatchers(
                                "/login", "/swagger-ui/**", "/v3/api-docs/**", "/docs/**", "/actuator/health"
                        ).permitAll()

                        // Public HTML pages (permit for GET and also HEAD automatically)
                        .requestMatchers(
                                "/", "/catalog",
                                "/tracks/**",
                                "/authors/**"
                        ).permitAll()

                        // Protected HTML pages
                        .requestMatchers("/upload").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // API rules keep as is
                        .requestMatchers("/api/v1/users").permitAll()
                        .requestMatchers("/api/v1/tracks/**").permitAll()
                        .requestMatchers("/api/v1/complaints/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/subscriptions/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .exceptionHandling(exceptions -> exceptions
                        // 401 only for API
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                        .accessDeniedHandler(new AccessDeniedHandlerImpl())
                )
                .requestCache(cache -> cache.requestCache(new NullRequestCache()));

        return http.build();
    }

    @Bean
    public Sha384PasswordEncoder passwordEncoder(@Value("${security.password.pepper}") String pepper) {
        return new Sha384PasswordEncoder(pepper);
    }
}
