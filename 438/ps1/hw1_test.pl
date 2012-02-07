#!/usr/bin/perl

# pg executable paths
my $pg_ctl = "~/pgsql/bin/pg_ctl";
my $psql = "~/pgsql/bin/psql";

# data path
# $my $pgdata = "~/pgdata"; EDITED -- 2012 02 06 -- change to ~/pg
my $pgdata = "~/pg";

# name of pg database to connect to
my $database_name = "test -p 5555";

# temp logfile
my $logfile = "test_output.log";

# create empty logfile, clearing any old one that exists
system("echo \"\" > $logfile");

# loop through buffer pool sizes
for ($buffers = 16; $buffers <= 128; $buffers = $buffers + 8) {
  print "running query ($buffers buffers)\n";

  # print current buffer pool size to logfile
  system("echo \"BUFFERS=$buffers\" >> $logfile");

  # start postmaster
  system("$pg_ctl start -D $pgdata -l $logfile -o \"-B $buffers -N 8 -o \'-te -fm -fh\'\" > /dev/null");

  # wait 2 seconds before running any queries (otherwise postmaster might not actually be ready)
  sleep 2;

  # execute query1 three times
  foreach (1..3) {
    system("$psql $database_name -o /dev/null -c \"SELECT * FROM raw_r_tuples;\"");
  }

  # insert a marker in the log that following entries relate to q2 not q1
  system("echo \"END_TEST\" >> $logfile");

  # execute query2 three times
  foreach (1..3) {
    system("$psql $database_name -o /dev/null -c \"SELECT * FROM raw_r_tuples r, raw_s_tuples s WHERE r.pkey = s.pkey;\"");
  }

  # stop postmaster (thus clearing buffer pool)
  system("$pg_ctl stop -D $pgdata > /dev/null");

  # marker that test ends
  system("echo \"END_TEST\" >> $logfile");

  # wait 1 second before starting again
  sleep 1;
}

# parse the logfile and pretty-print the results
print "\n\t\t  QUERY 1\t\t  QUERY 2\n";
print "buffers\t\tmin\tmax\t\tmin\tmax\n";
print "-------\t\t---\t---\t\t---\t---";
my @hitrates;
open(LOGFILE, $logfile);
while(<LOGFILE>) {
  chomp;
  if (m/BUFFERS=(\d+)/) {  # start each line with buffer pool size
    print "\n$1";
  } elsif (m/Shared.* = (\d+\.\d+)%/) {  # print each buffer hit rate
    push(@hitrates, $1);
  } elsif (m/END_TEST/ && $#hitrates > 0) {
    # find/print min and max hit rates
    my $min = $hitrates[0];
    my $max = $hitrates[0];
    for (my $i = 0; $i <= $#hitrates; $i++) {
      if ($hitrates[$i] < $min) { $min = $hitrates[$i]; }
      if ($hitrates[$i] > $max) { $max = $hitrates[$i]; }
    }
    print "\t\t$min\t$max";
  }

  # clear hitrates array at the end of each test
  if (m/END_TEST/) { @hitrates = (); }
}

# print a trailing newline
print "\n";

# remove temporary logfile
system("rm $logfile");

