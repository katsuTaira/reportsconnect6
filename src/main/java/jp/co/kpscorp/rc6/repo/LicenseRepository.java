package jp.co.kpscorp.rc6.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.License;

@Repository
public interface LicenseRepository extends JpaRepository<License, Integer> {
	List<License> findByLicensename(String licensename);
}
