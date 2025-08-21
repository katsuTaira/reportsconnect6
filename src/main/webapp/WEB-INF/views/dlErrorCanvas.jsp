<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
  "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link type="image/x-icon" rel="shortcut icon"
	href="http://techsupport.kpscorp.jp/templates/newkokyakuroku/favicon.ico">
<link rel="stylesheet" type="text/css"
	href="${baseUrl}/resources/common.css">
<link rel="stylesheet" type="text/css"
	href="${baseUrl}/resources/extended.css">
<link rel="stylesheet" type="text/css"
	href="${baseUrl}/resources/elements.css">
<link rel="stylesheet" type="text/css"
	href="${baseUrl}/resources/security_page.css">
<style type="text/css">
#main-logo {
	display: block;
	font-size: 20pt;
	height: 80px;
	overflow: hidden;
	position: absolute;
	width: 300px;
	z-index: 12;
	margin-top: 0px;
	padding-top: 30px;
	padding-left: 10px;
	color: Snow;
}
#pagewrap {
    width: 475px;
    margin: 0 auto;
    text-align: left;
}
#headerwrap {
	height:90px;
	padding-bottom:0px !important;
	text-align:left;
	width:475px;
}

#contentwrap {
    width:448px;
    overflow: visible;
	padding: 36px 30px 30px 30px;
}
#header {
	width:510px;
	height:76px;
	margin-top: 6px;
	margin-right:0px;
	float: left;
}

#headertext {
	font-size:12px;
	margin:30px 0 0;
	width:500px;
	text-align: right;
	z-index:10;
	position: relative;
}
</style>
<title>エラーページ</title>
</head>
<body>
	<div id="pagewrap">
		<a id="main-logo"
			href="http://techsupport.kpscorp.jp/reportsconnect.html">ReportsConnect</a>
		<div id="headerwrap">
			<div id="header">
				<div id="headertext">KPSの印刷ソリューション</div>
				<!-- .header-text-->
			</div>
		</div>
		<!-- .headerwrap -->
		<div id="contentwrap">
			<div class="oauthcontent" id="oauthcontent" style="height: 259px;">
				<div id="client_details" style="padding-top: 50px;">
					<img src="${baseUrl}/resources/ReportsConnect.png">
					<div id="client_description">印刷エラー。</div>
				</div>
				<div class="arrow"></div>
				<div id="perms_wrap" style="padding-top: 60px;">
					<h2 id="shorih">以下の理由により、印刷処理ができませんでした。</h2>
					<div class="oauth_text bullets">
						${error}:${error_description}</div>
				</div>
			</div>
			&nbsp;
		</div>
		<!-- #contentwrap -->
		<!--  	<div id="footer">Copyright &copy; 2000-2012 salesforce.com, inc.
			All rights reserved.</div> -->
		<!-- #footer -->
	</div>

</body>
</html>