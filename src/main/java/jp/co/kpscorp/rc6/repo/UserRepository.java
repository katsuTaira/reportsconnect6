package jp.co.kpscorp.rc6.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.kpscorp.rc6.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	List<User> findByUseridLike(String uid);
}
