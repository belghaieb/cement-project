package com.cement.prefacture.repository;

import com.cement.prefacture.entity.BonCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BonCommandeRepository extends JpaRepository<BonCommande, Long> {
    Optional<BonCommande> findByNumeroCommande(String numeroCommande);
}