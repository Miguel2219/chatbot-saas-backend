package com.chatbotsaas.chatbot_saas.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
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

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "lastname", nullable = false, length = 100)
    private String lastname;

    @Column(name = "phone", length = 13, nullable = false)
    private String phone;

    @Column(name = "number_document", unique = true, length = 15, nullable = false)
    private String numberDocument;

    @Column(name = "available", nullable = false)
    private Boolean available;

    public Person(String name, String lastname, String phone, String numberDocument, Boolean available) {
        this.name = name;
        this.lastname = lastname;
        this.phone = phone;
        this.numberDocument = numberDocument;
        this.available = available;
    }


    public static Person create(String name, String lastname, String phone, String numberDocument) {
        return new Person(name, lastname, phone, numberDocument, Boolean.TRUE);
    }
}
