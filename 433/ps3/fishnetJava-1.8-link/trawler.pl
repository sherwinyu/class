#!/usr/local/bin/perl

# Simple script to start Trawler

main();

sub main {
    
    $classpath = "out/";
    # $classpath = "lib/";
    
    $fishnetArgs = join " ", @ARGV;

    exec("nice -n 19 java -cp $classpath Trawler $fishnetArgs");
}
