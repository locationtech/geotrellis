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

package geotrellis.raster.gdal

import geotrellis.raster._
import geotrellis.proj4.CRS
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector.Extent

import cats.syntax.option._

trait Implicits extends Serializable {
  implicit class GDALRasterExtentMethods(val self: RasterExtent) {

    /**
      * This method copies gdalwarp -tap logic:
      *
      * The actual code reference: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L402-L461
      * The actual part with the -tap logic: https://github.com/OSGeo/gdal/blob/v2.3.2/gdal/apps/gdal_rasterize_lib.cpp#L455-L461
      *
      * The initial PR that introduced that feature in GDAL 1.8.0: https://trac.osgeo.org/gdal/attachment/ticket/3772/gdal_tap.patch
      * A discussion thread related to it: https://lists.osgeo.org/pipermail/gdal-dev/2010-October/thread.html#26209
      *
      */
    def alignTargetPixels: RasterExtent = {
      val extent = self.extent
      val cellSize @ CellSize(width, height) = self.cellSize

      RasterExtent(Extent(
        xmin = math.floor(extent.xmin / width) * width,
        ymin = math.floor(extent.ymin / height) * height,
        xmax = math.ceil(extent.xmax / width) * width,
        ymax = math.ceil(extent.ymax / height) * height
      ), cellSize)
    }
  }


  implicit class GDALWarpOptionsMethodExtension(val self: GDALWarpOptions) {
    def reproject(rasterExtent: GridExtent[Long], sourceCRS: CRS, targetCRS: CRS, resampleGrid: ResampleGrid[Long] = IdentityResampleGrid, resampleMethod: ResampleMethod = NearestNeighbor): GDALWarpOptions = {
      val reprojectOptions = ResampleGrid.toReprojectOptions[Long](rasterExtent, resampleGrid, resampleMethod)
      val re = rasterExtent.reproject(sourceCRS, targetCRS, reprojectOptions)

      self.copy(
        cellSize       = re.cellSize.some,
        targetCRS      = targetCRS.some,
        sourceCRS      = sourceCRS.some,
        resampleMethod = reprojectOptions.method.some
      )
    }

    def resample(gridExtent: => GridExtent[Long], resampleGrid: ResampleGrid[Long]): GDALWarpOptions = {
      resampleGrid match {
        case Dimensions(cols, rows) =>
          self.copy(te = gridExtent.extent.some, cellSize = None, dimensions = (cols.toInt, rows.toInt).some)

        case _ =>
          val re = {
            val targetRasterExtent = resampleGrid(gridExtent).toRasterExtent
            if(self.alignTargetPixels) targetRasterExtent.alignTargetPixels else targetRasterExtent
          }

          self.copy(te = re.extent.some, cellSize = re.cellSize.some)
      }
    }

    def convert(targetCellType: TargetCellType, noDataValue: Option[Double], dimensions: Option[(Int, Int)]): GDALWarpOptions = {
      val convertOptions =
        GDALWarpOptions
          .createConvertOptions(targetCellType, noDataValue)
          .map(_.copy(dimensions = self.cellSize.fold(dimensions)(_ => None)))
          .toList

      (convertOptions :+ self).reduce(_ combine _)
    }
  }
}

object Implicits extends Implicits
