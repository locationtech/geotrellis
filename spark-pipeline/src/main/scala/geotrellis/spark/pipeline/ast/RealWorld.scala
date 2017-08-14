package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.PipelineExpr

import io.circe.Json
import org.apache.spark.SparkContext

sealed trait RealWorld

object RealWorld {
  /** RealWorld node has nothing implemented */
  def instance: Node[RealWorld] = new Node[RealWorld] {
    def eval(implicit sc: SparkContext): RealWorld =
      throw new UnsupportedOperationException("get function is not supported by a RealWorld node")
    def arg: PipelineExpr =
      throw new UnsupportedOperationException("arg function is not supported by a RealWorld node")
    def asJson: List[Json] =
      throw new UnsupportedOperationException("asJson function is not supported by a RealWorld node")
  }
}
