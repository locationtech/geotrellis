# development samples

To run the cluster example with cluster monitoring, get the sigar binaries 
and put them in a dev/sigar directory.

To test the cluster example on a single machine with the existing 
configuration, run sbt in multiple terminal sessions (or screen session)
with at least one instance of RemoteServer on port 2551.  For example,
you could run:

# in first terminal:
$ ./sbt 
project dev
run-main geotrellis.dev.RemoteServer 2551


# You can run other instances on other ports, e.g.
# in another terminal:
$ ./sbt
project dev
run-main geotrellis.dev.RemoteServer 2552

and then run a RemoteClient instance on a different port

# in different terminal
$ ./sbt
project dev
run-main run 2553
(select RemoteClient)
