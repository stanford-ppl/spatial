.PHONY: all resources apps test
all: apps

###-----------------------------------###
## Update local numeric emulation lib. ##
###-----------------------------------###
install: 
	bash bin/make_poly.sh
	sbt "; project emul; publishLocal"
	sbt "; project templateResources; publishLocal"

###-----------------------------------###
## Make all apps (but not tests).      ##
###-----------------------------------###
apps:  
	bash bin/make_poly.sh
	sbt "; project emul; publishLocal"
	sbt "; project templateResources; publishLocal"
	sbt "; project apps; compile"

app: apps

###-----------------------------------###
## Make all tests and apps.            ##
###-----------------------------------###
tests:
	bash bin/make_poly.sh
	sbt "; project emul; publishLocal"
	sbt "; project templateResources; publishLocal"
	sbt "; project apps; compile"
	sbt test:compile

test: tests

###-----------------------------------###
## Update the files_list for Java jar. ##
###-----------------------------------###
resources:
	bash bin/update_resources.sh
	sbt "; project templateResources; publishLocal"

###-----------------------------------###
## Remove all generated files.	       ##
###-----------------------------------###
clear: 
	rm -f *.log	
	rm -f *.sim
	rm -rf logs
	rm -rf gen
	rm -rf reports

###-----------------------------------###
## Clean all compiled Scala projects   ##
###-----------------------------------###
clean:
	sbt "; forge/clean; argon/clean; nova/clean"
	sbt clean


