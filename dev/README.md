# development samples

To run the cluster example with cluster monitoring, get the sigar binaries 
and put them in a dev/sigar directory.

To test the cluster example on a single machine with the existing 
configuration, run at least one instance of the Server on port 2551, with

run 2551
(then select RemoteServer)

You can run other instances on other ports, e.g.
run 2552
(select RemoteServer)

and then run a RemoteClient instance on a different port

run 2553
(select RemoteClient)
