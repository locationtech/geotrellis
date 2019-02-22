Cassandra backend testing
*************************

Cassandra backend is based on [DataStax Cassandra Java driver](https://github.com/datastax/java-driver).

Cassandra driver not provides Cassandra mock instance to run tests. It is
possible to use
`SCassandra <https://github.com/scassandra/scassandra-server>`__ and
`CassandraUnit <https://github.com/jsevellec/cassandra-unit>`__ to launch an
embedded Cassandra, but this way is expensive by machine resources,
moreover it is not the fastest way to run test suit. A real Cassandra
instance limited by memory (exmaple: in  a `Docker <https://www.docker.com/>`__
container) can be used as a solution, that would be fastest and cheapest way
to launch tests. Before running Cassandra tests, be sure, that a local
(127.0.0.1)  Cassandra instance is available. Script to start local
Cassandra instance using Docker is provided
`here <https://github.com/pomadchin/geotrellis/blob/feature/cassandra-nmr/scripts/cassandraTestDB.sh>`__

One can also use `ccm <https://github.com/riptano/ccm>`__ for running a Cassandra
cluster on localhost for testing purposes.  ``ccm`` helps to manage and join
nodes into a local cluster and can help in stress testing against a more real-world 
cluster environment. 

### Mac OS X / Windows users

Docker is not supported by Mac OS X / Windows natively, it is possible to
use `Docker for Mac / Windows <https://www.docker.com/>`__ or smth else.
In case of a ``Docker Machine``, it is important to forward necessary ports, in our case to forward
Cassandra cql native transport port 9042 on localhost, from docker container
to a localhost,  from oracle vm where docker container is started. It can be
done using the following command:

.. code:: bash

    vboxmanage controlvm dev natpf1 "9042,tcp,,9042,,9042"

After that Cassandra would be available at ``localhost:9042``.
