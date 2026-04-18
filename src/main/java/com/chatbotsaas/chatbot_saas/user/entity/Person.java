package com.chatbotsaas.chatbot_saas.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "person")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class  Person {

    @Id
    private UUID personId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = true, length = 100)
    private String name;

    @Column(name = "lastname", nullable = true, length = 100)
    private String lastname;

    @Column(name = "phone", length = 13, nullable = true)
    private String phone;

    @Column(name = "number_document", unique = true, length = 15, nullable = true)
    private String numberDocument;

    @Column(name = "available", nullable = false)
    private Boolean available;
}
