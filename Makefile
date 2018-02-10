all: pcc

pcc:
	bash bin/make-isl.sh	
	sbt compile

clean:
	sbt clean

