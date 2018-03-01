.PHONY: nova all
all: nova
	
nova:
	bash bin/make-isl.sh	
	sbt compile

clean:
	sbt "; forge/clean; core/clean; nova/clean"
	sbt clean

