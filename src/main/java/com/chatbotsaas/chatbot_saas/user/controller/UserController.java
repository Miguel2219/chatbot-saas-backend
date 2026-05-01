package com.chatbotsaas.chatbot_saas.user.controller;

import com.chatbotsaas.chatbot_saas.user.dto.request.CreateUserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.request.UpdateUserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.chatbotsaas.chatbot_saas.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('users', 'view')")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(name = "tenantId", required = false) UUID tenantId) {

        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(userService.getUsers(tenantId, pageable), HttpStatus.OK);
    }

    @GetMapping("/without_tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersWithoutTenant() {
        return new ResponseEntity<>(userService.getUsersWithoutTenant(), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('users', 'create')")
    public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequestDto dto) {
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{user_id}")
    @PreAuthorize("@permissionChecker.hasPermission('users', 'edit')")
    public ResponseEntity<Void> updateUser(
            @PathVariable("user_id") UUID userId,
            @Valid @RequestBody UpdateUserRequestDto dto) {
        userService.updateUser(userId, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@permissionChecker.hasPermission('users', 'delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
