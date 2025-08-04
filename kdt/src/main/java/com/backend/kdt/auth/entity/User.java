package com.backend.kdt.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20, nullable = false)
    private Platform platform;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "name", length = 20, nullable = false, unique = true)
    private String name;

    @Column(name = "birth_Year", length = 4, nullable = false)
    private String birthYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20, nullable = false)
    private Gender gender;

    @Column(name = "profile", nullable = true, columnDefinition = "TEXT")
    private String profile;
}