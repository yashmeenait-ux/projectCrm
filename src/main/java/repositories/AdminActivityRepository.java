package repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import entities.AdminActivity;

public interface AdminActivityRepository
        extends JpaRepository<AdminActivity, Long>{

    List<AdminActivity>
    findTop10ByOrderByActivityTimeDesc();

}