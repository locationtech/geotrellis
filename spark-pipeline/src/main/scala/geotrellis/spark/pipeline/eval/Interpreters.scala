package geotrellis.spark.pipeline.eval

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.eval.ast._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object Interpreters {
  object MultibandSpatial {
    def evalWrite(ast: Node[RDD[(ProjectedExtent, MultibandTile)]])
                 (implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = ast match {
      case multiband.spatial.HadoopRead(arg) => arg.eval
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransform(ast: Node[RDD[(ProjectedExtent, MultibandTile)] => MultibandTileLayerRDD[SpatialKey]])
                     (implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] = ast match {
      case multiband.spatial.TileToLayout(node: Node[RDD[(ProjectedExtent, MultibandTile)]], arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuplePerTile(ast: Node[RDD[(ProjectedExtent, MultibandTile)] => RDD[(ProjectedExtent, MultibandTile)]])
                                 (implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = ast match {
      case multiband.spatial.PerTileReproject(node, arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuple(ast: Node[RDD[(ProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpatialKey])])
                          (implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpatialKey]) = ast match {
      case multiband.spatial.TileToLayoutWithZoom(node, arg) =>
        arg.eval(evalTransformTuplePerTile(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTupleBuffered(ast: Node[MultibandTileLayerRDD[SpatialKey] => (Int, MultibandTileLayerRDD[SpatialKey])])
                                  (implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpatialKey]) = ast match {
      case multiband.spatial.BufferedReproject(node, arg) => arg.eval(evalTransform(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def eval(ast: Node[(Int, MultibandTileLayerRDD[SpatialKey])])(implicit sc: SparkContext) = ast match {
      case multiband.spatial.HadoopWrite(
        node: Node[MultibandTileLayerRDD[SpatialKey] => (Int, MultibandTileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))
      case multiband.spatial.FileWrite(
        node: Node[MultibandTileLayerRDD[SpatialKey] => (Int, MultibandTileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))

      case multiband.spatial.HadoopWritePerTile(
        node: Node[RDD[(ProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case multiband.spatial.FileWritePerTile(
        node: Node[RDD[(ProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }
  }

  object MultibandTemporal {
    def evalWrite(ast: Node[RDD[(TemporalProjectedExtent, MultibandTile)]])
                 (implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = ast match {
      case multiband.temporal.HadoopRead(arg) => arg.eval
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransform(ast: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => MultibandTileLayerRDD[SpaceTimeKey]])
                     (implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] = ast match {
      case multiband.temporal.TileToLayout(node: Node[RDD[(TemporalProjectedExtent, MultibandTile)]], arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuplePerTile(ast: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => RDD[(TemporalProjectedExtent, MultibandTile)]])
                                 (implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = ast match {
      case multiband.temporal.PerTileReproject(node, arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuple(ast: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpaceTimeKey])])
                          (implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpaceTimeKey]) = ast match {
      case multiband.temporal.TileToLayoutWithZoom(node, arg) =>
        arg.eval(evalTransformTuplePerTile(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTupleBuffered(ast: Node[MultibandTileLayerRDD[SpaceTimeKey] => (Int, MultibandTileLayerRDD[SpaceTimeKey])])
                                  (implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpaceTimeKey]) = ast match {
      case multiband.temporal.BufferedReproject(node, arg) => arg.eval(evalTransform(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def eval(ast: Node[(Int, MultibandTileLayerRDD[SpaceTimeKey])])(implicit sc: SparkContext) = ast match {
      case multiband.temporal.HadoopWrite(
        node: Node[MultibandTileLayerRDD[SpaceTimeKey] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))
      case multiband.temporal.FileWrite(
        node: Node[MultibandTileLayerRDD[SpaceTimeKey] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))

      case multiband.temporal.HadoopWritePerTile(
        node: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case multiband.temporal.FileWritePerTile(
        node: Node[RDD[(TemporalProjectedExtent, MultibandTile)] => (Int, MultibandTileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }
  }

  object SinglebandSpatial {
    def evalWrite(ast: Node[RDD[(ProjectedExtent, Tile)]])
                 (implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = ast match {
      case singleband.spatial.HadoopRead(arg) => arg.eval
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransform(ast: Node[RDD[(ProjectedExtent, Tile)] => TileLayerRDD[SpatialKey]])
                     (implicit sc: SparkContext): TileLayerRDD[SpatialKey] = ast match {
      case singleband.spatial.TileToLayout(node: Node[RDD[(ProjectedExtent, Tile)]], arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuplePerTile(ast: Node[RDD[(ProjectedExtent, Tile)] => RDD[(ProjectedExtent, Tile)]])
                                 (implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = ast match {
      case singleband.spatial.PerTileReproject(node, arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuple(ast: Node[RDD[(ProjectedExtent, Tile)] => (Int, TileLayerRDD[SpatialKey])])
                          (implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = ast match {
      case singleband.spatial.TileToLayoutWithZoom(node, arg) =>
        arg.eval(evalTransformTuplePerTile(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTupleBuffered(ast: Node[TileLayerRDD[SpatialKey] => (Int, TileLayerRDD[SpatialKey])])
                                  (implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = ast match {
      case singleband.spatial.BufferedReproject(node, arg) => arg.eval(evalTransform(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def eval(ast: Node[(Int, TileLayerRDD[SpatialKey])])(implicit sc: SparkContext) = ast match {
      case singleband.spatial.HadoopWrite(
        node: Node[TileLayerRDD[SpatialKey] => (Int, TileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))
      case singleband.spatial.FileWrite(
        node: Node[TileLayerRDD[SpatialKey] => (Int, TileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))

      case singleband.spatial.HadoopWritePerTile(
        node: Node[RDD[(ProjectedExtent, Tile)] => (Int, TileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case singleband.spatial.FileWritePerTile(
        node: Node[RDD[(ProjectedExtent, Tile)] => (Int, TileLayerRDD[SpatialKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }
  }

  object SinglebandTemporal {
    def evalWrite(ast: Node[RDD[(TemporalProjectedExtent, Tile)]])
                 (implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = ast match {
      case singleband.temporal.HadoopRead(arg) => arg.eval
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransform(ast: Node[RDD[(TemporalProjectedExtent, Tile)] => TileLayerRDD[SpaceTimeKey]])
                     (implicit sc: SparkContext): TileLayerRDD[SpaceTimeKey] = ast match {
      case singleband.temporal.TileToLayout(node: Node[RDD[(TemporalProjectedExtent, Tile)]], arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuplePerTile(ast: Node[RDD[(TemporalProjectedExtent, Tile)] => RDD[(TemporalProjectedExtent, Tile)]])
                                 (implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = ast match {
      case singleband.temporal.PerTileReproject(node, arg) =>
        arg.eval(evalWrite(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTuple(ast: Node[RDD[(TemporalProjectedExtent, Tile)] => (Int, TileLayerRDD[SpaceTimeKey])])
                          (implicit sc: SparkContext): (Int, TileLayerRDD[SpaceTimeKey]) = ast match {
      case singleband.temporal.TileToLayoutWithZoom(node, arg) =>
        arg.eval(evalTransformTuplePerTile(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def evalTransformTupleBuffered(ast: Node[TileLayerRDD[SpaceTimeKey] => (Int, TileLayerRDD[SpaceTimeKey])])
                                  (implicit sc: SparkContext): (Int, TileLayerRDD[SpaceTimeKey]) = ast match {
      case singleband.temporal.BufferedReproject(node, arg) => arg.eval(evalTransform(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }

    def eval(ast: Node[(Int, TileLayerRDD[SpaceTimeKey])])(implicit sc: SparkContext) = ast match {
      case singleband.temporal.HadoopWrite(
        node: Node[TileLayerRDD[SpaceTimeKey] => (Int, TileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))
      case singleband.temporal.FileWrite(
        node: Node[TileLayerRDD[SpaceTimeKey] => (Int, TileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTupleBuffered(node))

      case singleband.temporal.HadoopWritePerTile(
        node: Node[RDD[(TemporalProjectedExtent, Tile)] => (Int, TileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case singleband.temporal.FileWritePerTile(
        node: Node[RDD[(TemporalProjectedExtent, Tile)] => (Int, TileLayerRDD[SpaceTimeKey])],
        arg
      ) => arg.eval(evalTransformTuple(node))
      case node =>
        throw new UnsupportedOperationException(s"Unsupported node $node")
    }
  }
}
