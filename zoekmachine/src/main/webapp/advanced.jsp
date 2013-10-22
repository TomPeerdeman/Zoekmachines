<!DOCTYPE>
<html>
<head>
<meta charset="utf-8" />
<title>Advanced search - Elgoog KVR</title>
<link href="css/classic-min.css" rel="stylesheet">
<link href="css/base.css" rel="stylesheet">

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
	<img src="elgoog.jpg" id="logo" class="center">

	<form method="POST" action="#" id="searchform" name="advsearchform">
		<input type='hidden' name='simple_query' id="simple_query" value="false" />
		<input type="hidden" name="entering_max" id="entering_max" value="<%=emax%>" />
		<input type="hidden" name="entering_min" id="entering_min" value="<%=emin%>" /> 
		<input type="hidden" name="answering_max" id="answering_max" value="<%=amax%>" /> 
		<input type="hidden" name="answering_min" id="answering_min" value="<%=amin%>" />
		<input type="hidden" name="page" id="page" value="1" />

		<table class="center" id="advtable">
			<tr>
				<td>Doc ID:</td>
				<td><input type='text' name='doc_id' /></td>
				<td rowspan="7">&nbsp;</td>
				
				<td>Keywords:</td>
				<td><input type='text' name='keywords' /></td>

			</tr>
			<tr>
				<td>Title :</td>
				<td><input type='text' name='title' /></td>
				
				<td>&nbsp;</td>
				<td>&nbsp;</td>				
			</tr>
			<tr>
				<td>Category:</td>
				<td><input type='text' name='category' /></td>
				
				<td>Answered:</td>
				<td><input type="radio" name="answered" value="y" />Yes&nbsp;&nbsp;
				<input type="radio" name="answered" value="n" />No&nbsp;&nbsp;
				<input type="radio" name="answered" value="yn" checked/>Show both</td>
			</tr>
			<tr>
				<td>Issue date:</td>
				<td><div id="entering" style="margin: 5px;"></div></td>
				
				<td>Answer date:</td>
				<td><div id="answering" style="margin: 5px;"></div>
				<div id='answering_none' style='display: none;'>None</div></td>
			</tr>
			<tr>
				<td>Questions:</td>
				<td><input type='text' name='questions' /></td>
				
				<td>Answers:</td>
				<td><input type='text' name='answers' /></td>

			</tr>
			<tr>
				<td>Questioner(s):</td>
				<td><input type='text' name='questioners' /></td>
				
				<td>Answerer(s):</td>
				<td><input type='text' name='answerers' /></td>

			</tr>
			<tr>
				<td>Questioners party:</td>
				<td><input type='text' name='questioners_party' /></td>
				
				<td>Answerers ministry:</td>
				<td><input type='text' name='answerers_ministry' /></td>
			</tr>
			
			<tr>
				<td colspan="4">&nbsp;</td>
			</tr>
			<tr>
				<td>&nbsp;</td>
				<td><input type='submit' id="submit_advanced" value='Search' /></td>
				<td>&nbsp;</td>
				<td><input type='button' id="simple_button"
					value='Simple search' /></td>
			</tr>
		</table>
	</form>
	<div class="center" id="result"></div>
	<script type="text/javascript">
		$('#entering').dateRangeSlider({
			bounds : {
				min : new Date('<%=emin%>'),
				max : new Date('<%=emax%>')
			},
			defaultValues : {
				min : new Date('<%=emin%>'),
				max : new Date('<%=emax%>')
			},
			arrows : false,
			valueLabels : "change",
			durationIn : 500,
			durationOut : 300
		});

		$('#answering').dateRangeSlider({
			bounds : {
				min : new Date('<%=amin%>'),
				max : new Date('<%=amax%>')
			},
			defaultValues : {
				min : new Date('<%=amin%>'),
				max : new Date('<%=amax%>')
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
					// postdata();
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
					// postdata();
				});

		$('#searchform').submit(function() {
			postdata();
			return false;
		});
		
		$('#simple_button').click(function() {
			window.location.href = '/';
		});
		
		$('input[name=answered]', '#searchform').change(function(){
			if($(this).val() == 'n') {
				$("#answering").dateRangeSlider("option", "enabled", false);
				$("#answering").fadeOut(function(){
					$("#answering_none").fadeIn();
				});
				$('#answering_max').val('');
				$('#answering_min').val('');
				
				$('input[name=answers]', '#searchform').val('').attr('disabled', true);
				$('input[name=answerers]', '#searchform').val('').attr('disabled', true);
				$('input[name=answerers_ministry]', '#searchform').val('').attr('disabled', true);
			} else {
				$("#answering").dateRangeSlider("option", "enabled", true);
				if($('#answering_max').val() == '' || $('#answering_min').val() == '') {
					$("#answering_none").fadeOut(function(){
						$("#answering").fadeIn();
					});
					
					$('input[name=answers]', '#searchform').attr('disabled', false);
					$('input[name=answerers]', '#searchform').attr('disabled', false);
					$('input[name=answerers_ministry]', '#searchform').attr('disabled', false);
					
					var values = $("#answering").dateRangeSlider("values");
					dmax = values.max;
					dmin = values.min;
					$('#answering_max').val(
							dmax.getFullYear() + '-' + (dmax.getMonth() + 1)
									+ '-' + dmax.getDate());
					$('#answering_min').val(
							dmin.getFullYear() + '-' + (dmin.getMonth() + 1)
									+ '-' + dmin.getDate());
				}
			}
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
		
		function applyPartyFilter(party) {
			document.advsearchform.questioners_party.value = party;
			postdata();
		}
		
		function setpage(page) {
			$('#page').val(page);
			postdata();
			$('#page').val(1);
		}
	</script>
	<%
		}
	%>
</body>
</html>
