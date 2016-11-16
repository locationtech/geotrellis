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

import geotrellis.proj4.{CRS, LatLng}
import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.vector.Extent

import it.geosolutions.imageio.utilities.ImageIOUtilities
import org.geotools.coverage.Category
import org.geotools.coverage.GridSampleDimension
import org.geotools.coverage.grid._
import org.geotools.geometry.Envelope2D
import org.geotools.referencing.{CRS => GeoToolsCRS}
import org.geotools.resources.coverage.CoverageUtilities
import org.geotools.util.NumberRange
import org.opengis.coverage.SampleDimensionType
import org.opengis.referencing.crs.CoordinateReferenceSystem
import spire.syntax.cfor._

import java.awt.Color
import java.awt.image.{Raster => AwtRaster, _}
import scala.collection.JavaConverters._

/**
  * Houses methods that aide the translate between GridCoverage2D and
  * related GeoTools types, and GeoTrellis types.
  */
object GridCoverage2DConverters {

  /**
    * Create an authority factory which generates CRSs with Longitude
    * guranteed to the be first coordinate.
    */
  val authorityFactory = GeoToolsCRS.getAuthorityFactory(true)

  /**
    * Given a GridCoverage2D and an index, this function optionally
    * produces the unique NODATA value.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    */
  def getNoData(gridCoverage: GridCoverage2D): Option[Double] = {
    val noDataContainer = CoverageUtilities.getNoDataProperty(gridCoverage)
    if(noDataContainer == null) None
    else Some(noDataContainer.getAsSingleValue)
  }

  /**
    * Given a GridCoverage2D and an index, this function return the
    * Geotrellis CellType that best approximates that of the given
    * layer.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    * @param  bandIndex       The index in gridCoverage2D to expose as the sole band of this tile
    */
  def getCellType(gridCoverage: GridCoverage2D): CellType = {
    val noDataValue = getNoData(gridCoverage)
    val renderedImage = gridCoverage.getRenderedImage
    val buffer = renderedImage.getData.getDataBuffer
    val dataType = buffer.getDataType
    val sampleType = gridCoverage.getSampleDimension(0).getSampleDimensionType

    (noDataValue, dataType, sampleType) match {

      // Bit

      case (_, DataBuffer.TYPE_BYTE, SampleDimensionType.UNSIGNED_1BIT) =>
        BitCellType

      // Unsigned Byte

      case (Some(nd), DataBuffer.TYPE_BYTE, SampleDimensionType.UNSIGNED_8BITS) if (nd.toInt > 0 && nd <= 255) =>
        UByteUserDefinedNoDataCellType(nd.toByte)
      case (Some(nd), DataBuffer.TYPE_BYTE, SampleDimensionType.UNSIGNED_8BITS) if nd.toByte == ubyteNODATA =>
        UByteConstantNoDataCellType
      case (_, DataBuffer.TYPE_BYTE, SampleDimensionType.UNSIGNED_8BITS) =>
        UByteCellType

      // Signed Byte

      case (Some(nd), DataBuffer.TYPE_BYTE, _) if (nd.toInt > Byte.MinValue.toInt && nd <= Byte.MaxValue.toInt) =>
        ByteUserDefinedNoDataCellType(nd.toByte)
      case (Some(nd), DataBuffer.TYPE_BYTE, _) if (nd.toByte == byteNODATA) =>
        ByteConstantNoDataCellType
      case (_, DataBuffer.TYPE_BYTE, _) =>
        ByteCellType

      // Unsigned Short

      case (Some(nd), DataBuffer.TYPE_USHORT, _) if (nd.toInt > 0 && nd <= 65535) =>
        UShortUserDefinedNoDataCellType(nd.toShort)
      case (Some(nd), DataBuffer.TYPE_USHORT, _) if (nd.toShort == ushortNODATA) =>
        UShortConstantNoDataCellType
      case (_, DataBuffer.TYPE_USHORT, _) =>
        UShortCellType

      // Signed Short

      case (Some(nd), DataBuffer.TYPE_SHORT, _) if (nd > Short.MinValue.toDouble && nd <= Short.MaxValue.toDouble) =>
        ShortUserDefinedNoDataCellType(nd.toShort)
      case (Some(nd), DataBuffer.TYPE_SHORT, _) if (nd.toShort == shortNODATA) =>
        ShortConstantNoDataCellType
      case (_, DataBuffer.TYPE_SHORT, _) =>
        ShortCellType


      // Unsigned Int32

      case (Some(nd), DataBuffer.TYPE_INT, SampleDimensionType.UNSIGNED_32BITS) if (nd.toLong > 0L && nd.toLong <= 4294967295L) =>
        FloatUserDefinedNoDataCellType(nd.toFloat)
      case (Some(nd), DataBuffer.TYPE_INT, SampleDimensionType.UNSIGNED_32BITS) if (nd.toLong == 0L) =>
        FloatConstantNoDataCellType
      case (None, DataBuffer.TYPE_INT, SampleDimensionType.UNSIGNED_32BITS) =>
        FloatCellType

      // Signed Int32

      case (Some(nd), DataBuffer.TYPE_INT, _) if (nd.toInt > Int.MinValue && nd.toInt <= Int.MaxValue) =>
        IntUserDefinedNoDataCellType(nd.toInt)
      case (Some(nd), DataBuffer.TYPE_INT, _) if (nd.toInt == NODATA) =>
        IntConstantNoDataCellType
      case (_, DataBuffer.TYPE_INT, _) =>
        IntCellType

      // Float

      case (Some(nd), DataBuffer.TYPE_FLOAT, _) if (isData(nd) & Float.MinValue.toDouble <= nd & Float.MaxValue.toDouble >= nd) =>
        FloatUserDefinedNoDataCellType(nd.toFloat)
      case (Some(nd), DataBuffer.TYPE_FLOAT, _) =>
        FloatConstantNoDataCellType
      case (None, DataBuffer.TYPE_FLOAT, _) =>
        FloatCellType

      // Double

      case (Some(nd), DataBuffer.TYPE_DOUBLE, _) if isData(nd) =>
        DoubleUserDefinedNoDataCellType(nd)
      case (Some(nd), DataBuffer.TYPE_DOUBLE, _) =>
        DoubleConstantNoDataCellType
      case (None, DataBuffer.TYPE_DOUBLE, _) =>
        DoubleCellType

      case _ =>
        throw new Exception(s"Unable to convert GridCoverage2D: Unsupported CellType (NoData=${noDataValue}  TypeEnum=${dataType}  SampleDimensionType=${sampleType}")
    }
  }

  /**
    * This function extracts a Geotrellis Extent from the extent
    * information stored in the given GridCoverage2D.
    *
    * @param  gridCoverage  The GeoTools GridCoverage2D object
    */
  def getExtent(gridCoverage: GridCoverage2D): Extent = {
    val envelope = gridCoverage.getEnvelope
    val Array(xmin, ymin) = envelope.getUpperCorner.getCoordinate
    val Array(xmax, ymax) = envelope.getLowerCorner.getCoordinate

    Extent(math.min(xmin, xmax), math.min(ymin, ymax), math.max(xmin, xmax), math.max(ymin, ymax))
  }

  /**
    * This function extracts a Geotrellis CRS from the CRS information
    * stored in the given GridCoverage2D.
    *
    * @param  gridCoverage  The GeoTools GridCoverage2D object
    */
  def getCrs(gridCoverage: GridCoverage2D): Option[CRS] = {
    val systems = gridCoverage.getCoordinateReferenceSystem2D
    val identifiers =
      gridCoverage
        .getCoordinateReferenceSystem2D
        .getIdentifiers()
        .asScala

    if(identifiers.isEmpty) {
      None
    } else {
      val crs = identifiers.head
      Some(CRS.fromName(s"${crs.getCodeSpace}:${crs.getCode}"))
    }
  }

  /**
    * A function to produce a GeoTools Envelope2D from a Geotrellis
    * Extent and CRS.
    *
    * @param  extent  The Geotrellis Extent
    * @param  crs     The CRS of the raster
    *
    * @return         A GeoTools Envelope2D
    */
  def getEnvelope2D(extent: Extent): Envelope2D = {
    val Extent(xmin, ymin, xmax, ymax) = extent

    new Envelope2D(null, xmin, ymin, (xmax - xmin), (ymax - ymin))
  }

  def getGeotoolsCRS(crs: CRS): CoordinateReferenceSystem = {
    crs.epsgCode match {
      case Some(code) =>
        authorityFactory.createCoordinateReferenceSystem(s"EPSG:${code}")
      case _ =>
        null
    }
  }

  /**
    * A function to produce a GeoTools Envelope2D from a Geotrellis
    * Extent and CRS. If the CRS cannot be converted, a null GeoTools
    * CRS is used.
    *
    * @param  extent  The Geotrellis Extent
    * @param  crs     The CRS of the raster
    *
    * @return         A GeoTools Envelope2D
    */
  def getEnvelope2D(extent: Extent, crs: CRS): Envelope2D = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    val geoToolsCRS = getGeotoolsCRS(crs)

    new Envelope2D(geoToolsCRS, xmin, ymin, (xmax - xmin), (ymax - ymin))
  }

  def getValueRange(cellType: CellType): (Double, Double) =
    cellType match {
      case BitCellType => (0.0, 1.0)
      case _: ByteCells => (Byte.MinValue.toDouble, Byte.MaxValue.toDouble)
      case _: UByteCells => (0.0, (1 <<  8).toDouble)
      case _: ShortCells => (Short.MinValue.toDouble, Short.MaxValue.toDouble)
      case _: UShortCells => (0.0, (1 <<  16).toDouble)
      case _: IntCells => (Int.MinValue.toDouble, Int.MaxValue.toDouble)
      case _: FloatCells => (Float.MinValue.toDouble, Float.MaxValue.toDouble)
      case _: DoubleCells => (Double.MinValue, Double.MaxValue)
    }

  def convertToMultibandTile(gridCoverage: GridCoverage2D): MultibandTile = {
    val numBands = gridCoverage.getRenderedImage.getSampleModel.getNumBands
    val tiles = (0 until numBands).map({ i => convertToTile(gridCoverage, i) })
    ArrayMultibandTile(tiles)
  }

  def convertToTile(gridCoverage: GridCoverage2D, bandIndex: Int): Tile = {
    val renderedImage = gridCoverage.getRenderedImage
    val buffer = renderedImage.getData.getDataBuffer
    val sampleModel = renderedImage.getSampleModel
    val rows: Int = renderedImage.getHeight
    val cols: Int = renderedImage.getWidth
    val cellType: CellType = getCellType(gridCoverage)

    def default =
      cellType match {
        case ct: DoubleCells =>
          val data = Array.ofDim[Double](cols * rows)
          sampleModel.getSamples(0, 0, cols, rows, bandIndex, data, buffer)
          DoubleArrayTile(data, cols, rows).convert(cellType)
        case ct: FloatCells =>
          val data = Array.ofDim[Float](cols * rows)
          sampleModel.getSamples(0, 0, cols, rows, bandIndex, data, buffer)
          FloatArrayTile(data, cols, rows).convert(cellType)
        case _ =>
          val data = Array.ofDim[Int](cols * rows)
          sampleModel.getSamples(0, 0, cols, rows, bandIndex, data, buffer)
          IntArrayTile(data, cols, rows).convert(cellType)
      }

    buffer match {
      case byteBuffer: DataBufferByte =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = byteBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: ByteCells =>
                PixelInterleaveBandArrayTile(ByteArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case ct: UByteCells =>
                PixelInterleaveBandArrayTile(UByteArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(UByteArrayTile(data, innerCols, rows, UByteCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = byteBuffer.getData(bandIndex)
            cellType match {
              case ct: ByteCells =>
                ByteArrayTile(data, cols, rows, ct)
              case ct: UByteCells =>
                UByteArrayTile(data, cols, rows, ct)
              case _ =>
                UByteArrayTile(data, cols, rows, UByteCellType).convert(cellType).toArrayTile
            }
          case mp: MultiPixelPackedSampleModel =>
            // Tricky sample model, just do the slow direct thing.
            val result = ArrayTile.alloc(cellType, cols, rows)
            val values = Array.ofDim[Int](1)
            cfor(0)(_ < cols, _ + 1) { col =>
              cfor(0)(_ < rows, _ + 1) { row =>
                gridCoverage.evaluate(new GridCoordinates2D(col, row), values)
                result.set(col, row, values(0))
              }
            }
            result
          case x =>
            default
        }
      case ushortBuffer: DataBufferUShort =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = ushortBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: UShortCells =>
                PixelInterleaveBandArrayTile(UShortArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(UShortArrayTile(data, innerCols, rows, UShortCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = ushortBuffer.getData(bandIndex)
            cellType match {
              case ct: UShortCells =>
                UShortArrayTile(data, cols, rows, ct)
              case _ =>
                UShortArrayTile(data, cols, rows, UShortCellType).convert(cellType).toArrayTile
            }

          case _ =>
            default
        }
      case shortBuffer: DataBufferShort =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = shortBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: ShortCells =>
                PixelInterleaveBandArrayTile(ShortArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(ShortArrayTile(data, innerCols, rows, ShortCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = shortBuffer.getData(bandIndex)
            cellType match {
              case ct: ShortCells =>
                ShortArrayTile(data, cols, rows, ct)
              case _ =>
                ShortArrayTile(data, cols, rows, ShortCellType).convert(cellType).toArrayTile
            }

          case _ =>
            default
        }
      case intBuffer: DataBufferInt =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = intBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: IntCells =>
                PixelInterleaveBandArrayTile(IntArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case ct: FloatCells =>
                // Deal with unsigned ints
                val floatData = data.map { z => (z & 0xFFFFFFFFL).toFloat }
                PixelInterleaveBandArrayTile(FloatArrayTile(floatData, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(IntArrayTile(data, innerCols, rows, IntCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = intBuffer.getData(bandIndex)
            cellType match {
              case ct: IntCells =>
                IntArrayTile(data, cols, rows, ct)
              case ct: FloatCells =>
                // Deal with unsigned ints
                val floatData = data.map { z => (z & 0xFFFFFFFFL).toFloat }
                FloatArrayTile(floatData, cols, rows, ct)
              case _ =>
                IntArrayTile(data, cols, rows, IntCellType).convert(cellType).toArrayTile
            }

          case _ =>
            default
        }
      case floatBuffer: DataBufferFloat =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = floatBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: FloatCells =>
                PixelInterleaveBandArrayTile(FloatArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(FloatArrayTile(data, innerCols, rows, FloatCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = floatBuffer.getData(bandIndex)
            cellType match {
              case ct: FloatCells =>
                FloatArrayTile(data, cols, rows, ct)
              case _ =>
                FloatArrayTile(data, cols, rows, FloatCellType).convert(cellType).toArrayTile
            }

          case _ =>
            default
        }
      case doubleBuffer: DataBufferDouble =>
        sampleModel match {
          case _: PixelInterleavedSampleModel =>
            val data = doubleBuffer.getData()
            val numBands = sampleModel.getNumBands
            val innerCols = cols * numBands
            cellType match {
              case ct: DoubleCells =>
                PixelInterleaveBandArrayTile(DoubleArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(DoubleArrayTile(data, innerCols, rows, DoubleCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = doubleBuffer.getData(bandIndex)
            cellType match {
              case ct: DoubleCells =>
                DoubleArrayTile(data, cols, rows, ct)
              case _ =>
                DoubleArrayTile(data, cols, rows, DoubleCellType).convert(cellType).toArrayTile
            }

          case _ =>
            default
        }
    }
  }

  def convertToGridCoverage2D(raster: Raster[Tile]): GridCoverage2D =
    convertToGridCoverage2D(Raster(ArrayMultibandTile(Array(raster.tile)), raster.extent))

  def convertToGridCoverage2D(raster: Raster[MultibandTile])(implicit d: DummyImplicit): GridCoverage2D =
    convertToGridCoverage2D(raster.tile, raster.extent, None)

  def convertToGridCoverage2D(raster: Raster[Tile], crs: CRS): GridCoverage2D =
    convertToGridCoverage2D(ArrayMultibandTile(Array(raster.tile)), raster.extent, Some(crs))

  def convertToGridCoverage2D(raster: Raster[MultibandTile], crs: CRS)(implicit d: DummyImplicit): GridCoverage2D =
    convertToGridCoverage2D(raster.tile, raster.extent, Some(crs))

  def convertToGridCoverage2D(raster: ProjectedRaster[Tile]): GridCoverage2D =
    convertToGridCoverage2D(ArrayMultibandTile(Array(raster.tile)), raster.extent, Some(raster.crs))

  def convertToGridCoverage2D(raster: ProjectedRaster[MultibandTile])(implicit d: DummyImplicit): GridCoverage2D =
    convertToGridCoverage2D(raster.tile, raster.extent, Some(raster.crs))

  def convertToGridCoverage2D(tile: MultibandTile, extent: Extent, crs: Option[CRS]): GridCoverage2D = {
    val bandCount = tile.bandCount
    val cellType = tile.cellType

    val (dataBuffer, dataType, noDataValue, sampleModel) =
      cellType match {
        case BitCellType =>
          // If it's only one band, we can represent this using a MultiPixelPackedSampleModel.
          // Otherwise, there is no way to correctly represent this in the GridCoverage2D model,
          // so just upcast to byte.
          if(tile.bandCount == 1) {
            val banks = Array.ofDim[Array[Byte]](1)
            val bankSize = tile.cols * tile.rows / 8

            cfor(0)(_ < bandCount, _ + 1) { b =>
              val band = tile.band(b)
              val paddedCols = ((band.cols + 7) / 8) * 8
              val bitArrayTile = BitArrayTile.empty(paddedCols, band.rows)
              band.foreach { (col, row, z) =>
                bitArrayTile.set(col, row, z)
              }
              val array = bitArrayTile.array
              val invertedBytes = array.clone
              // Need to invert the bytes, due to endian-ness
              cfor(0)(_ < invertedBytes.length, _ + 1) { i => invertedBytes(i) = invertByte(array(i)) }
              banks(b) = invertedBytes
            }

            val dataBuffer = new DataBufferByte(banks, bankSize)
            val dataType = DataBuffer.TYPE_BYTE
            val scanlineStride = (tile.cols + 7) / 8
            val lastbit = (tile.rows - 1) * scanlineStride * 8 + (tile.cols-1)

            val sampleModel =
              new MultiPixelPackedSampleModel(dataType, tile.cols, tile.rows, 1)

            (dataBuffer, dataType, None, sampleModel)

          } else {
            val banks = Array.ofDim[Array[Byte]](bandCount)
            val bankSize = tile.cols * tile.rows

            cfor(0)(_ < bandCount, _ + 1) { b =>
              val bandValues = Array.ofDim[Byte](bankSize)
              val band = tile.band(b)
              val cols = band.cols
              band.foreach { (col, row, z) =>
                bandValues(row * cols + col) = z.toByte
              }

              banks(b) = bandValues
            }

            val dataBuffer = new DataBufferByte(banks, bankSize)
            val dataType = DataBuffer.TYPE_BYTE
            val sampleModel =
              new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)

            (dataBuffer, dataType, None, sampleModel)

          }
        case byteCellType: ByteCells =>
          val banks = Array.ofDim[Array[Byte]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataByte =
            byteCellType match {
              case ByteCellType => None
              case ByteConstantNoDataCellType => Some(byteNODATA)
              case ByteUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: ByteArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                val cols = band.cols
                val bandValues = Array.ofDim[Byte](bankSize)
                noDataByte match {
                  case Some(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toByte
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case None =>
                    band.foreach { (col, row, z) =>
                      bandValues(row * cols + col) = z.toByte
                    }
                }

                banks(b) = bandValues
            }
          }

          val dataBuffer = new DataBufferByte(banks, bankSize)
          val dataType = DataBuffer.TYPE_BYTE
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataByte.map { z => z.toDouble }

          (dataBuffer, dataType, noData, sampleModel)

        case ubyteCellType: UByteCells =>
          val banks = Array.ofDim[Array[Byte]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataByte =
            ubyteCellType match {
              case UByteCellType => None
              case UByteConstantNoDataCellType => Some(ubyteNODATA)
              case UByteUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: ByteArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                val cols = band.cols
                val bandValues = Array.ofDim[Byte](bankSize)
                noDataByte match {
                  case Some(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toByte
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case None =>
                    band.foreach { (col, row, z) =>
                      bandValues(row * cols + col) = z.toByte
                    }
                }

                banks(b) = bandValues
            }
          }

          val dataBuffer = new DataBufferByte(banks, bankSize)
          val dataType = DataBuffer.TYPE_BYTE
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataByte.map { z => (z & 0xFF).toDouble }

          (dataBuffer, dataType, noData, sampleModel)

        case shortCellType: ShortCells =>
          val banks = Array.ofDim[Array[Short]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataShort =
            shortCellType match {
              case ShortCellType => None
              case ShortConstantNoDataCellType => Some(shortNODATA)
              case ShortUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: ShortArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                val cols = band.cols
                val bandValues = Array.ofDim[Short](bankSize)
                noDataShort match {
                  case Some(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toShort
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case None =>
                    band.foreach { (col, row, z) =>
                      bandValues(row * cols + col) = z.toShort
                    }
                }

                banks(b) = bandValues
            }
          }

          val dataBuffer = new DataBufferShort(banks, bankSize)
          val dataType = DataBuffer.TYPE_SHORT
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataShort.map { z => z.toDouble }

          (dataBuffer, dataType, noData, sampleModel)
        case ushortCellType: UShortCells =>
          val banks = Array.ofDim[Array[Short]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataUShort =
            ushortCellType match {
              case UShortCellType => None
              case UShortConstantNoDataCellType => Some(ushortNODATA)
              case UShortUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: UShortArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                val cols = band.cols
                val bandValues = Array.ofDim[Short](bankSize)
                noDataUShort match {
                  case Some(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toShort
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case None =>
                    band.foreach { (col, row, z) =>
                      bandValues(row * cols + col) = z.toShort
                    }
                }

                banks(b) = bandValues
            }
          }

          val dataBuffer = new DataBufferUShort(banks, bankSize)
          val dataType = DataBuffer.TYPE_USHORT
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataUShort.map { z => (z & 0xFFFF).toDouble }

          (dataBuffer, dataType, noData, sampleModel)

        case intCellType: IntCells =>
          val banks = Array.ofDim[Array[Int]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataInt =
            intCellType match {
              case IntCellType => None
              case IntConstantNoDataCellType => Some(NODATA)
              case IntUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: IntArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                noDataInt match {
                  case Some(nd) if nd != NODATA =>
                    val cols = band.cols
                    val bandValues = Array.ofDim[Int](bankSize)

                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }

                    banks(b) = bandValues
                  case _ =>
                    banks(b) = band.toArray
                }
            }
          }

          val dataBuffer = new DataBufferInt(banks, bankSize)
          val dataType = DataBuffer.TYPE_INT
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataInt.map { z => z.toDouble }

          (dataBuffer, dataType, noData, sampleModel)

        case floatCellType: FloatCells =>
          val banks = Array.ofDim[Array[Float]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataFloat =
            floatCellType match {
              case FloatCellType => None
              case FloatConstantNoDataCellType => Some(Float.NaN)
              case FloatUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: FloatArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                val cols = band.cols
                val bandValues = Array.ofDim[Float](bankSize)
                noDataFloat match {
                  case Some(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toFloat
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case None =>
                    band.foreach { (col, row, z) =>
                      bandValues(row * cols + col) = z.toFloat
                    }
                }

                banks(b) = bandValues
            }
          }

          val dataBuffer = new DataBufferFloat(banks, bankSize)
          val dataType = DataBuffer.TYPE_FLOAT
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)
          val noData =
            noDataFloat.map { z => z.toDouble }

          (dataBuffer, dataType, noData, sampleModel)

        case doubleCellType: DoubleCells =>
          val banks = Array.ofDim[Array[Double]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataDouble =
            doubleCellType match {
              case DoubleCellType => None
              case DoubleConstantNoDataCellType => Some(Double.NaN)
              case DoubleUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            tile.band(b) match {
              case arrayTile: DoubleArrayTile =>
                banks(b) = arrayTile.array
              case band =>
                noDataDouble match {
                  case Some(nd) if nd != NODATA =>
                    val cols = band.cols
                    val bandValues = Array.ofDim[Double](bankSize)

                    band.foreachDouble { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }

                    banks(b) = bandValues
                  case _ =>
                    banks(b) = band.toArrayDouble
                }
            }
          }

          val dataBuffer = new DataBufferDouble(banks, bankSize)
          val dataType = DataBuffer.TYPE_DOUBLE
          val sampleModel =
            new BandedSampleModel(dataType, tile.cols, tile.rows, tile.bandCount)

          (dataBuffer, dataType, noDataDouble, sampleModel)

      }

    // Sample dimensions
    val gridSampleDimensions = {
      val categories: Array[Category] =
        noDataValue match {
          case Some(nd) =>
            Array(
              if(java.lang.Double.isNaN(nd)) {
                new Category(CoverageUtilities.NODATA, Color.black, Double.NaN)
              } else {
                new Category(CoverageUtilities.NODATA, Array[Color](Color.black), NumberRange.create(nd, nd))
              }
            )
          case None =>
            Array()
        }

      val bands = Array.ofDim[GridSampleDimension](bandCount)
      cfor(0)(_ < bandCount, _ + 1) { b =>
        val (minValue, maxValue) = getValueRange(cellType)
        val sampleDimension =
          new GridSampleDimension(
            s"Band $b", // title
            null, // dimension type
            null, // color interpretation
            null, //color palette
            null, // categories
            noDataValue.map(nd => Array(nd)).getOrElse(null), // nodata values
            minValue, // minimum value
            maxValue, // maximum value
            1, // scale
            0, // offset
            null // unit
          )

        bands(b) = sampleDimension
      }

      bands
    }
    val writableRaster =
      AwtRaster.createWritableRaster(sampleModel, dataBuffer, new java.awt.Point(0, 0))

    val colorModel = ImageIOUtilities.createColorModel(sampleModel)
    val image = new BufferedImage(colorModel, writableRaster, false, null)
    val envelope =
      crs match {
        case Some(c) => getEnvelope2D(extent, c)
        case None => getEnvelope2D(extent)
      }
    val factory = new GridCoverageFactory

    val properties = new java.util.HashMap[String,Double]()

    // If we have a NoData value, we want to set it into the properties.
    noDataValue.foreach { nd =>
      properties.put("GC_NODATA", nd)
    }

    factory.create(
      "",
      image,
      envelope,
      gridSampleDimensions,
      null, // sources
      properties
    )
  }
}
