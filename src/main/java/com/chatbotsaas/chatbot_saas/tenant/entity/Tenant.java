package com.chatbotsaas.chatbot_saas.tenant.entity;

import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity //Tells that this class represents a database table
@Table(name = "tenants")
@Getter
@NoArgsConstructor
public class Tenant {

    @Id // marks which field is the primary key
    @Column(name = "tenant_id", updatable = false, nullable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name= "name", nullable = false) // extra constraints on a specific column. Optional but useful
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "implementation_type")
    @Enumerated(EnumType.STRING)
    private ImplementationType implementationType;

    @PrePersist //It sets createdAt automatically every time you save a new Tenant, so you never have to set it manually
    public void prePersist() {
        this.createdAt = LocalDateTime.now(); //Sets the date automatically on creation
    }

    public Tenant(String name, String email, Boolean isActive,  ImplementationType implementationType) {
        this.name = name;
        this.email = email;
        this.isActive = isActive;
        this.implementationType = implementationType;
    }

    public static Tenant create(String name, String email, ImplementationType implementationType) {
        return new Tenant(name, email, Boolean.TRUE, implementationType);
    }
}
