package com.chatbotsaas.chatbot_saas.auth.controller;

import com.chatbotsaas.chatbot_saas.auth.dto.*;
import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        authService.register(registerRequest);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    /**
     * Renueva la sesión usando el refresh token. Rota el RT (el viejo queda
     * inutilizable) y devuelve un par nuevo de access + refresh. Si el RT
     * es inválido/expirado/reusado, responde 401 con code
     * {@code INVALID_REFRESH_TOKEN}.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }

    /**
     * Invalida el refresh token en el backend. El cliente además debe
     * limpiar el storage local. Idempotente — tokens ya revocados o
     * inexistentes devuelven 204 igual.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    // TODO(MVP): "Cerrar sesión en todos los dispositivos". Dejado comentado
    // para retomar post-MVP. La lógica ya está implementada abajo (service +
    // repo + SecurityConfig); reactivar = descomentar acá, descomentar
    // AuthService.logoutAll() + RefreshTokenService.revokeAllForUserId() +
    // el matcher específico en SecurityConfig + el endpoint + método en el
    // AuthService del frontend + el botón en el sidebar.
    //
    // /**
    //  * Cierra sesión del usuario autenticado en TODOS sus dispositivos.
    //  * Revoca masivamente los refresh tokens en DB — el próximo intento de
    //  * /refresh en cualquier dispositivo devuelve 401 y empuja al user a
    //  * re-loguearse. Requiere un Bearer válido (el filter JWT debe haber
    //  * poblado el SecurityContext); sin auth responde 401.
    //  */
    // @PostMapping("/logout-all")
    // public ResponseEntity<Void> logoutAll() {
    //     authService.logoutAll();
    //     return ResponseEntity.noContent().build();
    // }

    @PutMapping("/change_password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        authService.changePassword(changePasswordRequest);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Inicia el flujo de recuperación de contraseña. Responde <b>siempre</b>
     * el mismo mensaje genérico, exista o no el email — anti-enumeración.
     * El service hace el rate-limit y manda el email en background ({@code @Async}).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<GenericMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(new GenericMessageResponse(
                "Si el correo está registrado, recibirás un enlace de recuperación en los próximos minutos."
        ));
    }

    /**
     * Valida el token del link antes de mostrar el formulario de nueva
     * password. Devuelve el email enmascarado para feedback visual.
     * Responde 400 {@code INVALID_RESET_TOKEN} si no sirve.
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<ValidateResetTokenResponse> validateResetToken(
            @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(passwordResetService.validate(token));
    }

    /**
     * Confirma el reset: cambia la password, consume el token y revoca
     * todos los refresh tokens del user (cierre global de sesiones).
     * Errores: 400 {@code INVALID_RESET_TOKEN} o 400 {@code WEAK_PASSWORD}.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<GenericMessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new GenericMessageResponse(
                "Tu contraseña fue actualizada. Te enviamos un email de confirmación."
        ));
    }
}
