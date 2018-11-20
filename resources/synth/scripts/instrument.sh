#!/bin/bash

if [[ ! -f instrumentation.txt ]]; then
	echo "No instrumentation file found!  Did you turn on --instrumentation during Spatial compile?"
	exit 1
fi

if [[ ! -f info/controller_tree.bak.html ]]; then
	cp info/controller_tree.html info/controller_tree.bak.html
else 
	cp info/controller_tree.bak.html info/controller_tree.html 
fi

appname=`cat info/controller_tree.html | grep "Diagram for" | sed 's/.*Diagram for //g' | sed 's/<\/h2>.*//g'`
# sed -i 's/<h2>Controller Diagram for (.*)<\/h2>/<h2>Controller Diagram for \\1 - <font color="red">Instrumentation Annotiations <\/font><\/h2>/g' info/controller_tree.html
sed -i "s/<h2>Controller Diagram for .*<\/h2>/<h2>Controller Diagram for $appname<\/h2><h2><font color=\"red\">Instrumentation Annotations <\/font><\/h2>/g" info/controller_tree.html
x=0
while IFS='' read -r line || [[ -n "$line" ]]; do
	if [[ x -eq 0 ]]; then
		sed -i "s/Instrumentation Annotations/Instrumentation Annotations ($line)/g" info/controller_tree.html
	else
		sym=`echo "$line" | sed "s/^ \+//g" | sed "s/ - .*//g" | sed "s/_.*//g"`
		cycsper=`echo "$line" | sed "s/^.* - //g" | sed "s/ (.*//g"`
		math=`echo "$line" | sed "s/^.* (/(/g" | sed "s/ \// total cycles,/g" | sed "s/)/ total iters)/g" | sed "s/).*/)/g"`
		perprnt=`echo "$line" | sed "s/^.*\[/\[/g" | sed "s/\].*/\]/g"`
		if [[ ! -z $sym ]]; then
			linenum=`awk "/<!-- Begin $sym -->/{ print NR; exit }" info/controller_tree.html`
			sed -i "$((linenum+3))i <br><font color=\"red\"> $cycsper cycles/iter<br><font size=\"2\">$math<br>$perprnt</font></font><br>" info/controller_tree.html
			# perl -i -pe "s|(<b>$sym.*?</b>)|<b>$sym - <font color=\"red\"> $cycsper cycles/iter<br><font size=\"2\">$math<br>$perprnt</font></font></b>|" info/controller_tree.html
		fi
	fi
	x=$((x+1))
done < instrumentation.txt

hasguide=`cat info/controller_tree.html | grep "Instrumentation Guide" | wc -l`
if [[ $hasguide -eq 0 ]]; then
	cat scripts/guide.html >> info/controller_tree.html
fi