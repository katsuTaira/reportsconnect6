package jp.co.kpscorp.rc6.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.Hint;

@Repository
public interface HintRepository extends JpaRepository<Hint, Integer> {
	List<Hint> findByKindIsNullOrKind(String kind);
}
