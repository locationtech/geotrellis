# geotrelis.hbase.test

HBase provides HBase mock instances to run tests though this way is
expensive by machine resources, and by dependencies.  It is possible to run
tests just on any HBase instance, by default it uses min resources settings
in a standalone mode, and provides standalone ZooKeeper.  However, it may be
required to increase ZooKeeper connections timeouts and limits [config
example](/.travis/hbase/hbase-site.xml). There is an [example
script](/scripts/hbaseTestDB.sh) to start HBase in a Docker container. HBase
is  extremely sensitive to host names, that's why there can be problems in
starting HBase in a Docker container, similar to problems accessing HBase
from a separate machine.

### Mac OS X / Windows users

Docker is not supported by Mac OS X / Windows natively, it is possible to
use [Docker Beta](https://beta.docker.com/), [Docker
Machine](https://docs.docker.com/machine/) or smth else.  Experiments with
it are appreciated, though the easiest way to run HBase on OS X machine, is
just to
[download](http://apache-mirror.rbc.ru/pub/apache/hbase/1.2.2/hbase-1.2.2-bin.tar.gz)
HBase dist and to launch it in a standalone mode.
