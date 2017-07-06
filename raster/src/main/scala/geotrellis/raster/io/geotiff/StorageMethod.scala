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

package geotrellis.raster.io.geotiff

import geotrellis.raster._

abstract sealed class StorageMethod extends Serializable

case class Tiled(blockCols: Int = 256, blockRows: Int = 256) extends StorageMethod

// Trait used only for implicit conversion of object
private[geotiff] trait TiledStorageMethod

object Tiled extends TiledStorageMethod {
  implicit def objectToStorageMethod(t: TiledStorageMethod): Tiled = Tiled()
}

class Striped(rowsPerStrip: Option[Int]) extends StorageMethod {
  def rowsPerStrip(rows: Int, bandType: BandType): Int =
    rowsPerStrip match {
      case Some(ris) => ris
      case None =>
        // strip height defaults to a value such that one strip is 8K or less.
        val rowSize = rows * bandType.bytesPerSample
        val ris = 8000 / rowSize
        if(ris == 0) 1
        else ris
    }
}

// Trait used only for implicit conversion of object
private[geotiff] trait StripedStorageMethod

object Striped extends StripedStorageMethod {
  def apply(rowsPerStrip: Int): Striped = new Striped(Some(rowsPerStrip))
  def apply(): Striped = new Striped(None)

  implicit def objectToStorageMethod(s: StripedStorageMethod): Striped = Striped()
}
