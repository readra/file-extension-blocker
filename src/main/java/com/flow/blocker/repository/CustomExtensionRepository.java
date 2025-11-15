package com.flow.blocker.repository;

import com.flow.blocker.domain.CustomExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomExtensionRepository extends JpaRepository<CustomExtension, Long> {
    Optional<CustomExtension> findByExtension(String extension);
    boolean existsByExtension(String extension);
    long countBy();
}
