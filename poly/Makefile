all: clean
	@mkdir -p ${HOME}/bin
	gcc resources/emptiness.c -o emptiness  `pkg-config --cflags --libs isl`
	@cp emptiness ${HOME}/bin
	@export PATH=${PATH}:${HOME}/bin

clean:
	@rm -f emptiness ||:
	@rm -f ${HOME}/bin/emptiness ||:
