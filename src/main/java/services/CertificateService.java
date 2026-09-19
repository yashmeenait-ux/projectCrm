package services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import entities.Certificate;
import repositories.CertificateRepository;

@Service
public class CertificateService {

	  @Autowired
	    private CertificateRepository certificateRepository;

	    public void saveCertificate(
	            Certificate certificate) {

	        certificateRepository.save(
	                certificate);
	    }

	    public List<Certificate> getAllCertificates() {

	        return certificateRepository.findAll();
	    }

	    public void deleteCertificate(
	            Long id) {

	        certificateRepository.deleteById(id);
	    }

	    public Certificate getCertificateById(
	            Long id) {

	        return certificateRepository
	                .findById(id)
	                .orElse(null);
	    }

	    public long getCertificateCount(
	            Long userId){

	        return certificateRepository
	                .findByUserId(userId)
	                .size();
	    }

	    public List<Certificate> getCertificatesByUser(
	            Long userId){

	        return certificateRepository
	                .findByUserId(userId);
	    }
	    
	    public boolean certificateExists(
	            Long userId,
	            String courseName){

	        return certificateRepository
	                .existsByUserIdAndCourseName(
	                        userId,
	                        courseName);
	    }
}