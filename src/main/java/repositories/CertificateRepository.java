package repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entities.Certificate;

@Repository
public interface CertificateRepository
        extends JpaRepository<Certificate, Long> {

    List<Certificate> findByUserId(Long userId);
    
    boolean existsByUserIdAndCourseName(
            Long userId,
            String courseName);

}