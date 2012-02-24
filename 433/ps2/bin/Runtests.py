import subprocess
import time



def startClient(numThreads, testLength):
  tag = "%s.%s.%s.%d" % (server, config, numThreads, testLength)
  SHTTPcommand = "java syu.SHTTPTestClient -server localhost -port 4444 -parallel %d -files requests.txt -T %d -tag %s" % (numThreads, testLength, tag)
  # shttparr = SHTTPcommand.split(" ");
  #print shttparr
  print SHTTPcommand
  # return subprocess.Popen(shttparr, shell='/usr/bin/bash')
  return subprocess.Popen(SHTTPcommand, shell=True)



def startServer(server, config):
  servercommand = "java syu.%(server)s -config %(configfile)s" %  {'server': server, 'configfile': config}
  sarr = servercommand.split(" ")
  # print sarr
  print servercommand
  return subprocess.Popen(servercommand, shell=True)
  #sproc = 

parallels = [1,2,3,4,5,7,9,11,15,20,25,30,40,50,60,80,100][:5]
servers = ["SequentialServer", "ThreadPerRequestServer", "ThreadPoolCompetingServer", "ThreadPoolPollingServer", "ThreadPoolWaitNotifyServer", "AsyncServer"][:]
configs = ["15threads", "50threads"][:]
testLengths = [40]

# parallels = [1,2,3,4,5,7,9,11,15,20,25,30,40,50,60,80,100][:1]
# servers = ["SequentialServer", "ThreadPerRequestServer", "ThreadPoolCompetingServer", "ThreadPoolPollingServer", "ThreadPoolWaitNotifyServer", "AsyncServer"][:1]
# configs = ["15threads", "50threads"][:1]
testLengths = [3]

for server in servers:
  for config in configs:
    if "Pool" not in server and config == "50threads":
      continue
    sproc = startServer(server, config)
    for numThreads in parallels:
      for testLength in testLengths:
        shttpproc = startClient(numThreads, testLength)
        shttpproc.wait()
    print "killing sproc! \n\n\n" 
    sproc.kill()
    time.sleep(1)

        
        
        

    





"""

proc = subprocess.Popen(["sleep", "60"]) 
print 'poll=', proc.poll()
time.sleep(3)
subprocess.call(["kill", "-9", "%d" % proc.pid])
proc.wait()
print 'poll=', proc.poll()
"""
