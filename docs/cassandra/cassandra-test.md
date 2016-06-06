# geotrelis.cassandra.test

Cassandra backend is based on [DataStax Cassandra Java driver](https://github.com/datastax/java-driver). 
Cassandra driver not provides Cassandra mock instance to run tests. It is possible to use [Scassandra](https://github.com/scassandra/scassandra-server) 
and [CassandraUnit](https://github.com/jsevellec/cassandra-unit) to launch an embedded Cassandra, but this way is expensive by machine resources, 
moreover it is not the fastest way to run test suit. A real Cassandra instance limited by memory (exmaple: in 
a [Docker](https://www.docker.com/) container) can be used as a solution, that would be fastest and cheapest way to launch tests. Before running Cassandra tests, be sure, that a local (127.0.0.1) 
Cassandra instance is available. Script to start local Cassandra instance using Docker is provided [here](https://github.com/pomadchin/geotrellis/blob/feature/cassandra-nmr/scripts/cassandraTestDB.sh).
