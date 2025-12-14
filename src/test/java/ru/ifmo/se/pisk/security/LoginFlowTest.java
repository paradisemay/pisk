package ru.ifmo.se.pisk.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.ifmo.se.pisk.config.SecurityConfig;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.controller.AuthController;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.password.pepper=test-pepper")
class LoginFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        when(authenticationProvider.supports(Mockito.any())).thenReturn(true);
        when(authenticationProvider.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ));
    }

    @Test
    void loginPageContainsCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("data-password-toggle")));
    }

    @Test
    void loginWithCsrfRedirectsToCatalog() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "user")
                        .param("password", "secret")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/catalog"));

        verify(authenticationProvider).authenticate(any());
    }
}
