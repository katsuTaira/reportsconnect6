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
	@Transient
	private String orgid;

	public String getOrgid() {
		if (organizationBean != null) {
			return organizationBean.getOrgid();
		}
		return orgid;
	}

	@ManyToOne
	@JoinColumn(name = "license")
	private License licenseBean;
	@Transient
	private String licensename;

	public String getLicensename() {
		if (licenseBean != null) {
			return licenseBean.getLicensename();
		}
		return licensename;
	}

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
