package jp.co.kpscorp.rc6.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.Usertbl;

@Repository
public interface UsertblRepository extends JpaRepository<Usertbl, Integer> {
	List<Usertbl> findByUseridLike(String uid);

	List<Usertbl> findByUserid(String uid);
}
