package com.example.mpesa.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";

    @Id
    @Column(name = "wallet_id")
    private String walletId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    private Long reserved;

    @Version
    private Long version;

    public Wallet(String walletId, String status, Long balance, Long reserved) {
        this.walletId = walletId;
        this.status = status;
        this.balance = balance;
        this.reserved = reserved;
    }

    public long available() {
        return balance - reserved;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}
