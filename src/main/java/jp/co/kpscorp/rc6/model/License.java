package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "license")
@Data
public class License {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String licensename;
	private Integer maxpage;
	private Integer maxmen;
	private String serverurl;
	@Column
	@Version
	private Timestamp lastmodifieddate;
	@OneToMany(mappedBy = "licenseBean")
	private List<Usertbl> users;
	@OneToMany(mappedBy = "licenseBean")
	private List<Organization> organizations;

	@PreUpdate
	void preUpdate() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

	@PrePersist
	void preInsert() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

}
