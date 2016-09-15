package geotrellis.spark.streaming

import geotrellis.spark.Metadata

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Duration, StreamingContext, Time}
import org.apache.spark.streaming.dstream.DStream

object ContextDStream {
  def apply[K, V, M](stream: DStream[(K, V)], metadata: M): DStream[(K, V)] with Metadata[M] =
    new ContextDStream(stream, metadata)(stream.context)

  implicit def tupleToContextDStream[K, V, M](tup: (DStream[(K, V)], M)): ContextDStream[K, V, M] =
    new ContextDStream(tup._1, tup._2)(tup._1.context)
}

class ContextDStream[K, V, M](val stream: DStream[(K, V)], val metadata: M)(implicit ssc: StreamingContext) extends DStream[(K, V)](ssc) with Metadata[M] {
  override def slideDuration: Duration = stream.slideDuration

  override def dependencies: List[DStream[_]] = stream.dependencies

  override def compute(validTime: Time): Option[RDD[(K, V)]] = stream.compute(validTime)
}