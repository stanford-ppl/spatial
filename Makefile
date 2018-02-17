all: nova

nova:
	bash bin/make-isl.sh	
	sbt compile

clean:
	sbt clean

