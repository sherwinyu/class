#define USE_XOPEN_EXTENDED


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>
// #include "Far.h"
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <errno.h>
#include <libgen.h>
#include <unistd.h>

FILE *arcfile;
char *arcname;
FILE *tmp;

DIR *dp;
struct dirent *entry;
static int MAX_INT = 10000;
int deleteErrors = 0;
struct stat stbuf;

int ROWS  = 0;
int COLS  = 0;

char** map;
int** dists;

void readDimensions()
{
  scanf("%d %d\n", &ROWS, &COLS );
  printf("rows:%d cols:%d\n", ROWS, COLS );
}
void initialize()
{

  map = malloc(ROWS * sizeof(char *));
  dists = malloc(ROWS * sizeof(int *));
  int i, j;
  for (i = 0; i < ROWS; i++)
  {
    map[i] = malloc( COLS *sizeof(char) );
    dists[i] = malloc( COLS *sizeof(int) );
    for (j = 0; j < COLS; j++)
    {
      map[i][j] = '.';
      dists[i][j] = MAX_INT;
      printf("%d.", dists[i][j]);
    }
    printf("\n");
  }

}
void readInput()
{
  int i, j;

  char * tempStr;
  for(i = 0; i < ROWS; i++)
  {
    size_t num_bytes = 0;
    getline(&map[i], &num_bytes, stdin);
  }
}

 
void prepare()
{
  for (i = 0; i< ROWS; i++)
    for (j = 0; j < COLS; j++)
    {
      if (map[i][j] == '#')
        ;
    }
}


void printArrC(char ** arr)
{
}

void printArrI(int ** arr)
{
  int i, j;
  printf("\nprintArr!:\n");
  for(i = 0; i < ROWS; i++)
  {
    for(j = 0; j < COLS; j++)
      printf("%d ", arr[i][j]);
    printf("\n");
  }
}

void printMap()
{
  int i, j;
  printf("\nmap contents:\n");
  for(i = 0; i < ROWS; i++)
  {
    for(j = 0; j < COLS; j++)
      putchar(map[i][j]);
    printf("\n");
  }
  printf("\nmap contents with printf:\n");
  for(i =0; i< ROWS; i++)
    printf("%s", map[i]);
}



int main(int argc, char *argv[])
{
  readDimensions();
  initialize();
  readInput();
  printMap();
  printArrI(dists);
  exit(0);
}
