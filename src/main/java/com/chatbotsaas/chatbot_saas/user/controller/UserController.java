package com.chatbotsaas.chatbot_saas.user.controller;

import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.chatbotsaas.chatbot_saas.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order) {

        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(userService.getUsers(pageable), HttpStatus.OK);
    }
}
