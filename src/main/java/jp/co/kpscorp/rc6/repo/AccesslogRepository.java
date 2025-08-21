package jp.co.kpscorp.rc6.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.Accesslog;

@Repository
public interface AccesslogRepository extends JpaRepository<Accesslog, Integer> {
	List<Accesslog> findByUseridLike(String uid);

	List<Accesslog> findByOrgidAndUserid(String orgid, String userid);
}
