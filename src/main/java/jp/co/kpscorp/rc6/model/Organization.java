package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "organization")
@Data
public class Organization {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private Integer numberof;
	private Date enddate;
	private String status;
	private Boolean oem;
	private String orgid;
	private Date startdate;
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

	@OneToMany(mappedBy = "organizationBean")
	private List<Usertbl> users;

	@PreUpdate
	void preUpdate() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

	@PrePersist
	void preInsert() {
		lastmodifieddate = new Timestamp(System.currentTimeMillis());
	}

}
