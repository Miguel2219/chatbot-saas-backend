package com.chatbotsaas.chatbot_saas.user.entity;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    @OneToOne(mappedBy = "user")
    private Person person;

    @ManyToMany(mappedBy = "leadAssignees")
    private List<Bot> bots = new ArrayList<>();

    @Column(name = "notification_channel", nullable = true)
    @Enumerated(EnumType.STRING)
    private NotificationChannel notificationChannel;

    @ManyToOne //The first parameter (Many in this case) corresponds to current class
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String email, String password, Boolean mustChangePassword, List<Role> roles, Tenant tenant, NotificationChannel notificationChannel) {
        this.email = email;
        this.password = password;
        this.mustChangePassword = mustChangePassword;
        this.roles = roles;
        this.tenant = tenant;
        this.notificationChannel = notificationChannel;
    }

    public static User create(String email, String password, Boolean mustChangePassword, List<Role> roles, Tenant tenant,  NotificationChannel notificationChannel) {
        return new User(email, password, mustChangePassword, roles, tenant, notificationChannel);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setNotificationChannel(NotificationChannel notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    public boolean isAdmin() {
        return this.roles.stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));
    }

    public boolean isTenantOwner() {
        return this.roles.stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE));
    }

}
