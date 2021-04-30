# GeoTrellis/GeoWave Connector

GeoTrellis/GeoWave connector for storing raster and volumetric data.

- [GeoTrellis/GeoWave Connector](#geotrellisgeowave-connector)
  - [Requirements](#requirements)
  - [Project Inventory](#project-inventory)
  - [Development](#development)
    - [!Important](#important)
    - [Executing Tests](#executing-tests)
## Requirements

- Docker Engine 17.12+
- Docker Compose 1.21+
- OpenJDK 8

## Project Inventory

- `src` - Main project with `GeoTrellisDataAdapter` enabling storing GeoTrellis types with GeoWave
- `benchmark` - Skeleton for microbenchmarks on GeoWave queries
- `docs` - Overview of GeoWave concepts relevant to index and data adapter usage

## Development

### !Important

After merging PRs / fetching changes from master and other branches be sure that you _recreated_
dev env. Any changes introduced into interfaces that are present in the `Persistable Registry`
and have `fromBinary` and `toBinary`methods can cause serialization / deserialization issues
in tests and as a consequence tests would fail with various of unpredictable runtime exceptions.

### Executing Tests

Tests are dependent on Apache Cassandra, Kafka, ZooKeeper, and Graphite with Grafana. First, ensure
these dependencies are running:

```bash
docker-compose up -d cassandra
```

Now, you can execute tests from project root:

```bash
$ ./sbt "project geowave" test
...
[info] All tests passed.
[success] Total time: 72 s, completed Nov 22, 2019 11:48:25 AM
```

When you're done, ensure that the services and networks created by Docker
Compose are torn down:

```bash
docker-compose down
```