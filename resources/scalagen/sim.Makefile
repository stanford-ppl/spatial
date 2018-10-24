all: 
	echo "$$(date +%s)" > start.log
	sbt compile
	echo "$$(date +%s)" > end.log

clean:
	rm -rf target