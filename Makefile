.PHONY: all resources apps test
all: apps

###-----------------------------------###
## Publish spatial locally to ivy2.    ##
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
## Publish spatial locally to m2.      ##
###-----------------------------------###
publishM2Local: export FRINGE_PACKAGE=''
publishM2Local: 
	@echo $(FRINGE_PACKAGE)
	sbt "; project emul; +publishM2"
	sbt "; project fringe; publishM2"
	sbt "; project argon; publishM2"
	sbt "; project forge; publishM2"
	sbt "; project spatial; publishM2"
	sbt "; project models; publishM2"
	sbt "; project poly; publishM2"
	sbt "; project utils; publishM2"

###-----------------------------------###
## Publish spatial locally to m2.      ##
###-----------------------------------###
publishM2Remote: export FRINGE_PACKAGE="" 
publishM2Remote: 
	@echo $(FRINGE_PACKAGE)
	sbt "; project emul; +publishSigned"
	sbt "; project fringe; publishSigned"
	sbt "; project argon; publishSigned"
	sbt "; project forge; publishSigned"
	sbt "; project spatial; publishSigned"
	sbt "; project models; publishSigned"
	sbt "; project poly; publishSigned"
	sbt "; project utils; publishSigned"

###-----------------------------------###
## Update fringe and emul libs.        ##
###-----------------------------------###
install: 
	mkdir -p ${HOME}/bin
	bash bin/make_poly.sh
	cp poly/emptiness ${HOME}/bin
	export PATH=${PATH}:${HOME}/bin
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


