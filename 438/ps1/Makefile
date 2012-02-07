CPP=gcc
OPTS=-g -Wall
LIBS=-lresolv -ldl -lm
INCLUDE=-I../postgresql-8.0.3/src/include

all: testclock testlru testmru

%.o: %.c
	$(CPP) $(OPTS) $(INCLUDE) -o $@ -c $<

testclock: freelist.clock.o stubs.o buftest.o
	$(CPP) $(OPTS) $(LIBS) $(INCLUDE) -o testclock freelist.clock.o stubs.o buftest.o

testlru: freelist.lru.o stubs.o buftest.o
	$(CPP) $(OPTS) $(LIBS) $(INCLUDE) -o testlru freelist.lru.o stubs.o buftest.o

testmru: freelist.mru.o stubs.o buftest.o
	$(CPP) $(OPTS) $(LIBS) $(INCLUDE) -o testmru freelist.mru.o stubs.o buftest.o

pgclock: freelist.clock.c
	cp freelist.clock.c ../postgresql-8.0.3/src/backend/storage/buffer/freelist.c
	cd ../postgresql-8.0.3; make install

pginit:
	rm -rf ~/pgdata
	~/pgsql/bin/initdb -D ~/pgdata
clean:
	rm -rf *.o
	rm -rf buftest
	cd ../postgresql-8.0.3; make clean
