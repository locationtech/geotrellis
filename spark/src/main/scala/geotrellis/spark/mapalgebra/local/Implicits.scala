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

package geotrellis.spark.mapalgebra.local

import geotrellis.layers.mapalgebra.local.{LocalTileCollectionMethods, LocalTileCollectionSeqMethods}
import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withLocalTileRDDMethods[K](val self: RDD[(K, Tile)])
    (implicit val keyClassTag: ClassTag[K]) extends LocalTileRDDMethods[K]

  implicit class withLocalTileRDDSeqMethods[K](val self: Traversable[RDD[(K, Tile)]])
    (implicit val keyClassTag: ClassTag[K]) extends LocalTileRDDSeqMethods[K]

  implicit class withLocalTileCollectionMethods[K](val self: Seq[(K, Tile)]) extends LocalTileCollectionMethods[K]
  implicit class withLocalTileCollectionSeqMethods[K](val self: Traversable[Seq[(K, Tile)]]) extends LocalTileCollectionSeqMethods[K]
}