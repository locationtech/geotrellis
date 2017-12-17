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
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

import scala.collection.mutable._

class BitGeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: BitCells with NoDataHandling,
  overviews: List[BitGeoTiffTile] = Nil
) extends GeoTiffTile(segmentLayout, compression, overviews) with GeoTiffSegmentLayoutTransform with BitGeoTiffSegmentCollection {

  def withNoData(noDataValue: Option[Double]): BitGeoTiffTile =
    new BitGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue), overviews.map(_.withNoData(noDataValue)))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: BitCells with NoDataHandling =>
        new BitGeoTiffTile(
          segmentBytes,
          decompressor,
          segmentLayout,
          compression,
          dt,
          overviews.map(_.interpretAs(newCellType)).collect { case gt: BitGeoTiffTile => gt }
        )
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
