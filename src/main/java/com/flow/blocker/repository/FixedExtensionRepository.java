package com.flow.blocker.repository;

import com.flow.blocker.domain.FixedExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedExtensionRepository extends JpaRepository<FixedExtension, Long> {
    Optional<FixedExtension> findByExtension(String extension);
    boolean existsByExtension(String extension);
    List<FixedExtension> findByCheckedTrue();
    long countByCheckedTrue();
    
    @Query("SELECT f FROM FixedExtension f ORDER BY f.extension ASC")
    List<FixedExtension> findAllOrderByExtension();
}
