package jp.co.kpscorp.rc6.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
	List<Organization> findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqual(
			String orgid, Date startdate, Date enddate);

	List<Organization> findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqualAndNumberofGreaterThan(
			String orgid, Date startdate, Date enddate, Integer numberof);

}
