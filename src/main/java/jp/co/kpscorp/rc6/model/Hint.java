package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "hint")
@Data
public class Hint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String msgstring;
	private String hintmessage;
	private String kind;

	@Column
	@Version
	private Timestamp lastmodifieddate;

	@PreUpdate
	void preUpdate() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

	@PrePersist
	void preInsert() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

}
