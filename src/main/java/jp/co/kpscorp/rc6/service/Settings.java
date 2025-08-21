package jp.co.kpscorp.rc6.service;

import java.util.HashMap;
import java.util.Map;

import jp.co.kpscorp.rc6.model.License;

public class Settings {

	public static final String TH_USRPROP = "kps_usrprop";

	public static final String TH_PAGECNT = "kps_pagecnt";

	private Map<String, Keys> kmap = new HashMap<String, Keys>();

	public Map<String, Keys> getKmap() {
		return kmap;
	}

	public void setKmap(Map<String, Keys> kmap) {
		this.kmap = kmap;
	}

	public static class Keys {
		public Keys() {
			super();
		}

		public Keys(String description, String consumerKey,
				String consumerSeacret, String canvasSeacret) {
			super();
			this.description = description;
			this.consumerKey = consumerKey;
			this.consumerSeacret = consumerSeacret;
			this.canvasSeacret = canvasSeacret;
		}

		private String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getConsumerKey() {
			return consumerKey;
		}

		public void setConsumerKey(String consumerKey) {
			this.consumerKey = consumerKey;
		}

		public String getConsumerSeacret() {
			return consumerSeacret;
		}

		public void setConsumerSeacret(String consumerSeacret) {
			this.consumerSeacret = consumerSeacret;
		}

		private String consumerKey;
		private String consumerSeacret;
		private String canvasSeacret;

		public String getCanvasSeacret() {
			return canvasSeacret;
		}

		public void setCanvasSeacret(String canvasSeacret) {
			this.canvasSeacret = canvasSeacret;
		}

	}

	public static class Userprop {
		private String orgid;
		private String userid;
		private License license;
		private String description;

		public Userprop(String orgid, String userid, License license,
				String description) {
			super();
			this.orgid = orgid;
			this.userid = userid;
			this.license = license;
			this.description = description;
		}

		public String getOrgid() {
			return orgid;
		}

		public void setOrgid(String orgid) {
			this.orgid = orgid;
		}

		public String getUserid() {
			return userid;
		}

		public void setUserid(String userid) {
			this.userid = userid;
		}

		public License getLicense() {
			return license;
		}

		public void setLicense(License license) {
			this.license = license;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
