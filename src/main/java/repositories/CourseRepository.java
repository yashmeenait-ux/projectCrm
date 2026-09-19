package repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import entities.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

}