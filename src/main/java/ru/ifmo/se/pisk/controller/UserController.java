package ru.ifmo.se.pisk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.RegisterUserRequest;
import ru.ifmo.se.pisk.controller.dto.UserResponse;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.UserService;

@RestController
@Tag(name = "Users", description = "Регистрация и просмотр профилей")
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Зарегистрировать пользователя", description = "Создаёт профиль пользователя")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        var profile = userService.registerUser(request.username(), request.email(), request.password(), request.role());
        return ResponseEntity.ok(ApiMapper.toUserResponse(profile));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Профиль пользователя", description = "Возвращает профиль по идентификатору")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return userService.findProfile(id)
                .map(ApiMapper::toUserResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
