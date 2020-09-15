package jp.co.kpscorp.rc6.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;

public class Accesslog {
	@Id
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
	private Integer v6cnt;
	private String userid;
	private String orgid;
	@ManyToOne
	@JoinColumn(name = "user")
	private User userBean;
	@Version
	private Timestamp lastmodifieddate;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getLastdate() {
		return lastdate;
	}

	public void setLastdate(Date lastdate) {
		this.lastdate = lastdate;
	}

	public Integer getAccesscnt() {
		return accesscnt;
	}

	public void setAccesscnt(Integer accesscnt) {
		this.accesscnt = accesscnt;
	}

	public Integer getPageovercnt() {
		return pageovercnt;
	}

	public void setPageovercnt(Integer pageovercnt) {
		this.pageovercnt = pageovercnt;
	}

	public Integer getMemovercnt() {
		return memovercnt;
	}

	public void setMemovercnt(Integer memovercnt) {
		this.memovercnt = memovercnt;
	}

	public Integer getIdruningcnt() {
		return idruningcnt;
	}

	public void setIdruningcnt(Integer idruningcnt) {
		this.idruningcnt = idruningcnt;
	}

	public Integer getOtherercnt() {
		return otherercnt;
	}

	public void setOtherercnt(Integer otherercnt) {
		this.otherercnt = otherercnt;
	}

	public String getLicensename() {
		return licensename;
	}

	public void setLicensename(String licensename) {
		this.licensename = licensename;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public String getLastjson() {
		return lastjson;
	}

	public void setLastjson(String lastjson) {
		this.lastjson = lastjson;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getDatacheckcnt() {
		return datacheckcnt;
	}

	public void setDatacheckcnt(Integer datacheckcnt) {
		this.datacheckcnt = datacheckcnt;
	}

	public Integer getV6cnt() {
		return v6cnt;
	}

	public void setV6cnt(Integer v6cnt) {
		this.v6cnt = v6cnt;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getOrgid() {
		return orgid;
	}

	public void setOrgid(String orgid) {
		this.orgid = orgid;
	}

	public User getUserBean() {
		return userBean;
	}

	public void setUserBean(User userBean) {
		this.userBean = userBean;
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
