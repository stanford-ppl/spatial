.PHONY: all resources apps test
all: apps

###-----------------------------------###
## Publish spatial locally.            ##
###-----------------------------------###
publish: 
	sbt "; project emul; +publishLocal"
	sbt "; project fringe; publishLocal"
	sbt "; project argon; publishLocal"
	sbt "; project forge; publishLocal"
	sbt "; project spatial; publishLocal"
	sbt "; project models; publishLocal"
	sbt "; project poly; publishLocal"
	sbt "; project utils; publishLocal"

###-----------------------------------###
## Update fringe and emul libs.        ##
###-----------------------------------###
install: 
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal"
	sbt "; project fringe; publishLocal"

###-----------------------------------###
## Make all apps (but not tests).      ##
###-----------------------------------###
apps:  
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal"
	sbt "; project fringe; publishLocal"
	sbt "; project apps; compile"

app: apps

###-----------------------------------###
## Make all tests and apps.            ##
###-----------------------------------###
tests:
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal"
	sbt "; project fringe; publishLocal"
	sbt "; project apps; compile"
	sbt test:compile

test: tests

###-----------------------------------###
## Update the files_list for Java jar. ##
###-----------------------------------###
resources:
	bash bin/update_resources.sh
	sbt "; project fringe; publishLocal"


###-----------------------------------###
## Make all documentation .            ##
###-----------------------------------###
doc:
	bin/scrub_doc prep
	sbt doc
	bin/scrub_doc replace
	bin/scrub_doc scrub
	echo "Please publish to spatial-doc:"
	echo "  cp -r target/scala-2.12/api/* ~/spatial-doc"

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
	sbt "; forge/clean; argon/clean; spatial/clean"
	sbt clean


