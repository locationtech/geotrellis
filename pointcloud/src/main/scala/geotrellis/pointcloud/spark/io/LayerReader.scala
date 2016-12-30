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

package geotrellis.pointcloud.spark.io

import io.pdal._

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._
import org.apache.spark.rdd._
import spray.json._

import scala.reflect._

trait LayerReader[ID] {
  def defaultNumPartitions: Int

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,   
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, numPartitions: Int): RDD[(K, PointCloud)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID): RDD[(K, PointCloud)] with Metadata[M] =
    read(id, defaultNumPartitions)

  def reader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ]: Reader[ID, RDD[(K, PointCloud)] with Metadata[M]] =
    new Reader[ID, RDD[(K, PointCloud)] with Metadata[M]] {
      def read(id: ID): RDD[(K, PointCloud)] with Metadata[M] =
        LayerReader.this.read[K, M](id)
    }
}
