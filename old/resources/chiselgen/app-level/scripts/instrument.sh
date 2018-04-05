#!/bin/bash

if [[ ! -f instrumentation.txt ]]; then
	echo "No instrumentation file found!  Did you turn on --instrumentation during Spatial compile?"
	exit 1
fi

appname=`cat controller_tree.html | grep "Diagram for" | sed 's/.*Diagram for //g' | sed 's/<\/h2>.*//g'`
# sed -i 's/<h2>Controller Diagram for (.*)<\/h2>/<h2>Controller Diagram for \\1 - <font color="red">Instrumentation Annotiations <\/font><\/h2>/g' controller_tree.html
sed -i "s/<h2>Controller Diagram for .*<\/h2>/<h2>Controller Diagram for $appname<\/h2><h2><font color=\"red\">Instrumentation Annotiations <\/font><\/h2>/g" controller_tree.html
while IFS='' read -r line || [[ -n "$line" ]]; do
	sym=`echo "$line" | sed "s/^ \+//g" | sed "s/ - .*//g"`
	cycsper=`echo "$line" | sed "s/^.* - //g" | sed "s/ (.*//g"`
	math=`echo "$line" | sed "s/^.* (/(/g" | sed "s/ \// total cycles,/g" | sed "s/)/ total iters)/g" | sed "s/).*/)/g"`
	perprnt=`echo "$line" | sed "s/^.*\[/\[/g" | sed "s/\].*/\]/g"`
	if [[ ! -z $sym ]]; then
		perl -i -pe "s|(<b>$sym.*?</b>)|<b>$sym - <font color=\"red\"> $cycsper cycles/iter<br><font size=\"2\">$math<br>$perprnt</font></font></b>|" controller_tree.html
	fi
done < instrumentation.txt

# insert the controller.js into the add the the html (it is a little hacky :-))
cat controller_tree.html |grep -v '</html>' > controller_tree_js.html
cat scripts/controller.js >> controller_tree_js.html
echo '</html>' >> controller_tree_js.html
