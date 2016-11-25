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

package geotrellis.geotools

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.scalatest._

import java.awt.image.DataBuffer
import scala.collection.JavaConverters._
import scala.math.{min, max}


abstract class RasterToGridCoverage2DSpec[T <: CellGrid](implicit ev1: Raster[T] => ToGridCoverage2DMethods, ev2: ProjectedRaster[T] => ToGridCoverage2DMethods)
    extends FunSpec
    with Matchers {

  val tile: T
  val extent: Extent
  val crs: Option[CRS]
  val nd: Option[Double]

  lazy val gridCoverage =
    crs match {
      case Some(c) => ProjectedRaster(Raster(tile, extent), c).toGridCoverage2D
      case None => Raster(tile, extent).toGridCoverage2D
    }

  lazy val raster = Raster(tile, extent)
  lazy val renderedImage = gridCoverage.getRenderedImage
  lazy val buffer = renderedImage.getData.getDataBuffer
  lazy val sampleModel = renderedImage.getSampleModel

  def getWidth: Int = renderedImage.getWidth

  def getHeight: Int = renderedImage.getHeight

  def getBandCount: Int = sampleModel.getNumBands

  def getCrs: Option[CRS] =
    GridCoverage2DConverters.getCrs(gridCoverage)

  def getNodata: Option[Double] =
    GridCoverage2DConverters.getNoData(gridCoverage)

  def getPixel(col: Int, row: Int) = {
    val n = getBandCount

    buffer.getDataType match {
      case DataBuffer.TYPE_FLOAT =>
        sampleModel.getPixel(col, row, Array.ofDim[Double](n), buffer)
      case DataBuffer.TYPE_DOUBLE =>
        sampleModel.getPixel(col, row, Array.ofDim[Double](n), buffer)
      case DataBuffer.TYPE_BYTE =>
        sampleModel.getPixel(col, row, Array.ofDim[Int](n), buffer)
      case DataBuffer.TYPE_USHORT =>
        sampleModel.getPixel(col, row, Array.ofDim[Int](n), buffer)
      case DataBuffer.TYPE_SHORT =>
        sampleModel.getPixel(col, row, Array.ofDim[Int](n), buffer)
      case DataBuffer.TYPE_INT =>
        sampleModel.getPixel(col, row, Array.ofDim[Int](n), buffer)
    }
  }

  describe("The *RasterToGridCoverage2D Objects") {

    it("should produce the correct width") {
      getWidth should be (tile.cols)
    }

    it("should produce the correct height") {
      getHeight should be (tile.rows)
    }

    it("should produce the correct CRS") {
      getCrs should be (crs)
    }

    it("should produce the correct extent") {
      val envelope = gridCoverage.getEnvelope
      val Array(xmin, ymin) = envelope.getUpperCorner.getCoordinate
      val Array(xmax, ymax) = envelope.getLowerCorner.getCoordinate
      val actual = Extent(min(xmin, xmax), min(ymin, ymax), max(xmin, xmax), max(ymin, ymax))

      actual should be (extent)
    }

    it("should produce the correct NODATA") {
      nd match {
        case Some(ndValue) =>
          getNodata match {
            case Some(actualNdValue) =>
              if(ndValue.isNaN) actualNdValue.isNaN should be (true)
              else actualNdValue should be (ndValue)
            case None =>
              getNodata should be (Some(ndValue))
          }
        case None =>
          getNodata should be (None)
      }
    }
  }
}

class RasterToGridCoverage2D_IntTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = IntCellType
  val tile = ArrayTile.empty(cellType, 10, 10)
  val extent = Extent(0.1, 0.2, 1.1, 1.2)
  val crs = Some(ConusAlbers)
  val nd = None
  tile.set(1, 1, 33)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(1, 2)(0) should be (0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (33)
    }
  }
}

class RasterToGridCoverage2D_ConstIntTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = IntConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 10, 10)
  val extent = Extent(0.1, 0.2, 1.1, 1.2)
  val crs = Some(ConusAlbers)
  val nd = Some[Double](NODATA)
}

class RasterToGridCoverage2D_UdIntTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = IntUserDefinedNoDataCellType(42 << 24)
  val tile = ArrayTile.empty(cellType, 10, 10)
  val extent = Extent(0.1, 0.2, 1.1, 1.2)
  val crs = Some(ConusAlbers)
  val nd = Some[Double](42 << 24)
}

class RasterToGridCoverage2D_FloatTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = FloatCellType
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = None
  tile.setDouble(1, 1, 42.0)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0.0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (42.0)
    }
  }
}

class RasterToGridCoverage2D_ConstFloatTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = FloatConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = Some[Double](floatNODATA)
}

class RasterToGridCoverage2D_UdFloatTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = FloatUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_DoubleTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = DoubleCellType
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = None
  tile.setDouble(1, 1, 42.0)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0.0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (42.0)
    }
  }
}

class RasterToGridCoverage2D_ConstDoubleTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = DoubleConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = Some[Double](doubleNODATA)
}

class RasterToGridCoverage2D_UdDoubleTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = DoubleUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 20, 20)
  val extent = Extent(0.1, 0.2, 2.1, 3.2)
  val crs = Some(LatLng)
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_ShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ShortCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = None
  tile.set(1, 1, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (107)
    }
  }

}

class RasterToGridCoverage2D_ConstShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ShortConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](shortNODATA)
}

class RasterToGridCoverage2D_UdShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ShortUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_UShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UShortCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = None
  tile.set(1, 1, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (107)
    }
  }

}

class RasterToGridCoverage2D_ConstUShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UShortConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](ushortNODATA)
}

class RasterToGridCoverage2D_UdUShortTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UShortUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_ByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ByteCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = None
  tile.set(1, 1, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (107)
    }
  }
}

class RasterToGridCoverage2D_ConstByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ByteConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](byteNODATA)
}

class RasterToGridCoverage2D_UdByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = ByteUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_UByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UByteCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = None
  tile.set(1, 1, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0)(0) should be (0)
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1)(0) should be (107)
    }
  }
}

class RasterToGridCoverage2D_ConstUByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UByteConstantNoDataCellType
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](ubyteNODATA)
}

class RasterToGridCoverage2D_UdUByteTileSpec extends RasterToGridCoverage2DSpec[Tile] {
  val cellType = UByteUserDefinedNoDataCellType(42)
  val tile = ArrayTile.empty(cellType, 40, 40)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = None
  val nd = Some[Double](42)
}

class RasterToGridCoverage2D_IntMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = IntCellType
  val bands = Array(
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512)
  )
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = None
  val crs = Some(ConusAlbers)
  bands(0).set(1, 1, 33); bands(1).set(1, 1, 42); bands(2).set(2, 2, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0).toList should be (List(0, 0, 0))
    }

    it("should preserve pixels that have been set") {
      getPixel(1, 1).toList should be (List(33, 42, 0))
    }

    it("should preserve other pixels that have been set") {
      getPixel(2, 2).toList should be (List(0, 0, 107))
    }
  }
}

class RasterToGridCoverage2D_ConstIntMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = IntConstantNoDataCellType
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](NODATA)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UdIntMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = IntUserDefinedNoDataCellType(42)
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](42)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_ByteMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = ByteCellType
  val bands = Array(
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512)
  )
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = Some(LatLng)
  val nd = None
  bands(0).set(1, 1, 33); bands(1).set(1, 1, 42); bands(2).set(2, 2, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0).toList should be (List(0, 0, 0))
    }

    it("should preserve pixels that have been set 1") {
      getPixel(1, 1).toList should be (List(33, 42, 0))
    }

    it("should preserve pixels that have been set 2") {
      getPixel(2, 2).toList should be (List(0, 0, 107))
    }
  }
}

class RasterToGridCoverage2D_UdByteMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = ByteUserDefinedNoDataCellType(42)
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](42)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UByteMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UByteCellType
  val bands = Array(
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512)
  )
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = Some(LatLng)
  val nd = None
  bands(0).set(1, 1, 33); bands(1).set(1, 1, 42); bands(2).set(2, 2, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0).toList should be (List(0, 0, 0))
    }

    it("should preserve pixels that have been set 1") {
      getPixel(1, 1).toList should be (List(33, 42, 0))
    }

    it("should preserve pixels that have been set 2") {
      getPixel(2, 2).toList should be (List(0, 0, 107))
    }
  }
}

class RasterToGridCoverage2D_ConstUByteMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UByteConstantNoDataCellType
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](ubyteNODATA)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UdUByteMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UByteUserDefinedNoDataCellType(42)
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](42)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_ShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = ShortCellType
  val bands = Array(
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512)
  )
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = Some(LatLng)
  val nd = None
  bands(0).set(1, 1, 33); bands(1).set(1, 1, 42); bands(2).set(2, 2, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0).toList should be (List(0, 0, 0))
    }

    it("should preserve pixels that have been set 1") {
      getPixel(1, 1).toList should be (List(33, 42, 0))
    }

    it("should preserve pixels that have been set 2") {
      getPixel(2, 2).toList should be (List(0, 0, 107))
    }
  }
}

class RasterToGridCoverage2D_ConstShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = ShortConstantNoDataCellType
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](shortNODATA)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UdShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = ShortUserDefinedNoDataCellType(42)
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](42)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UShortCellType
  val bands = Array(
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512),
    ArrayTile.empty(cellType, 512, 512)
  )
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val crs = Some(LatLng)
  val nd = None
  bands(0).set(1, 1, 33); bands(1).set(1, 1, 42); bands(2).set(2, 2, 107)

  describe("Copying of Data") {

    it("should leave blank pixels blank") {
      getPixel(0,0).toList should be (List(0, 0, 0))
    }

    it("should preserve pixels that have been set 1") {
      getPixel(1, 1).toList should be (List(33, 42, 0))
    }

    it("should preserve pixels that have been set 2") {
      getPixel(2, 2).toList should be (List(0, 0, 107))
    }
  }
}

class RasterToGridCoverage2D_ConstUShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UShortConstantNoDataCellType
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](ushortNODATA)
  val crs = Some(ConusAlbers)
}

class RasterToGridCoverage2D_UdUShortMultibandSpec extends RasterToGridCoverage2DSpec[MultibandTile] {
  val cellType = UShortUserDefinedNoDataCellType(42)
  val bands = Array(ArrayTile.empty(cellType, 512, 512))
  val tile = ArrayMultibandTile(bands)
  val extent = Extent(0.0, 0.0, 5.0, 5.0)
  val nd = Some[Double](42)
  val crs = Some(ConusAlbers)
}
