.PHONY: nova all resources apps
all: apps

install: 
	sbt "; project emul; publishLocal"

apps:  
	sbt "; project apps; compile"
	sbt "; project emul; publishLocal"

nova:
	sbt compile

resources:
	bash bin/update_resources.sh

clean:
	sbt "; forge/clean; core/clean; nova/clean"
	sbt clean

