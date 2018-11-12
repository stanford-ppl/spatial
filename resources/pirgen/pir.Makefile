all: 
	sbt "runMain AccelMain --run-psim --net=asic"

clean:
	rm -rf target
