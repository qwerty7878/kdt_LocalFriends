package com.backend.kdt.auth.entity;

import com.amazonaws.util.Platform;
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

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @Column(name = "email", length = 100, nullable = true, unique = true)
    private String email;

    @Column(name = "name", length = 20, nullable = false, unique = true)
    private String name;

    @Column(name = "profile", nullable = true, columnDefinition = "TEXT")
    private String profile;

    @Column(name = "point")
    private Long point;

    @Column(name = "watched", nullable = false)
    @Builder.Default
    private Boolean watched = false;
}