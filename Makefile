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
publishM2Local:
	bin/publish local

###-----------------------------------###
## Publish spatial to OSS repo  .      ##
###-----------------------------------###
publishM2Snapshot:
	bin/publish remoteSnapshot

###-----------------------------------###
## Publish spatial to OSS repo  .      ##
###-----------------------------------###
publishM2Release:
	bin/publish remoteRelease

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
	git submodule update --init
	bin/update_resources.sh
	cd pir && make install

pir-update:
	git submodule update pir
	bin/update_resources.sh
	cd pir && make update

pir-clean:
	make -C pir/ clean

pir-develop:
	git submodule update --init --recursive
	bin/update_resources.sh
	cd pir/ && git checkout develop && make install

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
## Update the models package.          ##
###-----------------------------------###
models:
	sbt "; project models; publishLocal"

###-----------------------------------###
## Update local emul package.          ##
###-----------------------------------###
emul:
	sbt "; project emul; publishLocal"


###-----------------------------------###
## Make all documentation .            ##
###-----------------------------------###
doc:
	bin/scrub_doc prep
	sbt doc
	bin/scrub_doc replace
	bin/scrub_doc scrub
	cat build.sbt | grep "version :="
	echo "Please publish to spatial-doc, under appropriate version:"
	echo "  cp -r target/scala-2.12/api/* ~/spatial-doc/<version>"


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
clean: clean-argon clean-forge clean-spatial clean-emul
	sbt clean

###-----------------------------------###
## Clean Spatial projects              ##
###-----------------------------------###
clean-spatial:
	rm -f $(HOME)/bin/emptiness
	sbt "; spatial/clean"

###-----------------------------------###
## Clean Argon projects                ##
###-----------------------------------###
clean-argon:
	rm -f $(HOME)/bin/emptiness
	sbt "; argon/clean"

###-----------------------------------###
## Clean Forge projects                ##
###-----------------------------------###
clean-forge:
	rm -f $(HOME)/bin/emptiness
	sbt "; forge/clean"

###-----------------------------------###
## Clean Emul projects                ##
###-----------------------------------###
clean-emul:
	rm -f $(HOME)/bin/emptiness
	sbt "; emul/clean"
