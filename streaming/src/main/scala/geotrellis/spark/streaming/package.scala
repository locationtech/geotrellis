package geotrellis.spark

import org.apache.spark.streaming.dstream.DStream

package object streaming {
  implicit class WithStreamingContextWrapper[K, V, M](val stream: DStream[(K, V)] with Metadata[M]) {
    def withContext[K2, V2](f: DStream[(K, V)] => DStream[(K2, V2)]) =
      new ContextDStream(f(stream), stream.metadata)(stream.context)

    def mapContext[M2](f: M => M2) =
      new ContextDStream(stream, f(stream.metadata))(stream.context)
  }
}
