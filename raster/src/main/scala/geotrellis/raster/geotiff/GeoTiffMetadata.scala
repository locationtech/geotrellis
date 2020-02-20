/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.geotiff

import geotrellis.raster.{RasterMetadata, SourceName}
import geotrellis.proj4.CRS
import geotrellis.raster.{CellType, GridExtent, CellSize}
import geotrellis.raster.io.geotiff.Tags

case class GeoTiffMetadata(
  name: SourceName,
  crs: CRS,
  bandCount: Int,
  cellType: CellType,
  gridExtent: GridExtent[Long],
  resolutions: List[CellSize],
  tags: Tags
) extends RasterMetadata {
  /** Returns the GeoTiff head tags. */
  def attributes: Map[String, String] = tags.headTags
  /** Returns the GeoTiff per band tags. */
  def attributesForBand(band: Int): Map[String, String] = tags.bandTags.lift(band).getOrElse(Map.empty)
}