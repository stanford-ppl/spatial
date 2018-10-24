.PHONY: all resources apps test pir
all: apps

###-----------------------------------###
## Publish spatial locally.            ##
###-----------------------------------###
publish: 
	sbt "; project emul; +publishLocal; project fringe; publishLocal; project argon; publishLocal; project forge; publishLocal; project spatial; publishLocal; project models; publishLocal; project poly; publishLocal; project utils; publishLocal"

###-----------------------------------###
## Update fringe and emul libs.        ##
###-----------------------------------###
install: 
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal; project fringe; publishLocal"

###-----------------------------------###
## Update pir libs.                    ##
###-----------------------------------###
pir:
	git submodule update --init
	cd pir; sbt publishAll

###-----------------------------------###
## Make all apps (but not tests).      ##
###-----------------------------------###
apps:  
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal; project fringe; publishLocal; project apps; compile"

app: apps

###-----------------------------------###
## Make all tests and apps.            ##
###-----------------------------------###
tests:
	bash bin/make_poly.sh
	sbt "; project emul; +publishLocal; project fringe; publishLocal; project apps; compile; test:compile"

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
	sbt "; forge/clean; argon/clean; spatial/clean; clean"
