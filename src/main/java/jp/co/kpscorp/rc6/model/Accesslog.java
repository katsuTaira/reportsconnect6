package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "accesslog")
@Data
public class Accesslog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private Date lastdate;
	private Integer accesscnt;
	private Integer pageovercnt;
	private Integer memovercnt;
	private Integer idruningcnt;
	private Integer otherercnt;
	private String licensename;
	private String memo;
	private String lastjson;
	private String ip;
	private Integer datacheckcnt;
	private Integer v5cnt;
	private String userid;
	private String orgid;
	@ManyToOne
	@JoinColumn(name = "usertbl")
	private Usertbl usertblBean;
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

	public Accesslog(String oid, String uid) {
		// TODO Auto-generated constructor stub
	}

}
