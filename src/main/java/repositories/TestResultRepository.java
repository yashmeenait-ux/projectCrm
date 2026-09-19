package repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entities.TestResult;

@Repository
public interface TestResultRepository
        extends JpaRepository<TestResult, Long> {

    List<TestResult> findByUserId(Long userId);

}