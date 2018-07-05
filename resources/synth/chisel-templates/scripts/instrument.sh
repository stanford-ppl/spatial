#!/bin/bash

if [[ ! -f instrumentation.txt ]]; then
	echo "No instrumentation file found!  Did you turn on --instrumentation during Spatial compile?"
	exit 1
fi

if [[ ! -f html/controller_tree.html.bak ]]; then
	cp html/controller_tree.html html/controller_tree.html.bak
fi

appname=`cat html/controller_tree.html | grep "Diagram for" | sed 's/.*Diagram for //g' | sed 's/<\/h2>.*//g'`
# sed -i 's/<h2>Controller Diagram for (.*)<\/h2>/<h2>Controller Diagram for \\1 - <font color="red">Instrumentation Annotiations <\/font><\/h2>/g' html/controller_tree.html
sed -i "s/<h2>Controller Diagram for .*<\/h2>/<h2>Controller Diagram for $appname<\/h2><h2><font color=\"red\">Instrumentation Annotiations <\/font><\/h2>/g" html/controller_tree.html
while IFS='' read -r line || [[ -n "$line" ]]; do
	sym=`echo "$line" | sed "s/^ \+//g" | sed "s/ - .*//g"`
	cycsper=`echo "$line" | sed "s/^.* - //g" | sed "s/ (.*//g"`
	math=`echo "$line" | sed "s/^.* (/(/g" | sed "s/ \// total cycles,/g" | sed "s/)/ total iters)/g" | sed "s/).*/)/g"`
	perprnt=`echo "$line" | sed "s/^.*\[/\[/g" | sed "s/\].*/\]/g"`
	if [[ ! -z $sym ]]; then
		linenum=`awk "/<b>$sym.*<\/b>/{ print NR; exit }" html/controller_tree.html`
		sed -i "${linenum}i <br><font color=\"red\"> $cycsper cycles/iter<br><font size=\"2\">$math<br>$perprnt</font></font>" html/controller_tree.html
		# perl -i -pe "s|(<b>$sym.*?</b>)|<b>$sym - <font color=\"red\"> $cycsper cycles/iter<br><font size=\"2\">$math<br>$perprnt</font></font></b>|" html/controller_tree.html
	fi
done < instrumentation.txt

hasguide=`cat html/controller_tree.html | grep "Instrumentation Guide" | wc -l`
if [[ $hasguide -eq 0 ]]; then
	cat scripts/guide.html >> html/controller_tree.html
fi