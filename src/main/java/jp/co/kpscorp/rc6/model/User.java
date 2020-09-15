package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "user")
public class User {
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

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public Organization getOrganizationBean() {
		return organizationBean;
	}

	public void setOrganizationBean(Organization organizationBean) {
		this.organizationBean = organizationBean;
	}

	public License getLicenseBean() {
		return licenseBean;
	}

	public void setLicenseBean(License licenseBean) {
		this.licenseBean = licenseBean;
	}

	public Timestamp getLastmodifieddate() {
		return lastmodifieddate;
	}

	public void setLastmodifieddate(Timestamp lastmodifieddate) {
		this.lastmodifieddate = lastmodifieddate;
	}

	@PreUpdate
	void preUpdate() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

	@PrePersist
	void preInsert() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

}
