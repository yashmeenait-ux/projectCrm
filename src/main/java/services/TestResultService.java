
package services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import entities.TestResult;
import repositories.TestResultRepository;

@Service
public class TestResultService {

    @Autowired
    private TestResultRepository testResultRepository;

    public void saveResult(TestResult result) {
        testResultRepository.save(result);
    }

    public List<TestResult> getResultsByUser(Long userId) {
        return testResultRepository.findByUserId(userId);
    }

    public List<TestResult> getAllResults() {
        return testResultRepository.findAll();
    }

    public void deleteResult(Long id) {
        testResultRepository.deleteById(id);
    }
    
    public long getTestCount(Long userId){

        return testResultRepository
                .findByUserId(userId)
                .size();
    }
    
   
}
