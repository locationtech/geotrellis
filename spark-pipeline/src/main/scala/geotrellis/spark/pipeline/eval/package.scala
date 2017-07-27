package geotrellis.spark.pipeline

import geotrellis.spark._
import geotrellis.spark.pipeline.eval.ast.Node
import org.apache.spark.SparkContext

package object eval {
  import Interpreters._

  implicit class withMultibandTemporalEvaluationMethods(ast: Node[(Int, MultibandTileLayerRDD[SpaceTimeKey])]) {
    def eval(implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpaceTimeKey]) = MultibandTemporal.eval(ast)
  }

  implicit class withMultibandSpatialEvaluationMethods(ast: Node[(Int, MultibandTileLayerRDD[SpatialKey])]) {
    def eval(implicit sc: SparkContext): (Int, MultibandTileLayerRDD[SpatialKey]) = MultibandSpatial.eval(ast)
  }

  implicit class withSinglebandTemporalEvaluationMethods(ast: Node[(Int, TileLayerRDD[SpaceTimeKey])]) {
    def eval(implicit sc: SparkContext): (Int, TileLayerRDD[SpaceTimeKey]) = SinglebandTemporal.eval(ast)
  }

  implicit class withSinglebandSpatialEvaluationMethods(ast: Node[(Int, TileLayerRDD[SpatialKey])]) {
    def eval(implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = SinglebandSpatial.eval(ast)
  }
}
