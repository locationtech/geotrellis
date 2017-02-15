Vector Data Backends
********************

GeoTrellis supports two well-known distributed vector-feature stores:
`GeoMesa <http://www.geomesa.org/>`__ and
`GeoWave <https://github.com/ngageoint/geowave>`__. A question that
often arises in the vector processing world is: "Which should I use?" At
first glance, it can be hard to tell the difference, apart from "one is
Java and the other is Scala". The real answer is, of course, "it
depends".

In the fall of 2016, our team was tasked with an official comparison of
the two. It was our goal to increase awareness of their respective
strengths and weaknesses, so that both teams can focus on their
strengths during development, and the public can make an easier choice.
We analysed a number of angles, including:

-  Feature set
-  Performance
-  Ease of use
-  Project maturity

The full report should be made public in Q1/Q2 of 2017.

While developing applications directly with these projects is quite a
different experience, in terms of our GeoTrellis interfaces for each
project (as a vector data backend), they support essentially the same
feature set (GeoWave optionally supports reading/writing Raster layers).

Keep in mind that as of 2016 October 25, both of these GeoTrellis
modules are still experimental.

GeoMesa
=======

.. code:: scala

    import geotrellis.spark._
    import geotrellis.spark.io._
    import geotrellis.spark.io.geomesa._

    val instance: GeoMesaInstance(
      tableName = ...,
      instanceName = ...,
      zookeepers = ...,
      users = ...,
      password = ...,
      useMock = ...
    )

    val reader = new GeoMesaFeatureReader(instance)
    val writer = new GeoMesaFeatureWriter(instance)

    val id: LayerId = ...
    val query: Query = ... /* GeoMesa query type */

    val spatialFeatureType: SimpleFeatureType = ... /* from geomesa - see their docs */

    /* for some generic D, following GeoTrellis `Feature[G, D]` */
    val res: RDD[SimpleFeature] = reader.read[Point, D](
      id,
      spatialFeatureType,
      query
    )

GeoWave
=======

.. code:: scala

    import geotrellis.spark._
    import geotrellis.spark.io._
    import geotrellis.spark.io.geowave._

    val res: RDD[Feature[G, Map[String, Object]]] = GeoWaveFeatureRDDReader.read(
      zookeepers = ...,
      accumuloInstanceName = ...,
      accumuloInstanceUser = ...,
      accumuloInstancePass = ...,
      gwNamespace = ...,
      simpleFeatureType = ... /* from geowave */
    )
