package com.ptithcm.movie.movie.entity;

import com.ptithcm.movie.common.constant.PersonJob;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "people", schema = "public")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column
    private Integer tmdbId;

    @Column
    private String fullName;

    @Column
    private String profilePath;

    @Column
    private String biography;

    @Column
    private LocalDate birthDate;

    @Column
    private String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "job")
    private PersonJob job;

}
