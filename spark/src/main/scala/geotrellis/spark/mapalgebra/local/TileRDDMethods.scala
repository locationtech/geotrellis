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

import scala.reflect.ClassTag
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD

/** Trait that defines a Methods class as an RDD[(K, Tile)] MethodsExtensions
  * having a ClassTag for K. This trait is used in the local mapalgebra case
  * because we have traits that stack onto the eventual implicit Methods class
  * for local operations. This breaks from the usual pattern of
  * marking a Methods trait as an abstract class if it needs context bounds like
  * ClassTag.
  */
private[local] trait TileRDDMethods[K] extends MethodExtensions[RDD[(K, Tile)]] {
  implicit val keyClassTag: ClassTag[K]
}
