package geotrellis.spark.streaming.io

import org.apache.spark.streaming.StreamingContext

package object hadoop {
  implicit class HadoopStreamingContextMethodsWrapper(val ssc: StreamingContext) extends HadoopStreamingContextMethods
}
