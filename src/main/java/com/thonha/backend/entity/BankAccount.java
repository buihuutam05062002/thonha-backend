package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;

    @ToString.Exclude
    @Column(name = "account_number", nullable = false, length = 30)
    String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 100)
    String accountHolder;
}
