package com.chatbotsaas.chatbot_saas.module.entity;

import com.chatbotsaas.chatbot_saas.permission.entity.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modules")
@Getter
public class Module {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "module_id", updatable = false, nullable = false)
    private UUID moduleId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "route", nullable = false)
    private String route;

    @Column(name = "icon", nullable = false)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "module", fetch = FetchType.EAGER)
    private List<Permission> permissions = new ArrayList<>();

}