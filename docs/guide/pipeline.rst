The Pipeline Tool (an alternative ETL tool)
===========================================

Pipelines are an idea originally inspired by [PDAL](https://pdal.io/pipeline.html) pipelines.
Pipelines represent a set of instructions: how to read data, transform (process) said data, and
write it. It is possible to do this with other parts of the GeoTrellis API, but the pipeline provides
an alternative which could simplify some common processing tasks and to reduce the amount of code
that is necessary to perform some common operations.

Pipelines are represented as `JSON` objects which each represent discrete
steps (which we will call `Stage Objects`) to be performed.

You can break these `Stage Objects` into three categories: `readers`, `writers`, and `transformations`.

Sample Pipeline Application
---------------------------

.. code:: scala

    import geotrellis.spark._
    import geotrellis.spark.pipeline._
    import geotrellis.spark.pipeline.ast._

    implicit val sc: SparkContext = ???

    // pipeline json example
    val maskJson =
      """
        |[
        |  {
        |    "uri" : "s3://geotrellis-test/daunnc/",
        |    "type" : "singleband.spatial.read.s3"
        |  },
        |  {
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.tile-to-layout"
        |  },
        |  {
        |    "crs" : "EPSG:3857",
        |    "scheme" : {
        |      "crs" : "epsg:3857",
        |      "tileSize" : 256,
        |      "resolutionThreshold" : 0.1
        |    },
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.buffered-reproject"
        |  },
        |  {
        |    "end_zoom" : 0,
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.pyramid"
        |  },
        |  {
        |    "name" : "mask",
        |    "uri" : "s3://geotrellis-test/colingw/pipeline/",
        |    "key_index_method" : {
        |      "type" : "zorder"
        |    },
        |    "scheme" : {
        |      "crs" : "epsg:3857",
        |      "tileSize" : 256,
        |      "resolutionThreshold" : 0.1
        |    },
        |    "type" : "singleband.spatial.write"
        |  }
        |]
      """.stripMargin

      // parse the JSON above
      val list: Option[Node[Stream[(Int, TileLayerRDD[SpatialKey])]]] = maskJson.node

      list match {
        case None => println("Couldn't parse the JSON")
        case Some(node) => {
          // eval evaluates the pipeline
          // the result type of evaluation in this case would ben Stream[(Int, TileLayerRDD[SpatialKey])]
          node.eval.foreach { case (zoom, rdd) =>
            println(s"ZOOM: ${zoom}")
            println(s"COUNT: ${rdd.count}")
          }
        }
      }


To understand what's going on in the above pipeline, read the corresponding `type` field of the each
pipeline step. In our case:

- ``singleband.spatial.read.s3`` -- load tiles into Spark memory as (ProjectedExtent, Tile) tuples
- ``singleband.spatial.transform.tile-to-layout`` -- tile and index data as (SpatialKey, Tile) tuples
- ``singleband.spatial.transform.buffered-reproject`` -- reproject everything into a target CRS
- ``singleband.spatial.transform.pyramid`` -- build a pyramid (i.e. build out layers for different zoom levels)
- ``singleband.spatial.write`` -- write the output of the above operations to storage

The result node type should equal to the final operation type, meaning that it is possible to evaluate a pipeline and
continue working with its results (whose character we can know based on the final operation type)

It is also possible to build pipelines using only the internal scala DSL:

.. code:: scala

    import geotrellis.spark._
    import geotrellis.spark.tiling._
    import geotrellis.spark.pipeline._
    import geotrellis.spark.pipeline.json._
    import geotrellis.spark.pipeline.json.read._
    import geotrellis.spark.pipeline.json.transform._
    import geotrellis.spark.pipeline.json.write._
    import geotrellis.spark.pipeline.ast._
    import geotrellis.spark.pipeline.ast.untyped.ErasedNode

    import org.apache.spark.SparkContext

    implicit val sc: SparkContext = ???

    val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
    val jsonRead = JsonRead("s3://geotrellis-test/", `type` = ReadTypes.SpatialS3Type)
    val jsonTileToLayout = TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType)
    val jsonReproject = Reproject("EPSG:3857", scheme, `type` = TransformTypes.SpatialBufferedReprojectType)
    val jsonPyramid = Pyramid(`type` = TransformTypes.SpatialPyramidType)
    val jsonWrite = JsonWrite("mask", "s3://geotrellis-test/pipeline/", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialType)

    val list: List[PipelineExpr] = jsonRead ~ jsonTileToLayout ~ jsonReproject ~ jsonPyramid ~ jsonWrite

    // typed way, as in the JSON example above
    val typedAst: Node[Stream[(Int, TileLayerRDD[SpatialKey])]] =
      list
        .node[Stream[(Int, TileLayerRDD[SpatialKey])]]
    val result: Stream[(Int, TileLayerRDD[SpatialKey])] = typedAst.eval

Pipeline in user applications
------------------------------

The above sample application can be placed in a new SBT project that has
a dependency on
``"org.locationtech.geotrellis" %% "geotrellis-spark-pipeline" % s"$VERSION"``
in addition to dependency on ``spark-core`` and built into an assembly
with the ``sbt-assembly`` plugin. You should be careful to include an
``assemblyMergeStrategy`` for sbt assembly plugin as it is provided in
`spark-pipeline build file <build.sbt>`__.

Each `Pipeline` config represents a full `Main` and, thus, requires the
creation of separate ``App`` objects (scala more idiomatic Main) per ingest.

Built-in Pipeline assembly  fat jar
-----------------------------------

The example of a user application with a proper `build.sbt` file
is ``spark-pipeline`` project itself.

You may use it by building an assembly jar of ``spark-pipeline`` project as
follows:

.. code:: bash

    cd geotrellis
    ./sbt
    sbt> project spark-pipeline
    sbt> assembly

The assembly jar will be placed in
``geotrellis/spark-pipeline/target/scala-2.11`` directory.


Scala DSL and Pipeline Stage Objects description
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The type of the operation is described in the JSON object `type` field.
Usually such type has the following structure:

``{singleband | multiband}.[spatial | temporal}.{read | write | transform}.{operation name}``

Reader objects
--------------

.. code:: javascript

    {
       "uri" : "{s3| file | hdfs | ...}://...",
       "time_tag" : "TIFFTAG_DATETIME", // optional field
       "time_format" : "yyyy:MM:dd HH:mm:ss", // optional field
       "type" : "{singleband | multiband}.{spatial | temporal}.read.{s3 | hadoop}"
    }

+-----------------------+-------------------+
| Key                   | Value             |
+=======================+===================+
| uri                   | Uri               |
|                       | to the source     |
|                       | imagery           |
+-----------------------+-------------------+
| time_tag              | The name of the   |
|                       | time tag in the   |
|                       | dataset metadata  |
+-----------------------+-------------------+
| type                  | operation type    |
+-----------------------+-------------------+

The structure of all operations is pretty simple, basically here only two types of readers are available:
To read from `S3` or from `Hadoop` supported file systems via Hadoop API.

Writer objects
--------------

.. code:: javascript

    {
       "name" : "layerName",
       "uri" : "{s3| file | hdfs | ...}://...",
       "key_index_method" : {
          "type" : "{zorder | hilbert}",
          "temporal_resolution": 1 // optional, if set - temporal index is used
       },
       "scheme" : {
          "crs" : "epsg:3857",
          "tileSize" : 256,
          "resolutionThreshold" : 0.1
       },
       "type" : "{singleband | multiband}.{spatial | temporal}.write"
    }

+-----------------------+-------------------+
| Key                   | Value             |
+=======================+===================+
| uri                   | Uri               |
|                       | to the source     |
|                       | imagery           |
+-----------------------+-------------------+
| name                  | layer name        |
+-----------------------+-------------------+
| key_index_method      | key index method  |
|                       | to generate index |
|                       | from spatial keys |
+-----------------------+-------------------+
| key_index_method.type | ``zorder``,       |
|                       | ``row-major``,    |
|                       | ``hilbert``       |
+-----------------------+-------------------+
| key_index_method.     | temporal          |
| tmporal_resolution    | resolution in     |
|                       | millis            |
+-----------------------+-------------------+
| scheme                | target layout     |
|                       | scheme            |
+-----------------------+-------------------+
| scheme.crs            | scheme target crs |
+-----------------------+-------------------+
| scheme.tileSize       | layout scheme     |
|                       | tile size         |
+-----------------------+-------------------+
| scheme.               | Resolution for    |
| resolutionThreshold   | user defined      |
|                       | Layout Scheme     |
|                       | (optional field)  |
+-----------------------+-------------------+

The structure of all operations is pretty simple, basically here only two types of readers are available:
To read from `S3` or from `Hadoop` supported file systems via Hadoop API.

Transformation objects
----------------------

Tile To Layout
^^^^^^^^^^^^^^

.. code:: json

    {
       "resample_method" : "nearest-neighbor",
       "type" : "{singleband | multiband}.{spatial | temporal}.transform.tile-to-layout"
    }

Projects `RDD[({ProjectedExtent | TemporalProjectedExtent}, {Tile | MultibandTile})]`
into `RDD[({SpatialKey | SpaceTimeKey}, {Tile | MultibandTile})]`.


+-----------------------+-------------------+
| Key                   | Options           |
+=======================+===================+
| resample_method       | ``nearest-neighbo |
|                       | r``,              |
|                       | ``bilinear``,     |
|                       | ``cubic-convoluti |
|                       | on``,             |
|                       | ``cubic-spline``, |
|                       | ``lanczos``       |
+-----------------------+-------------------+

ReTile To Layout
^^^^^^^^^^^^^^^^

.. code:: json

    {
       "layout_definition": {
          "extent": [0, 0, 1, 1],
          "tileLayout": {
             "layoutCols": 1,
             "layoutRows": 1,
             "tileCols": 1,
             "tileRows": 1
          }
        },
       "resample_method" : "nearest-neighbor",
       "type" : "{singleband | multiband}.{spatial | temporal}.transform.retile-to-layout"
    }

Retiles `RDD[({SpatialKey | SpaceTimeKey}, {Tile | MultibandTile})]` according to some layout definition.

Buffered Reproject
^^^^^^^^^^^^^^^^^^

.. code:: json

    {
       "crs" : "EPSG:3857",
       "scheme" : {
          "crs" : "epsg:3857",
          "tileSize" : 256,
          "resolutionThreshold" : 0.1
       },
       "resample_method" : "nearest-neighbor",
       "type" : "{singleband | multiband}.{spatial | temporal}.transform.buffered-reproject"
    }

Projects `RDD[({SpatialKey | SpaceTimeKey}, {Tile | MultibandTile})]`
into the destination ``CRS`` according to some layout scheme.

+-----------------------+-------------------+
| Key                   | Options           |
+=======================+===================+
| crs                   | scheme target crs |
+-----------------------+-------------------+
| tileSize              | layout scheme     |
|                       | tile size         |
+-----------------------+-------------------+
| resolutionThreshold   | Resolution for    |
|                       | user defined      |
|                       | Layout Scheme     |
|                       | (optional field)  |
+-----------------------+-------------------+
| resample_method       | ``nearest-neighbo |
|                       | r``,              |
|                       | ``bilinear``,     |
|                       | ``cubic-convoluti |
|                       | on``,             |
|                       | ``cubic-spline``, |
|                       | ``lanczos``       |
+-----------------------+-------------------+

Per Tile Reproject
^^^^^^^^^^^^^^^^^^

.. code:: json

    {
       "crs" : "EPSG:3857",
       "scheme" : {
          "crs" : "epsg:3857",
          "tileSize" : 256,
          "resolutionThreshold" : 0.1
       },
       "resample_method" : "nearest-neighbor",
       "type" : "{singleband | multiband}.{spatial | temporal}.transform.per-tile-reproject"
    }

Projects `RDD[({ProjectedExtent | TemporalProjectedExtent}, {Tile | MultibandTile})]`
into the destination ``CRS`` according to some layout scheme.


+-----------------------+-------------------+
| Key                   | Options           |
+=======================+===================+
| scheme                | target layout     |
|                       | scheme            |
+-----------------------+-------------------+
| scheme.crs            | scheme target crs |
+-----------------------+-------------------+
| scheme.tileSize       | layout scheme     |
|                       | tile size         |
+-----------------------+-------------------+
| scheme.               | Resolution for    |
| resolutionThreshold   | user defined      |
|                       | Layout Scheme     |
|                       | (optional field)  |
+-----------------------+-------------------+
| resample_method       | ``nearest-neighbo |
|                       | r``,              |
|                       | ``bilinear``,     |
|                       | ``cubic-convoluti |
|                       | on``,             |
|                       | ``cubic-spline``, |
|                       | ``lanczos``       |
+-----------------------+-------------------+

Pyramid
^^^^^^^

.. code:: json

    {
       "end_zoom" : 0,
       "resample_method" : "nearest-neighbor",
       "type" : "{singleband | multiband}.{spatial | temporal}.transform.pyramid"
    }

Pyramids `RDD[({SpatialKey | SpaceTimeKey}, {Tile | MultibandTile})]` up to `end_zoom` level, the result
type is `Stream[RDD[({SpatialKey | SpaceTimeKey}, {Tile | MultibandTile})]]`.


+-----------------+-------------------------------------------------+
| Key             | Options                                         |
+=================+=================================================+
| Layout Scheme   | Options                                         |
+=================+=================================================+
| end_zoom        | The lowest zoom level to build pyramid down to  |
+-----------------+-------------------------------------------------+
| resample_method | Floating layout scheme in a native projection   |
+-----------------+-------------------------------------------------+

Layout Scheme
^^^^^^^^^^^^^

GeoTrellis is able to tile layers in either ``ZoomedLayoutScheme``,
matching TMS pyramid, or ``FloatingLayoutScheme``, matching the native
resolution of input raster. These alternatives may be selecting by using
the ``layoutScheme`` option.

Note that ``ZoomedLayoutScheme`` needs to know the world extent, which
it gets from the CRS, in order to build the TMS pyramid layout. This
will likely cause resampling of input rasters to match the resolution of
the TMS levels.

On other hand ``FloatingLayoutScheme`` will discover the native
resolution and extent and partition it by given tile size without
resampling.

User-Defined Layout
-------------------

You may bypass the layout scheme logic by providing ``layoutExtent`` and
either a ``tileLayout`` or a ``cellSize`` and ``tileSize`` to fully
define the layout and start the tiling process.  The user may optionally
specify an output ``cellType`` as well (default case uses the input
``cellType``).

Reprojection
------------

``spark-pipeline`` project supports two methods of reprojection: ``buffered``
and ``per-tile``. They provide a trade-off between accuracy and
flexibility.

Buffered reprojection method is able to sample pixels past the tile
boundaries by performing a neighborhood join. This method is the default
and produces the best results. However it requires that all of the
source tiles share the same CRS.

Per tile reproject method can not consider pixels past the individual
tile boundaries, even if they exist elsewhere in the dataset. Any pixels
past the tile boundaries will be as ``NODATA`` when interpolating. This
restriction allows for source tiles to have a different projections per
tile. This is an effective way to unify the projections for instance
when projection from multiple UTM projections to WebMercator.
