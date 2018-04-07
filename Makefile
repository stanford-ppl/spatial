.PHONY: nova all resources apps
all: nova

apps:  
	sbt "; project apps; compile"

nova:
	bash bin/make-isl.sh	
	sbt compile

resources:
	bash bin/update_resources.sh

clean:
	sbt "; forge/clean; core/clean; nova/clean"
	sbt clean

