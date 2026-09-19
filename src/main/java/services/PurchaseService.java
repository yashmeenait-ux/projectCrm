package services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import entities.Course;
import entities.Purchase;
import repositories.CourseRepository;
import repositories.PurchaseRepository;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CourseRepository courseRepository;

    public boolean savePurchase(Purchase purchase) {

        if (purchaseRepository.existsByUserIdAndCourseId(
                purchase.getUserId(),
                purchase.getCourseId())) {

            return false;
        }

        purchaseRepository.save(purchase);

        return true;
    }

    public List<Purchase> getPurchasesByUser(Long userId) {
        return purchaseRepository.findByUserId(userId);
    }

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    // NEW METHOD
    public List<Course> getPurchasedCourses(Long userId) {

        List<Purchase> purchases = purchaseRepository.findByUserId(userId);

        List<Long> courseIds = purchases.stream()
                .map(Purchase::getCourseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        return courseRepository.findAllById(courseIds);
    }
}