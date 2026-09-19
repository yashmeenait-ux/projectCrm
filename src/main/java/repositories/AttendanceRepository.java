package repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import entities.Attendance;

@Repository
public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    List<Attendance> findByUserId(Long userId);

    Attendance findByUserIdAndDate(
            Long userId,
            String date);

    boolean existsByUserIdAndDate(
            Long userId,
            String date);
}