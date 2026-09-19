package com.anikettcodes.ims.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class ReceiptSeq {

    @Id
    private LocalDate day;

    @Column(nullable = false)
    private Integer lastSeq;
}
