package services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import entities.Attendance;
import repositories.AttendanceRepository;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getAttendanceByUser(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);

    
    }
    
    
    public void saveAttendance(
            Attendance attendance) {

        attendanceRepository.save(attendance);
    }
    
    public Attendance getAttendanceByUserAndDate(
            Long userId,
            String date) {

        return attendanceRepository
                .findByUserIdAndDate(
                        userId,
                        date);
    }
    
    public double getAttendancePercentage(Long userId) {

        List<Attendance> attendanceList =
                attendanceRepository.findByUserId(userId);

        if(attendanceList.isEmpty()) {
            return 0;
        }

        long present =
                attendanceList.stream()
                        .filter(a ->
                                "Present".equalsIgnoreCase(
                                        a.getStatus()))
                        .count();

        return (present * 100.0)
                / attendanceList.size();
    }
    public void markAbsentIfNotExists(Long userId) {

        String today = LocalDate.now().toString();

        if (!attendanceRepository.existsByUserIdAndDate(userId, today)) {

            Attendance attendance = new Attendance();

            attendance.setUserId(userId);
            attendance.setDate(today);
            attendance.setStatus("Absent");

            attendanceRepository.save(attendance);
        }
    }
}

