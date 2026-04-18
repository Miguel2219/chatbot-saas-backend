package com.chatbotsaas.chatbot_saas.permission.entity;

import com.chatbotsaas.chatbot_saas.module.entity.Module;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
public class Permission {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "permission_id", updatable = false, nullable = false)
    private UUID permissionId;

    @ManyToOne
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(name = "action", nullable = false)
    private String action;

    @ManyToMany(mappedBy = "permissions")
    private List<Role> roles = new ArrayList<>();
}