Extract-Transform-Load (ETL)
============================

This brief tutorial describes how to use GeoTrellis'
`Extract-Transform-Load <https://en.wikipedia.org/wiki/Extract,_transform,_load>`__
("ETL") functionality to create a GeoTrellis catalog. We will accomplish
this in four steps:

1. we will build the ETL assembly from code in the GeoTrellis source
   tree,
2. we will compose JSON configuration files describing the input and
   output data,
3. we will perform the ingest, creating a GeoTrellis catalog, and
4. we will exercise the ingested data using a simple project.

It is assumed throughout this tutorial that `Spark 2.0.0 or
greater <http://spark.apache.org/downloads.html>`__ is installed, that
the `GDAL command line tools <http://www.gdal.org/>`__ are installed,
and that the GeoTrellis source tree has been locally cloned.

Local ETL
---------

Build the ETL Assembly
^^^^^^^^^^^^^^^^^^^^^^

Navigate into the GeoTrellis source tree, build the assembly, and copy
it to the ``/tmp`` directory:

.. code:: console

    cd geotrellis
    ./sbt "project spark-etl" assembly
    cp spark-etl/target/scala-2.11/geotrellis-spark-etl-assembly-1.0.0.jar /tmp

Although in this tutorial we have chosen to build this assembly directly
from the GeoTrellis source tree, in some applications it may be
desirable to create a class in one's own code base that uses or derives
from ``geotrellis.spark.etl.SinglebandIngest`` or
``geotrellis.spark.etl.MultibandIngest``, and use that custom class as
the entry-point. Please see the `Chatta
Demo <https://github.com/geotrellis/geotrellis-chatta-demo/blob/94ae99269236610e66841893990860b7760e3663/geotrellis/src/main/scala/geotrellis/chatta/ChattaIngest.scala>`__
for an example of how to do that.

Compose JSON Configuration Files
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The configuration files that we create in this section are intended for
use with a single multiband GeoTiff image. Three JSON files are
required: one describing the input data, one describing the output data,
and one describing the backend(s) in which the catalog should be stored.
Please see our `more detailed ETL documentation <../guide/etl.html>`__ for
more information about the configuration files.

We will now create three files in the ``/tmp/json`` directory:
``input.json``, ``output.json``, and ``backend-profiles.json``. (The
respective schemas that those files must obey can be found
`here <https://github.com/geotrellis/geotrellis/blob/master/spark-etl/src/main/resources/input-schema.json>`__,
`here <https://github.com/geotrellis/geotrellis/blob/master/spark-etl/src/main/resources/output-schema.json>`__,
and
`here <https://github.com/geotrellis/geotrellis/blob/master/spark-etl/src/main/resources/backend-profiles-schema.json>`__.)

Here is ``input.json``:

.. code:: json

    [{
        "format": "multiband-geotiff",
        "name": "example",
        "cache": "NONE",
        "backend": {
            "type": "hadoop",
            "path": "file:///tmp/rasters"
        }
    }]

The value ``multiband-geotiff`` is associated with the ``format`` key.
That is required if you want to access the data as an RDD of
``SpatialKey``-``MultibandTile`` pairs. Making that value ``geotiff``
instead of ``multiband-geotiff`` would result in ``SpatialKey``-``Tile``
pairs. The value ``example`` associated with the key ``name`` gives the
name of the layer(s) that will be created. The ``cache`` key gives the
Spark caching strategy that will be used during the ETL process.
Finally, the value associated with the ``backend`` key specifies where
the data should be read from. In this case, the source data are stored
in the directory ``/tmp/rasters`` on local filesystem and accessed via
Hadoop.

Here is the ``output.json`` file:

.. code:: json

    {
        "backend": {
            "type": "hadoop",
            "path": "file:///tmp/catalog/"
        },
        "reprojectMethod": "buffered",
        "pyramid": true,
        "tileSize": 256,
        "keyIndexMethod": {
            "type": "zorder"
        },
        "resampleMethod": "cubic-spline",
        "layoutScheme": "zoomed",
        "crs": "EPSG:3857"
    }

That file says that the catalog should be created on the local
filesystem in the directory ``/tmp/catalog`` using Hadoop. The source
data is pyramided so that layers of zoom level 0 through 12 are created
in the catalog. The tiles are 256-by-256 pixels in size and are indexed
in according to Z-order. Bicubic resampling (spline rather than
convolutional) is used in the reprojection process, and the CRS
associated with the layers is EPSG 3857 (a.k.a. Web Mercator).

Here is the ``backend-profiles.json`` file:

.. code:: json

    {
        "backend-profiles": []
    }

In this case, we did not need to specify anything since we are using
Hadoop for both input and output. It happens that Hadoop only needs to
know the path to which it should read or write, and we provided that
information in the ``input.json`` and ``output.json`` files. Other
backends such as Cassandra and Accumulo require information to be
provided in the ``backend-profiles.json`` file.

Create the Catalog
^^^^^^^^^^^^^^^^^^

Before performing the ingest, we will first retile the source raster.
This is not strictly required if the source image is small enough
(probably less than 2GB), but is still good practice even if it is not
required.

.. code:: console

    mkdir -p /tmp/rasters
    gdal_retile.py source.tif -of GTiff -co compress=deflate -ps 256 256 -targetDir /tmp/rasters

The result of this command is a collection of smaller GeoTiff tiles in
the directory ``/tmp/rasters``.

Now with all of the files that we need in place
(``/tmp/geotrellis-spark-etl-assembly-1.0.0.jar``, ``/tmp/json/input.json``,
``/tmp/json/output.json``, ``/tmp/json/backend-profiles.json``, and
``/tmp/rasters/*.tif``) we are ready to perform the ingest. That can be
done by typing:

.. code:: console

    rm -rf /tmp/catalog
    $SPARK_HOME/bin/spark-submit \
       --class geotrellis.spark.etl.MultibandIngest \
       --master 'local[*]' \
       --driver-memory 16G \
       /tmp/geotrellis-spark-etl-assembly-1.0.0.jar \
       --input "file:///tmp/json/input.json" \
       --output "file:///tmp/json/output.json" \
       --backend-profiles "file:///tmp/json/backend-profiles.json"

After the ``spark-submit`` command completes, there should be a
directory called ``/tmp/catalog`` which contains the catalog.

Optional: Exercise the Catalog
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Clone or download `this example
code <https://github.com/geotrellis/geotrellis-examples/tree/be8707499bdf0d481396049d42d44492db7ec982>`__
(a zipped version of which can be downloaded from
`here <https://github.com/geotrellis/geotrellis-examples/archive/be8707499bdf0d481396049d42d44492db7ec982.zip>`__).
The example code is a very simple project that shows how to read layers
from an HDFS catalog, perform various computations on them, then dump
them to disk so that they can be inspected.

Once obtained, the code can be built like this:

.. code:: console

    cd EtlTutorial
    ./sbt "project tutorial" assembly
    cp tutorial/target/scala-2.11/tutorial-assembly-0.jar /tmp

The code can be run by typing:

.. code:: console

    mkdir -p /tmp/tif
    $SPARK_HOME/bin/spark-submit \
       --class com.azavea.geotrellis.tutorial.EtlExercise \
       --master 'local[*]' \
       --driver-memory 16G \
       /tmp/tutorial-assembly-0.jar /tmp/catalog example 12

In the block above, ``/tmp/catalog`` is an HDFS URI pointing to the
location of the catalog, ``example`` is the layer name, and ``12`` is
the layer zoom level. After running the code, you should find a number
of images in ``/tmp/tif`` which are GeoTiff renderings of the tiles of
the raw layer, as well as the layer with various transformations applied
to it.

GeoDocker ETL
-------------

The foregoing discussion showed how to ingest data to the local
filesystem, albeit via Hadoop. In this section, we will give a basic
example of how to use the ETL machinery to ingest into HDFS on
GeoDocker. Throughout this section we will assume that the files that
were previously created in the local ``/tmp`` directory (namely
``/tmp/geotrellis-spark-etl-assembly-1.0.0.jar``, ``/tmp/json/input.json``,
``/tmp/json/output.json``, ``/tmp/json/backend-profiles.json``, and
``/tmp/rasters/*.tif``) still exist.

In addition to the dependencies needed to complete the steps given
above, this section assumes that user has a recent version of
``docker-compose`` installed and working.

Edit ``output.json``
^^^^^^^^^^^^^^^^^^^^

Because we are planning to ingest into HDFS and not to the filesystem,
we must modify the ``output.json`` file that we used previously. Edit
``/tmp/json/output.json`` so that it looks like this:

.. code:: json

    {
        "backend": {
            "type": "hadoop",
            "path": "hdfs://hdfs-name/catalog/"
        },
        "reprojectMethod": "buffered",
        "pyramid": true,
        "tileSize": 256,
        "keyIndexMethod": {
            "type": "zorder"
        },
        "resampleMethod": "cubic-spline",
        "layoutScheme": "zoomed",
        "crs": "EPSG:3857"
    }

The only change is the value associated with the ``path`` key; it now
points into HDFS instead of at the local filesystem.

Download ``docker-compose.yml`` File
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

We must now obtain a ``docker-compose.yml`` file. Download `this
file <https://raw.githubusercontent.com/geodocker/geodocker-hdfs/2542b92075fbc750a1b1fb1b9dc47190fc7beb35/docker-compose.yml>`__
and move it to the ``/tmp`` directory. The directory location is
important, because ``docker-compose`` will use that to name the network
and containers that it creates.

Bring Up GeoDocker
^^^^^^^^^^^^^^^^^^

With the ``docker-compose.yml`` file in place, we are now ready to start
our GeoDocker instance:

.. code:: console

    cd /tmp
    docker-compose up

After a period of time, the various Hadoop containers should be up and
working.

Perform the Ingest
^^^^^^^^^^^^^^^^^^

In a different terminal, we will now start another container:

.. code:: console

    docker run -it --rm --net=tmp_default -v $SPARK_HOME:/spark:ro -v /tmp:/tmp openjdk:8-jdk bash

Notice that the network name was derived from the name of the directory
in which the ``docker-compose up`` command was run. The
``--net=tmp_default`` switch connects the just-started container to the
bridge network that the GeoDocker cluster is running on. The
``-v $SPARK_HOME:/spark:ro`` switch mounts our local Spark installation
at ``/spark`` within the container so that we can use it. The
``-v /tmp:/tmp`` switch mounts our host ``/tmp`` directory into the
container so that we can use the data and jar files that are there.

Within the just-started container, we can now perform the ingest:

.. code:: console

    /spark/bin/spark-submit \
       --class geotrellis.spark.etl.MultibandIngest \
       --master 'local[*]' \
       --driver-memory 16G \
       /tmp/geotrellis-spark-etl-assembly-1.0.0.jar \
       --input "file:///tmp/json/input.json" \
       --output "file:///tmp/json/output.json" \
       --backend-profiles "file:///tmp/json/backend-profiles.json"

The only change versus what we did earlier is the location of the
``spark-submit`` binary.

Optional: Exercise the Catalog
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Now, we can exercise the catalog:

.. code:: console

    rm -f /tmp/tif/*.tif
    /spark/bin/spark-submit \
       --class com.azavea.geotrellis.tutorial.EtlExercise \
       --master 'local[*]' \
       --driver-memory 16G \
       /tmp/tutorial-assembly-0.jar /tmp/catalog example 12

The only differences form what we did earlier are the location of the
``spark-submit`` binary and URI specifying the location of the catalog.
