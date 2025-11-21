package com.ptithcm.movie.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 2)
    private String isoCode;

    @Column(nullable = false, length = 128)
    private String name;
}