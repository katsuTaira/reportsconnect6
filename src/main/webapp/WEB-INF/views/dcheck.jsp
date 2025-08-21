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
<style type="text/css">
#acceptdeny {
    padding-top: 10px;
    text-align: center;
}
</style>
<title>データの確認</title>
</head>
<body>
	<div class="bodyDiv brdPalette brandPrimaryBrd">
		<div class="bPageTitle">
			<div class="ptBody">
				<div class="content">
					<img title="データの確認" class="pageTitleIcon" alt="データの確認"
						src="http://techsupport.kpscorp.jp/templates/newkokyakuroku/favicon.ico">
					<h1 class="pageType">
						ReportsConnect<span class="titleSeparatingColon">:</span>
					</h1>
					<h2 class="pageDescription">データの確認</h2>
					<div class="blank">&nbsp;</div>
				</div>
			</div>
			<div class="ptBreadcrumb"></div>
		</div>
		<div class="apexp">
			<div class="individualPalette">
				<div class="Custom85Block">
					<div
						class="bPageBlock brandSecondaryBrd apexDefaultPageBlock secondaryPalette">
						<div class="pbBody">
							<c:if test="${ex!=null}">
								<div id="CSRFDelError" class="message errorM3">
									<table cellspacing="0" cellpadding="0" border="0"
										class="messageTable">
										<tbody>
											<tr>
												<td><img title="エラー" class="msgIcon" alt="エラー"
													src="${baseUrl}/resources/er.png"></td>
												<td class="messageCell"><div class="messageText">
														<h4>
															以下の例外が発生しています！<br>
														</h4>
														<p>
														<pre>${ex.msgOrStr}</pre>
														</p>
													</div></td>
											</tr>
										</tbody>
									</table>
								</div>
							</c:if>
							<c:if test="${hint!=null}">
								<fieldset class="note">
									<legend>ヒント! </legend>
									<div class="noteBody">
										<img align="left"
											src="${baseUrl}/resources/helpNote_icon.gif"> ${hint}
									</div>
								</fieldset>
							</c:if>
							<div>
								<div class="pbSubsection">
									<table cellspacing="0" cellpadding="0" border="0"
										class="detailList">
										<tbody>
											<c:if test="${ex!=null}">
												<tr>
													<td class="labelCol  first "><label> Stuck
															Trace</label></td>
													<td class="data2Col  first "><textarea rows="6"
															cols="115">
														<c:forEach items="${ex.stackTrace}" var="el">
at ${el.className}.${el.methodName}(${el.lineNumber})</c:forEach>
														</textarea></td>
												</tr>
											</c:if>
											<tr>
												<td class="labelCol  first "><label> SOQL</label></td>
												<td class="data2Col  first "><textarea rows="6"
														cols="115">${ql}	</textarea></td>
											</tr>
											<tr>
												<td class="labelCol  first "><label> CSVデータソース</label></td>
												<td class="data2Col  first ">
													<form method='POST' action='uc'>
														<input type='hidden' value='CsvDataSource.csv'
															name='ksp_filename' />
														<textarea rows="6" cols="115"  name='kps_textarea'>${cw.csv}</textarea>
														<input type='submit' value='ファイルCSVデータソースのダウンロード' />
													</form>
												</td>
											</tr>
											<c:forEach items="${cw.csvws}" var="csvw">
												<tr>
													<td class="labelCol  first "><label>
															CSVデータソース：${csvw.name}</label></td>
													<td class="data2Col  first ">
														<form method='POST' action='uc'>
															<input type='hidden'
																value='${csvw.name}CsvDataSource.csv'
																name='ksp_filename' />
															<textarea rows="6" cols="115"  name='kps_textarea'>${csvw.csv}</textarea>
															<input type='submit' value='ファイルCSVデータソースのダウンロード' />
														</form>
													</td>
												</tr>
											</c:forEach>
											<tr>
												<td class="labelCol  first "><label>
														読み込んだデータの構造（先頭10件まで）</label></td>
												<td class="data2Col  first "><textarea rows="6"
														cols="115">${dataStr}	</textarea></td>
											</tr>
											<tr>
												<td class="labelCol  first "><label> パラメータ一覧</label></td>
												<td class="data2Col  first "><textarea rows="6"
														cols="115">${pmapStr}	</textarea></td>
											</tr>

										</tbody>
									</table>
								</div>
							</div>
							<div id="acceptdeny">
								<input type="button" title="閉じる" class="btnPrimary btn allowBtn"
									value=" 閉じる " onClick="window.close();"">
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

	</div>


</body>
</html>