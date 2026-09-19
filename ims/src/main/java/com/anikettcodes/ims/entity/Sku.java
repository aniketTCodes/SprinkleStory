package com.anikettcodes.ims.entity;

import com.anikettcodes.ims.util.enums.SkuStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String uniqueName;

    @Column(nullable = false)
    private String displayName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false)
    private BigDecimal mrp;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SkuStatus status;

    @Column(unique = true)
    private String barcode;

    @Version
    private Integer version;
}
