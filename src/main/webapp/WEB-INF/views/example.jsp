<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ page session="false"%>
<html>
<head>
<title>Long Poll Spring MVC</title>

<script type="text/javascript" src="<c:url value='/dojo/dojo.js.uncompressed.js'/>">
</script>
<script type="text/javascript" src="<c:url value='/resources/scripts/cloudfoundry-timeout.js'/>">
</script>

</head>
<body>
	<script type="text/javascript">
	function ajaxButtonClick(requestUrl) {
		dojo.xhrPost({
			url : requestUrl,
			load : function(result) {
				dojo.byId("text").innerHTML += "\n" + result;
			},
			error : function(result,ioargs) {
				dojo.byId("text").innerHTML += "\nERROR :" + result + " " + ioargs.xhr.status;
			}
		});
	}
	</script>
	<div>
		<input type="button" value="Ajax" onclick="ajaxButtonClick('ajaxrequest')" />
		<input type="button" value="Drip" onclick="ajaxButtonClick('ajaxdrip')" />
	</div>
	<div>
		<textarea id="text" rows="40" cols="80">Spring MVC</textarea>
	</div>
</body>
</html>