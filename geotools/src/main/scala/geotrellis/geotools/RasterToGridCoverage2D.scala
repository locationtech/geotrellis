/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.geotools

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.coverage.GridSampleDimension
import org.geotools.gce.geotiff._
import org.geotools.geometry.Envelope2D
import org.geotools.referencing.{CRS => GeoToolsCRS}
import org.geotools.referencing.operation.transform._
import org.opengis.coverage.SampleDimensionType
import org.osgeo.proj4j._

import java.awt.color.ColorSpace
import java.awt.image.{Raster => AwtRaster, _}
import java.awt.Transparency
import javax.media.jai.{RasterFactory, TiledImage}
import scala.collection.mutable


trait RasterToGridCoverage2D {

  /**
    * Given a Geotrellis [[MultibandTile]], produce a Java
    * [[WritableRaster]] containing the same data.
    *
    * At present, this function does not support Float- or
    * Double-typed Geotrellis MultibandTiles because creating a banked
    * [[Raster]] of Doubles is not allowed.  This simple expedient of
    * creating an interleaved raster does not work because the data
    * must be interleaved at least at the row level.
    *
    * @param  tile  The Geotrellis MultibandTile
    * @return       A Java WritableRaster
    */
  def writableRaster(tile: MultibandTile): WritableRaster = {
    val n = tile.bandCount
    val buffer = tile.cellType match {
      case _: ByteCells | _: UByteCells =>
        new DataBufferByte(tile.bands.map(_.toBytes).toArray, tile.cols * tile.rows)
      case _: FloatCells | _: DoubleCells => {
        throw new Exception("Float and Double MultibandTiles not supported")
      }
      case _ =>
        new DataBufferInt(tile.bands.map(_.toArray).toArray, tile.cols * tile.rows)
    }

    AwtRaster.createBandedRaster(
      buffer, // banked buffer
      tile.cols, tile.rows, tile.cols, // width, height, scanline stride
      (0 until n).toArray, // band indices
      Array.fill(n)(0), // band offsets
      null // location
    )
  }

  /**
    * Given a Geotrellis [[Tile]], produce a Java [[BufferedImage]]
    * containing the same data.
    *
    *    - Signed and unsigned Tiles are converted to 8bpp grayscale
    *      images.
    *    - Signed and unsigned short Tiles are converted to 64bpp RGB
    *      images with all three samples equal (essentially
    *      grayscale).
    *    - Int Tiles are converted to 32bpp RGBa images.
    *    - Float and double tiles are converted to 64bpp grayscale
    *      images.
    *
    * @param  tile  The Geotrellis Tile
    * @return       A Java BufferedImage
    */
  def bufferedImage(tile: Tile): BufferedImage = {
    tile.cellType match {

      // (|Unsigned )Byte
      case _: ByteCells | _: UByteCells => {
        val buffer = new DataBufferByte(tile.toBytes, tile.cols * tile.rows)
        val raster = AwtRaster.createInterleavedRaster(
          buffer, // buffer
          tile.cols, tile.rows, tile.cols, 1, // width, height, line stride, pixel stride
          Array(0), // offsets
          null // location
        )
        val colorModel = new ComponentColorModel(
          ColorSpace.getInstance(ColorSpace.CS_GRAY), // color space
          false, false, // has alpha?, is alpha premultiplied?
          Transparency.OPAQUE, // transparency
          DataBuffer.TYPE_BYTE // transfer type
        )

        new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
      }

      // (|Unsigned )Short
      case _: ShortCells | _: UShortCells => {
        val sampleModel = new SinglePixelPackedSampleModel(
          DataBuffer.TYPE_INT,
          tile.cols, tile.rows,
          Array(0x0000ffff, 0x0000ffff, 0x0000ffff)
        )
        val buffer = new DataBufferInt(tile.toArray, tile.cols * tile.rows)
        val raster = AwtRaster.createWritableRaster(sampleModel, buffer, null)
        val colorModel = new DirectColorModel(
          ColorSpace.getInstance(ColorSpace.CS_sRGB), // color space
          32, 0x0000ffff, 0x0000ffff, 0x0000ffff, 0, // bits, masks
          false, // is alpha premultiplied?
          DataBuffer.TYPE_INT // transfer type
        )

        new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
      }

      // Integer
      case _: IntCells => {
        val raster = AwtRaster.createPackedRaster(
          new DataBufferInt(tile.toArray, tile.cols * tile.rows),
          tile.cols, tile.rows, tile.cols,
          Array(0xff000000, 0x00ff0000, 0x0000ff00, 0x000000ff),
          null
        )
        val colorModel = new DirectColorModel(
          ColorSpace.getInstance(ColorSpace.CS_sRGB), // color space
          32, 0xff000000, 0x00ff0000, 0x0000ff00, 0x000000ff, // bits, masks
          false, // is alpha premultiplied?
          DataBuffer.TYPE_INT // transfer type
        )

        new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
      }

      // (Float|Double)
      case _: FloatCells | _: DoubleCells => {
        val sampleModel = new ComponentSampleModel(
          DataBuffer.TYPE_DOUBLE,
          tile.cols, tile.rows, 1, tile.cols,
          Array(0)
        )
        val buffer = new DataBufferDouble(tile.toArrayDouble, tile.cols * tile.rows)
        val raster = AwtRaster.createWritableRaster(sampleModel, buffer, null)
        val colorModel = new ComponentColorModel(
          ColorSpace.getInstance(ColorSpace.CS_GRAY), // color space
          false, false, // has alpha?, is alpha premultiplied?
          Transparency.OPAQUE, // transparency
          DataBuffer.TYPE_DOUBLE // transfer type
        )

        new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
      }

      // Other
      case _ => throw new Exception("Unknown CellType")
    }
  }

  /**
    * A function to produce a GeoTools [[Envelope2D]] from a
    * Geotrellis raster and CRS.
    *
    * @param  raster  The Geotrellis raster
    * @param  crs     The CRS of the raster
    * @return         A GeoTools Envelope2D
    */
  def envelope2D[T <: CellGrid](raster: Raster[T], crs: Option[CRS]): Envelope2D = {
    val Raster(tile, Extent(xmin, ymin, xmax, ymax)) = raster
    val geoToolsCRS = crs match {
      case Some(crs) =>
        GeoToolsCRS.decode(s"EPSG:${crs.epsgCode.get}")
      case None =>
        GeoToolsCRS.decode("EPSG:404000")
    }
    new Envelope2D(geoToolsCRS, xmin, ymin, (xmax - xmin), (ymax - ymin))
  }

  /**
    * A function to extract the NODATA value from a Geotrellis
    * [[CellType]] and return it as an optional double.
    *
    * @param  cellType  The CellType
    * @return           The NODATA value, if there is one
    */
  def noData(cellType: CellType): Option[Double] = {
    cellType match {
      case ByteUserDefinedNoDataCellType(nd) =>
        if (nd > 0) Option(nd)
        else throw new Exception("NODATA value must be positive") // bug in Java?
      case UByteUserDefinedNoDataCellType(nd) => Option(nd)
      case ShortUserDefinedNoDataCellType(nd) => Option(nd)
      case UShortUserDefinedNoDataCellType(nd) => Option(nd)
      case IntUserDefinedNoDataCellType(nd) => Option(nd)
      case FloatUserDefinedNoDataCellType(nd) => Option(nd)
      case DoubleUserDefinedNoDataCellType(nd) => Option(nd)
      case ByteConstantNoDataCellType => Option(byteNODATA)
      case UByteConstantNoDataCellType => Option(ubyteNODATA)
      case ShortConstantNoDataCellType => Option(shortNODATA)
      case UShortConstantNoDataCellType => Option(ushortNODATA)
      case IntConstantNoDataCellType => Option(NODATA)
      case FloatConstantNoDataCellType => Option(floatNODATA)
      case DoubleConstantNoDataCellType => Option(doubleNODATA)
      case _ => None
    }
  }

  /**
    * A function to compute the [[SampleDimensionType]] associated
    * with a particular [[CellType]].  This function produces data
    * types that work with the [[WritableRaster]] (respectively
    * [[BufferedImage]]) produced by 'writableRaster' (respectively
    * 'bufferedImage').
    *
    * @param  cellType  The Geotrellis CellType
    * @return           The associated SampleDimensionType
    */
  def sampleDimensionType(cellType: CellType): SampleDimensionType = {
    cellType match {
      case _: ByteCells => SampleDimensionType.SIGNED_8BITS
      case _: UByteCells => SampleDimensionType.UNSIGNED_8BITS
      case _: ShortCells => SampleDimensionType.UNSIGNED_32BITS // sic
      case _: UShortCells => SampleDimensionType.UNSIGNED_32BITS // sic
      case _: IntCells => SampleDimensionType.SIGNED_32BITS
      case _: FloatCells => SampleDimensionType.REAL_64BITS // sic
      case _: DoubleCells => SampleDimensionType.REAL_64BITS
      case _ => throw new Exception("Unknown CellType")
    }
  }

  /**
    * A function that returns the minimum and maximum possible values
    * for a given [[CellType]].
    *
    * There appears to be a bug in the Java sample dimension machinery
    * wherein the range of a byte must contain only non-negative
    * numbers, this function assigns a range of [0,2^7) to signed
    * bytes.
    *
    * @param  cellType  The Geotrellis CellType
    * @return           A tuple of doubles, the min and max of the range
    */
  def minMax(cellType: CellType): (Double, Double) = {
    cellType match {
      case _: ByteCells => (0, (1<<7)-1) // sic(!)
      case _: UByteCells => (0, (1<<8)-1)
      case _: ShortCells => (0, (1<<16)-1) // sic
      case _: UShortCells => (0, (1<<16)-1)
      case _: IntCells => (Int.MinValue, Int.MaxValue)
      case _: FloatCells => (Float.MinValue, Float.MaxValue)
      case _: DoubleCells => (Double.MinValue, Double.MaxValue)
      case _ => throw new Exception("Unknown CellType")
    }
  }

  /**
    * Produce an array of [[GridSampleDimension]]s that is compatible
    * with the [[BufferedImage]] produced by 'bufferedImage'.  This is
    * necessary to provide the NODATA metadata.
    *
    * @param  cellType  A Geotrellis CellType
    * @return           An array of GeoTools GridSampleDimensions
    */
  def tileSampleDimensions(cellType: CellType): Array[GridSampleDimension] = {
    val dims = cellType match {
      case _: IntCells => 4
      case _: ShortCells | _: UShortCells => 3
      case _ => 1
    }
    sampleDimensions(cellType, dims)
  }

  /**
    * Produce an array of [[GridSampleDimension]]s that is compatible
    * with the [[WritableRaster]] (respectively [[BufferedImage]])
    * produced by 'writableRaster' (respectively 'bufferedImage').
    *
    * @param  cellType  A Geotrellis [[CellType]]
    * @param  dims      The number of dimensions that the final image will have
    * @return           An array of GeoTools GridSampleDimensions
    */
  def sampleDimensions(cellType: CellType, dims: Int): Array[GridSampleDimension] = {
    val description = cellType.toString
    val dimType = sampleDimensionType(cellType)
    val nd = noData(cellType)
    val lohi = minMax(cellType)
    var i = 0

    Array.fill(dims)(new GridSampleDimension(
      s"$description ${i += 1; i}", // title
      dimType, // dimension type
      null, null, // color interpretation, color palette
      null, // categories
      nd.toArray, // nodata values
      lohi._1, lohi._2, // minimum and maximum values
      1, 0, // scale, offset
      null // unit
    ))
  }
}


/**
  * An object housing constructors for converting a Geotrellis
  * Raster[Tile] to GridCoverage2D.
  */
object TileRasterToGridCoverage2D extends RasterToGridCoverage2D {

  /**
    * Given a Geotrellis [[Tile]] [[Raster]] and a [[CRS]], produce a
    * GeoTools [[GridCoverage2D]].  A (hopefully) suitable
    * [[GridSampleDimension]]s will be provided by the
    * 'sampleDimensions' function.
    *
    * @param  raster  The Geotrellis raster
    * @param  crs     The Geotrellis CRS
    * @return         The GeoTools GridCoverage2D
    */
  def apply(raster: Raster[Tile], crs: Option[CRS]): GridCoverage2D = {
    val cellType = raster.tile.cellType
    val bands = cellType match {
      case _: IntCells =>
        Array(
          new GridSampleDimension(
            "Red",
            SampleDimensionType.UNSIGNED_8BITS,
            null, null, null,
            noData(cellType) match {
              case Some(nd) => Array[Double](0xff & (nd.toInt >> 24))
              case _ => Array.empty[Double]
            },
            0, 255, 1, 0, null
          ),
          new GridSampleDimension(
            "Green",
            SampleDimensionType.UNSIGNED_8BITS,
            null, null, null,
            noData(cellType) match {
              case Some(nd) => Array[Double](0xff & (nd.toInt >> 16))
              case _ => Array.empty[Double]
            },
            0, 255, 1, 0, null
          ),
          new GridSampleDimension(
            "Blue",
            SampleDimensionType.UNSIGNED_8BITS,
            null, null, null,
            noData(cellType) match {
              case Some(nd) => Array[Double](0xff & (nd.toInt >> 8))
              case _ => Array.empty[Double]
            },
            0, 255, 1, 0, null
          ),
          new GridSampleDimension(
            "Alpha",
            SampleDimensionType.UNSIGNED_8BITS,
            null, null, null,
            noData(cellType) match {
              case Some(nd) => Array[Double](0xff & nd.toInt)
              case _ => Array.empty[Double]
            },
            0, 255, 1, 0, null
          )
        )
      case _ =>
        tileSampleDimensions(raster.tile.cellType)
    }

    apply(raster, bands, crs)
  }

  /**
    * Given a Geotrellis [[Tile]] [[Raster]], an array of
    * [[GridSampleDimension]] objects, and a Geotrellis [[CRS]],
    * produce a GeoTools [[GridCoverage2D]].
    *
    * @param  raster  The Geotrellis raster
    * @param  bands   An array of GridSampleDimension objects
    * @param  crs     The Geotrellis CRS
    * @return         The GeoTools GridCoverage2D
    */
  def apply(raster: Raster[Tile], bands: Array[GridSampleDimension], crs: Option[CRS]): GridCoverage2D = {
    val name = raster.toString
    val image = bufferedImage(raster.tile)
    val envelope = envelope2D(raster, crs)
    val factory = new GridCoverageFactory

    val properties: java.util.HashMap[String,Double] =
      noData(raster.tile.cellType) match {
        case Some(nd) =>
          val p = new java.util.HashMap[String,Double]()
          p.put("GC_NODATA", nd)
          p
        case None =>
          null
      }

    factory.create(
      name,
      image,
      envelope,
      bands,
      null, // sources
      properties // sources, properties
    )
  }
}

/**
  * An object housing constructors for converting a Geotrellis
  * Raster[MultibandTile] to GridCoverage2D.
  */
object MultibandTileRasterToGridCoverage2D extends RasterToGridCoverage2D {

  /**
    * Given a Geotrellis [[MultibandTile]] [[Raster]] and a [[CRS]],
    * produce a GeoTools [[GridCoverage2D]].  A (hopefully) suitable
    * [[GridSampleDimension]]s will be provided by the
    * 'sampleDimensions' function.
    *
    * @param  raster  The Geotrellis raster
    * @param  crs     The Geotrellis CRS
    * @return         The GeoTools GridCoverage2D
    */
  def apply(raster: Raster[MultibandTile], crs: Option[CRS]): GridCoverage2D = {
    val bands = sampleDimensions(raster.tile.cellType, raster.tile.bandCount)

    apply(raster, bands, crs)
  }

  /**
    * Given a Geotrellis [[MultibandTile]] [[Raster]], an array of
    * [[GridSampleDimension]] objects, and a Geotrellis [[CRS]],
    * produce a GeoTools [[GridCoverage2D]].
    *
    * @param  raster  The Geotrellis raster
    * @param  bands   An array of GridSampleDimension objects
    * @param  crs     The Geotrellis CRS
    * @return         The GeoTools GridCoverage2D
    */
  def apply(raster: Raster[MultibandTile], bands: Array[GridSampleDimension], crs: Option[CRS]): GridCoverage2D = {
    val name = raster.toString
    val writable = writableRaster(raster)
    val model = bands(0).getColorModel(0, bands.length, writable.getDataBuffer.getDataType)
    val image = new BufferedImage(model, writable, false, null)
    val envelope = envelope2D(raster, crs)
    val factory = new GridCoverageFactory

    val properties = new java.util.HashMap[String,Double]()

    noData(raster.tile.cellType).foreach { nd =>
      properties.put("GC_NODATA", nd)
    }

    factory.create(
      name,
      image,
      envelope,
      bands,
      null, // sources
      properties // sources, properties
    )
  }
}
