<!DOCTYPE>
<html>
<head>
<meta charset="utf-8" />
<title>Slider</title>
<link href="css/classic-min.css" rel="stylesheet">
<style type="text/css">
.center {
	margin-left: auto;
	margin-right: auto;
}

body {
	text-align: center;
}
</style>

<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"
	type="text/javascript"></script>
<script
	src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"
	type="text/javascript"></script>
<script src="js/jQDateRangeSlider-min.js" type="text/javascript"></script>
</head>
<body>
	<%
		String emax = (String) request.getAttribute("emax");
		String emin = (String) request.getAttribute("emin");
		String amax = (String) request.getAttribute("amax");
		String amin = (String) request.getAttribute("amin");

		if (emax == null || emin == null || amax == null || amin == null) {
			out.println("Date values not set!");
		} else {
	%>
	<img border="0" src="elgoog.jpg" style="width: 327px; height: 265px;"
		class="center">

	<br> Advanced search:
	<form method="POST" action="search">
		<input type='hidden' name='simple_query' /> <input type="hidden"
			name="entering_max" id="entering_max" value="<%=emax%>" /> <input
			type="hidden" name="entering_min" id="entering_min" value="<%=emin%>" />
		<input type="hidden" name="answering_max" id="answering_max"
			value="<%=amax%>" /> <input type="hidden" name="answering_min"
			id="answering_min" value="<%=amin%>" />
		<table border="1" class="center">
			<tr>
				<td>Query</td>
				<td><input type='text' name='query' /></td>
			</tr>
			<tr>
				<td>Entering date:</td>
				<td><div id="entering" style="margin: 5px;"></div></td>
			</tr>
			<tr>
				<td>Answering date:</td>
				<td><div id="answering" style="margin: 5px;"></div></td>
			</tr>
			<tr>
				<td>&nbsp;</td>
				<td><input type='submit' value='Search' /></td>
			</tr>
		</table>
	</form>
	<script type="text/javascript">
		// Months are range 0-11
		$('#entering').dateRangeSlider({
			bounds : {
				min : new Date('<%=emin %>'),
				max : new Date('<%=emax %>')
			},
			defaultValues : {
				min : new Date('<%=emin %>'),
				max : new Date('<%=emax %>')
			},
			arrows : false,
			valueLabels : "change",
			durationIn : 500,
			durationOut : 300
		});

		$('#answering').dateRangeSlider({
			bounds : {
				min : new Date('<%=amin %>'),
				max : new Date('<%=amax %>')
			},
			defaultValues : {
				min : new Date('<%=amin %>'),
				max : new Date('<%=amax %>')
			},
			arrows : false,
			valueLabels : "change",
			durationIn : 500,
			durationOut : 300
		});

		$('#entering').on(
				"userValuesChanged",
				function(e, data) {
					dmax = data.values.max;
					dmin = data.values.min;
					$('#entering_max').val(
							dmax.getFullYear() + '-' + (dmax.getMonth() + 1)
									+ '-' + dmax.getDate());
					$('#entering_min').val(
							dmin.getFullYear() + '-' + (dmin.getMonth() + 1)
									+ '-' + dmin.getDate());
				});

		$('#answering').on(
				"userValuesChanged",
				function(e, data) {
					dmax = data.values.max;
					dmin = data.values.min;
					$('#answering_max').val(
							dmax.getFullYear() + '-' + (dmax.getMonth() + 1)
									+ '-' + dmax.getDate());
					$('#answering_min').val(
							dmin.getFullYear() + '-' + (dmin.getMonth() + 1)
									+ '-' + dmin.getDate());
				});
	</script>
	<%
		}
	%>
</body>
</html>