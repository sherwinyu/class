# Makefile for Fishnet

JAVAC = javac
FLAGS = -nowarn -g -d out
JAVA_FILES = $(wildcard lib/*.java) $(wildcard proj/*.java) $(wildcard test/*.java)

.PHONY = all clean

all: $(JAVA_FILES)
	@echo 'Making all...'
	@$(JAVAC) $(FLAGS) $?

clean:
	rm -f $(JAVA_FILES:.java=.class)
	rm -f *~ lib/*~ proj/*~
