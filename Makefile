.PHONY: nova all resources
all: nova
	
nova:
	bash bin/make-isl.sh	
	sbt compile

resources:
	bash bin/update_resources.sh

clean:
	sbt "; forge/clean; core/clean; nova/clean"
	sbt clean

