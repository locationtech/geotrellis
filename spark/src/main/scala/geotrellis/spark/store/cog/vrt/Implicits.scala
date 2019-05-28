/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.cog.vrt

import geotrellis.layers.cog.vrt.VRT
import geotrellis.layers.cog.vrt.VRT._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.util.CollectionAccumulator

import scala.collection.JavaConverters._


object Implicits extends Implicits

trait Implicits {
  implicit class withVRTMethods(val self: VRT) extends MethodExtensions[VRT] {
    /** Creates a copy of a VRT object from a Spark Accumulator */
    def fromAccumulator(acc: CollectionAccumulator[IndexedSimpleSource]): VRT =
      self.fromSimpleSources(acc.value.asScala.toList.sortBy(_._1).map(_._2))
  }
}
