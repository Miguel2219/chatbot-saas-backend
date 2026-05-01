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

    // Setters para los campos editables desde `UserService.updateUser`. No se
    // usa @Setter a nivel de clase para no exponer mutabilidad innecesaria
    // (personId, user, available no deberían cambiar post-creación).
    public void setName(String name) { this.name = name; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNumberDocument(String numberDocument) { this.numberDocument = numberDocument; }
}
