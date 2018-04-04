<script> 
function update () { $("div h4").each(function() {
		var maxcycle = 0;
		var totalcycle = 0;
		var iters = 0;
		var text;
		var table = $(this).siblings('table');
		var mytext = table.children().children().children('td').each( function () { 
			<!-- 	  var cycle = parseInt($(this).html().match(/ (\d+) cycles/)[1],10);;  -->
                  var matches = $(this).text().match(/ (\d+) cycles.*? (\d+) total iters/);
		  var cycle = parseInt(matches[1],10);
		  iters = parseInt(matches[2],10);
			<!-- iters = $(this).html().match(/.*? (\d+) total iters/)[1];  -->
		  if (cycle > maxcycle) 
		    maxcycle = cycle;
		  totalcycle += cycle;
		});

		var parentcycle = parseInt($(this).parent().parent().html().match(/ (\d+) cycles/)[1],10); 
		var parentiters = $(this).parent().parent().html().match(/ (\d+) total iters/)[1]; 

		if (parentiters > 0) {
		  var cycle;
		  if ($(this).parent().parent().text().match(/^\S+_Seq/)) {
                     cycle = totalcycle;
		  } else {
                     cycle = maxcycle;
		  }

		  var percent = cycle * 100 * iters / (parentcycle * parentiters);
		  $(this).find("a").css( 
			  "background-color", "yellow"
			  );
		  $(this).text(percent.toFixed(2) + "%" + "(" + cycle +":" + iters + "/" + parentcycle +":" + parentiters + ")" ); 
		}
	   });
}

		update();
</script>
