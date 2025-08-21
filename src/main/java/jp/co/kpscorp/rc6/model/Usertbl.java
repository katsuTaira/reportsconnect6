package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "usertbl")
@Data
public class Usertbl {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String userid;
	@ManyToOne
	@JoinColumn(name = "organization")
	private Organization organizationBean;
	@ManyToOne
	@JoinColumn(name = "license")
	private License licenseBean;
	@Version
	private Timestamp lastmodifieddate;
	@Transient
	private String licensename;

	@PreUpdate
	void preUpdate() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

	@PrePersist
	void preInsert() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

}
