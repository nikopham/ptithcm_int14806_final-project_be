package com.ptithcm.movie.movie.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "profile_path")
    private String profilePath;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> job;
}