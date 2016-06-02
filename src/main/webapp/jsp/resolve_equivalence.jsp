<<<<<<< HEAD
<% String pageTitle="Resolve Equivalence"; %>
<%@ include file="/jsp/header_game.jsp" %>
=======
<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - Equivalence Challenge</title>

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- jQuery -->
	<script src="js/jquery.min.js" type="text/javascript" ></script>

	<!-- Slick -->
	<link href="css/slick_1.5.9.css" rel="stylesheet" type="text/css" />
	<script src="js/slick_1.5.9.min.js" type="text/javascript" ></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript" ></script>
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

	<!-- Codemirror -->
	<script src="codemirror/lib/codemirror.js" type="text/javascript" ></script>
	<script src="codemirror/mode/clike/clike.js" type="text/javascript" ></script>
	<script src="codemirror/mode/diff/diff.js" type="text/javascript" ></script>
	<link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet" type="text/css" />

	<script>
		$(document).ready(function() {
			$('.single-item').slick({
				arrows: true,
				infinite: true,
				speed: 300,
				draggable:false
			});
			$('#messages-div').delay(10000).fadeOut();
		});
	</script>
</head>
<body>

<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<% Game game = (Game) session.getAttribute("game"); %>

<nav class="navbar navbar-inverse navbar-fixed-top">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1" aria-expanded="false">
			</button>
			<a class="navbar-brand" href="/">
				<span><img class="logo" href="/" src="images/logo.png"/></span>
				Code Defenders
			</a>
		</div>
		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
			<ul class="nav navbar-nav navbar-left">
				<li><a href="games/user">My Games</a></li>
				<li class="navbar-text">Game ID: <%= game.getId() %></li>
				<li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
				<li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
				<% if (game.getAliveMutants().size() == 1) {%>
				<li class="navbar-text">1 Mutant Alive</li>
				<% } else {%>
				<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants Alive</li>
				<% }%>
				<li class="navbar-text">
					<% if (game.getState().equals(ACTIVE)) {%>
						<% if (game.getActiveRole().equals(Game.Role.ATTACKER)) {%>
							<span class="label label-primary turn-badge">Your turn</span>
						<% } else { %>
							<span class="label label-default turn-badge">Waiting</span>
						<% } %>
					<% } else { %>
						<span class="label label-default turn-badge">Finished</span>
					<% } %>
				</li>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<li></li>
				<li>
					<p class="navbar-text">
						<span class="glyphicon glyphicon-user" aria-hidden="true"></span>
						<%=request.getSession().getAttribute("username")%>
					</p>
				</li>
				<li><input type="submit" form="logout" class="btn btn-inverse navbar-btn" value="Log Out"/></li>
			</ul>
		</div>
	</div>
</nav>

<form id="logout" action="login" method="post">
	<input type="hidden" name="formType" value="logOut">
</form>

<%
	ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
	request.getSession().removeAttribute("messages");
	if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
	<% for (String m : messages) { %>
	<pre><strong><%=m%></strong></pre>
	<% } %>
</div>
<%	} %>
>>>>>>> 37098c05c265c5f6f462ceae5c51150a1695cb37

<div class="row-fluid">
	<div class="col-md-6">
		<h2> Class Under Test </h2>
		<input type="hidden" name="formType" value="createMutant">
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" cols="80" rows="50"><%= game.getCUT().getAsString() %></textarea></pre>
		<h2> Tests </h2>
		<div class="slider single-item">
			<%
				boolean isTests = false;
				for (Test t : game.getExecutableTests()) {
					isTests = true;
					String tc = "";
					for (String line : t.getHTMLReadout()) { tc += line + "\n"; }
			%>
			<div><h4>Test <%= t.getId() %></h4><pre class="readonly-pre"><textarea class="utest readonly-textarea" cols="20" rows="10"><%=tc%></textarea></pre></div>
			<%
				}
				if (!isTests) {%>
			<div><h2></h2><p> There are currently no tests </p></div>
			<%}
			%>
		</div> <!-- slider single-item -->
	</div> <!-- col-md6 left -->
	<div class="col-md-6">
		<h2>Equivalent mutant?</h2>
		<table class="table table-hover table-responsive table-paragraphs">

			<%
				ArrayList<Mutant> equivMutants = game.getMutantsMarkedEquivalent();
				if (! equivMutants.isEmpty()) {
					Mutant m = equivMutants.get(0);
			%>
			<tr>
				<td>
					<h4>Mutant <%= m.getId() %></h4>
				</td>
				<td>
					<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
					<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog">
						<div class="modal-dialog">
							<!-- Modal content-->
							<div class="modal-content">
								<div class="modal-header">
									<button type="button" class="close" data-dismiss="modal">&times;</button>
									<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
								</div>
								<div class="modal-body">
									<pre class="readonly-pre"><textarea class="mutdiff readonly-textarea" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
								</div>
								<div class="modal-footer">
									<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
								</div>
							</div>
						</div>
					</div>
				</td>
				<td>
					<form id="equivalenceForm" action="play" method="post">
						<input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= m.getId() %>">
						<input type="hidden" name="formType" value="resolveEquivalence">
						<div class="btn-right">
						<input class="btn btn-default" name="acceptEquivalent" type="submit" value="Accept Equivalent">
						<input class="btn btn-primary" name="rejectEquivalent" type="submit" value="Submit Killing Test">							</div>
					</form>
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<% for (String change :	m.getHTMLReadout()) { %>
					<p><%=change%><p>
						<% } %>
				</td>
			</tr>
			<%
			} else {
			%>
			<tr class="blank_row">
				<td class="row-borderless" colspan="2">No mutant alive is marked as equivalent.</td>
			</tr>
			<%
				}
			%>
		</table>

		<h2>Not Equivalent? Write a killing test here</h2>
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
				    testCode = previousTestCode;
				} else
					testCode = game.getCUT().getTestTemplate();
			%>
	        <pre><textarea id="newtest" name="test" form="equivalenceForm" cols="80" rows="30"><%= testCode %></textarea></pre>
	</div> <!-- col-md6 right -->
</div> <!-- row-fluid -->

<script>
	var editorTest = CodeMirror.fromTextArea(document.getElementById("newtest"), {
		lineNumbers: true,
		indentUnit: 4,
		indentWithTabs: true,
		matchBrackets: true,
		mode: "text/x-java"
	});
	editorTest.on('beforeChange',function(cm,change) {
		var text = cm.getValue();
		var lines = text.split(/\r|\r\n|\n/);
		var readOnlyLines = [0,1,2,3,4,5,6,7];
		var readOnlyLinesEnd = [lines.length-1,lines.length-2];
		if ( ~readOnlyLines.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
			change.cancel();
		}
	});
	editorTest.setSize("100%", 500);
	var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
		lineNumbers: true,
		matchBrackets: true,
		mode: "text/x-java",
		readOnly: true
	});
	editorSUT.setSize("100%", 500);
	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
			mode: "text/x-java",
			readOnly: true
		});
	};
	/* Mutants diffs */
	$('.modal').on('shown.bs.modal', function() {
		var codeMirrorContainer = $(this).find(".CodeMirror")[0];
		if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
			codeMirrorContainer.CodeMirror.refresh();
		} else {
			var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
				lineNumbers: false,
				mode: "diff",
				readOnly: true /* onCursorActivity: null */
			});
			editorDiff.setSize("100%", 500);
		}
	});
</script>
<%@ include file="/jsp/footer.jsp" %>