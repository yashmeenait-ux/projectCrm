package services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import entities.AdminActivity;
import repositories.AdminActivityRepository;

@Service
public class AdminActivityService {

    @Autowired
    private AdminActivityRepository repository;

    public void saveActivity(
            String activity,
            String adminName){

        AdminActivity log =
                new AdminActivity();

        log.setActivity(activity);
        log.setAdminName(adminName);
        log.setActivityTime(
                LocalDateTime.now());

        repository.save(log);
    }

    public List<AdminActivity>
    getRecentActivities(){

        return repository
                .findTop10ByOrderByActivityTimeDesc();
    }
}