package com.nestora.nestora_app.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_amenities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 100)
    private String amenity;
}