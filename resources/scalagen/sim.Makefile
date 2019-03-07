all: 
	echo "$$(date +%s)" > start.log
	sbt compile
	echo "$$(date +%s)" > end.log

clean:
	rm -rf target

dse-model: 
	sbt "; project model; runMain model.AppRuntimeModel_dse ${ARGS}"

final-model: 
	sbt "; project model; runMain model.AppRuntimeModel_final ${ARGS}"