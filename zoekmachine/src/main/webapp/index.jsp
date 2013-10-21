<!DOCTYPE>
<html>
<head>
<meta charset="utf-8" />
<title>Elgoog KVR</title>
<link href="css/base.css" rel="stylesheet">

<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"
	type="text/javascript"></script>

</head>
<body>
	<img src="elgoog.jpg" id="logo" class="center">

	<form method="POST" action="#" id="searchform">
		<input type='hidden' name='simple_query' id="simple_query"
			value="true" />

		<table class="center" id="simpletable">
			<tr>
				<td colspan="2"><input type='text' name='query' id="query" style="width:400px"/></td>
			</tr>
			<tr>
				<td style="width:50%"><center><input type='submit' id="submit_simple" value='Elgoog Search' /></center></td>
				<td style="width:50%"><center><input type='button' id="advanced_button"
					value='Advanced search' /></center></td>
			</tr>
		</table>
	</form>
	<div class="center" id="result"></div>
	<script type="text/javascript">
		$('#searchform').submit(function() {
			postdata();
			return false;
		});

		$('#advanced_button').click(function() {
			window.location.href = 'search?adv=true';
		});
		
		function postdata() {
			$.ajax({
				type : 'POST',
				url : 'search',
				data : $('#searchform').serialize(),
				cache : false,
				success : function(data) {
					$('#result').html(data);
					$(".toggle").click(function() {
						var myClasses = this.classList;
						$("p."+myClasses[1]).toggle();
					});
				}
			});
		}
	</script>
</body>
</html>
