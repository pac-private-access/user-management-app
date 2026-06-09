package com.pac.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "divisions")
public class Division {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;
}
