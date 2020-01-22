all: 

clean:
	rm -rf target

dse-model: 
	sbt "; runMain model.AppRuntimeModel_dse"
