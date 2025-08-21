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
	font-size: 30pt;
	height: 80px;
	overflow: hidden;
	position: absolute;
	width: 300px;
	z-index: 12;
	margin-top: 0px;
	padding-top: 20px;
	padding-left: 10px;
	color: Snow;
}
</style>
<title>ダウンロードページ</title>
</head>
<body>
	<div id="pagewrap">
		<a id="main-logo"
			href="http://techsupport.kpscorp.jp/reportsconnect.html">ReportsConnect</a>
		<div id="headerwrap">
			<div id="header">
				<div id="headertext" style="font-size:18px"><a href="http://www.kpscorp.co.jp/">KPS</a>の印刷ソリューション</div>
				<!-- .header-text-->
			</div>
		</div>
		<!-- .headerwrap -->
		<div id="contentwrap">
			<div class="oauthcontent" id="oauthcontent" style="height: 259px;">
				<div id="client_details" style="padding-top: 50px;">
					<img
						src="${baseUrl}/resources/ReportsConnect.png">
					<div id="client_description">印刷サーバーにPDFファイルの作成を要求しました。</div>
				</div>
				<div class="arrow"></div>
				<div id="perms_wrap" style="padding-top: 5px;">
					<a href="http://www.reportsconnect.com/reportsmake.html"  target="blank"><img src="${baseUrl}/resources/makereports.png" border="0" alt="帳票作成" title="セールスフォース帳票" width="220" /></a><br/><br/>
					<h2 id="shorih">ただ今処理中...</h2>
					<div class="oauth_text bullets">
						PDFファイルのダウンロードが始まるまで、少々お待ちください。</div>
					<div class="oauth_text">
						ダウンロード完了後、このウインドウを閉じてください。</div>
					<div id="acceptdeny">
						<input type="button" title="閉じる" class="btnPrimary btn allowBtn"
							value=" 閉じる " onClick="window.close();"">
					</div>
				</div>
			</div>
			&nbsp;
		</div>
		<!-- #contentwrap -->
	<!--  	<div id="footer">Copyright &copy; 2000-2012 salesforce.com, inc.
			All rights reserved.</div> -->
		<!-- #footer -->
	</div>
	<script type="text/javascript">
		function addLoadEvent(func) {
			var oldonload = window.onload;
			if (typeof window.onload != 'function') {
				window.onload = func;
			} else {
				window.onload = function() {
					oldonload();
					func();
				}
			}
		}
		function doDl() {
			location.href = "dl2?state=${key}";
		}

		if ("${key}" != "") {
			//location.href = "dl?state=${key}";
			addLoadEvent(doDl);
		}
	</script>

</body>
</html>