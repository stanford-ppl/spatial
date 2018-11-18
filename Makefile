.PHONY: all resources apps test pir
all: apps

###-----------------------------------###
## Publish spatial locally to ivy2.    ##
###-----------------------------------###
publish: 
	sbt "; project emul; +publishLocal; project fringe; publishLocal; project argon; publishLocal; project forge; publishLocal; project spatial; publishLocal; project models; publishLocal; project poly; publishLocal; project utils; publishLocal"

###-----------------------------------###
## Publish spatial locally to m2.      ##
###-----------------------------------###
publishM2Local: clean
	bin/publish local

###-----------------------------------###
## Publish spatial locally to m2.      ##
###-----------------------------------###
publishM2Remote: clean
	bin/publish remoteSnapshot

###-----------------------------------###
## Update fringe and emul libs.        ##
###-----------------------------------###
install: 
	bin/update_resources.sh
	sbt "; project emul; +publishLocal; project fringe; publishLocal"

###-----------------------------------###
## Update pir libs.                    ##
###-----------------------------------###
pir:
	#git submodule update --init --recursive
	cd pir; git checkout new_spatial; git pull; sbt publishAll

###-----------------------------------###
## Make all apps (but not tests).      ##
###-----------------------------------###
apps:  
	bin/update_resources.sh
	sbt "; project emul; +publishLocal; project fringe; publishLocal; project apps; compile"

app: apps

###-----------------------------------###
## Make all tests and apps.            ##
###-----------------------------------###
tests:
	bin/update_resources.sh
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
clean: clean-argon clean-forge clean-spatial
	sbt clean

###-----------------------------------###
## Clean Spatial projects              ##
###-----------------------------------###
clean-spatial:
	sbt "; spatial/clean"
	sbt clean

###-----------------------------------###
## Clean Argon projects                ##
###-----------------------------------###
clean-argon:
	sbt "; argon/clean"
	sbt clean

###-----------------------------------###
## Clean Forge projects                ##
###-----------------------------------###
clean-forge:
	sbt "; forge/clean"
	sbt clean
