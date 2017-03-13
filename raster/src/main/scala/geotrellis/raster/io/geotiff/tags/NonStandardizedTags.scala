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

package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

import scala.collection.immutable.HashMap

@Lenses("_")
case class NonStandardizedTags(
  asciisMap: HashMap[Int, String] = HashMap[Int, String](),
  longsMap: HashMap[Int, Array[Long]] = HashMap[Int, Array[Long]](),
  fractionalsMap: HashMap[Int, Array[(Long, Long)]] = HashMap[Int, Array[(Long, Long)]](),
  undefinedMap: HashMap[Int, Array[Byte]] = HashMap[Int, Array[Byte]](),
  doublesMap: HashMap[Int, Array[Double]] = HashMap[Int, Array[Double]]()
)
