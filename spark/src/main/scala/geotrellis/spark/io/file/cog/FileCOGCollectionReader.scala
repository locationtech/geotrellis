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

package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark.io._
import geotrellis.spark.io.cog._

import com.typesafe.config.ConfigFactory
import java.net.URI

class FileCOGCollectionReader[V <: CellGrid](
  implicit val tileMergeMethods: V => TileMergeMethods[V],
           val tilePrototypeMethods: V => TilePrototypeMethods[V],
           val tiffMethods: TiffMethods[V]
) extends COGCollectionReader[V] {
  lazy val defaultThreads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")

  def fullPath(path: String): URI = new URI(s"file://$path")
}
