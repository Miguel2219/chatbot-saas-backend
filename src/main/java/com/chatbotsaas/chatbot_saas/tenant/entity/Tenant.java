package com.chatbotsaas.chatbot_saas.tenant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity //Tells that this class represents a database table
@Table(name = "tenants")
@Data //automatically generates getters and setters
@NoArgsConstructor //generates an empty constructor. JPA requires this
@AllArgsConstructor //generates a constructor with all fields
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

    @PrePersist //It sets createdAt automatically every time you save a new Tenant, so you never have to set it manually
    public void prePersist() {
        this.createdAt = LocalDateTime.now(); //Sets the date automatically on creation
    }
}
