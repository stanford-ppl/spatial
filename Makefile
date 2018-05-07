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

clear: 
	rm *.sim
	rm -rf logs
	rm -rf gen
	rm -rf reports

clean:
	sbt "; forge/clean; argon/clean; nova/clean"
	sbt clean

tests:
	sbt test:compile
