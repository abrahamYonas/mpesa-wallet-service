package com.example.mpesa.Bank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bank {

    @Id
    @Column(name = "payout_id")
    private String payoutId;

    @Column(name = "bank_account", nullable = false)
    private String bankAccount;

    @Column(nullable = false)
    private Integer amount;

    private String result;

    private String reason;

    @Column(name = "bank_ref")
    private String bankRef;

    @Column(nullable = false)
    private String status;
}
