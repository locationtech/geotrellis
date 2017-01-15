/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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