package geotrellis.geotools

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.vector.Extent

import it.geosolutions.imageio.utilities.ImageIOUtilities
import org.geotools.coverage.Category
import org.geotools.coverage.GridSampleDimension
import org.geotools.coverage.grid._
import org.geotools.geometry.Envelope2D
import org.geotools.referencing.{CRS => GeoToolsCRS}
import org.geotools.resources.coverage.CoverageUtilities
import org.geotools.util.NumberRange
import org.opengis.referencing.crs.CoordinateReferenceSystem
import spire.syntax.cfor._

import java.awt.Color
import java.awt.image.{Raster => AwtRaster, _}
import scala.collection.JavaConverters._

/**
  * Houses methods that aide the translate
  * between GridCoverage2D and related GeoTools types,
  * and GeoTrellis types.
  */
object GridCoverage2DConverters {
  /**
    * Given a [[GridCoverage2D]] and an index, this function
    * optionally produces the unique NODATA value.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    */
  def getNoData(gridCoverage: GridCoverage2D): Option[Double] = {
    val noDataContainer = CoverageUtilities.getNoDataProperty(gridCoverage)
    if(noDataContainer == null) None
    else Some(noDataContainer.getAsSingleValue)
  }

  /**
    * Given a [[GridCoverage2D]] and an index, this function return
    * the Geotrellis [[CellType]] that best approximates that of the
    * given layer.
    *
    * @param  gridCoverage2D  The GeoTools GridCoverage2D object
    * @param  bandIndex       The index in gridCoverage2D to expose as the sole band of this tile
    */
  def getCellType(gridCoverage: GridCoverage2D): CellType = {
    val noDataValue = getNoData(gridCoverage)
    val renderedImage = gridCoverage.getRenderedImage
    val buffer = renderedImage.getData.getDataBuffer
    val typeEnum = buffer.getDataType

    (noDataValue, typeEnum) match {
      case (None, DataBuffer.TYPE_BYTE) => UByteCellType
      case (None, DataBuffer.TYPE_USHORT) => UShortCellType
      case (None, DataBuffer.TYPE_SHORT) => ShortCellType
      case (None, DataBuffer.TYPE_INT) => IntCellType
      case (None, DataBuffer.TYPE_FLOAT) => FloatCellType
      case (None, DataBuffer.TYPE_DOUBLE) => DoubleCellType
      case (Some(nd), DataBuffer.TYPE_BYTE) =>
        val byte = nd.toByte
        if (byte == ubyteNODATA) UByteConstantNoDataCellType
        else UByteUserDefinedNoDataCellType(byte)
      case (Some(nd), DataBuffer.TYPE_USHORT) =>
        val short = nd.toShort
        if (short == ushortNODATA) UShortConstantNoDataCellType
        else UShortUserDefinedNoDataCellType(short)
      case (Some(nd), DataBuffer.TYPE_SHORT) =>
        val short = nd.toShort
        if (short == shortNODATA) ShortConstantNoDataCellType
        else ShortUserDefinedNoDataCellType(short)
      case (Some(nd), DataBuffer.TYPE_INT) =>
        val int = nd.toInt
        if (int == NODATA) IntConstantNoDataCellType
        else IntUserDefinedNoDataCellType(int)
      case (Some(nd), DataBuffer.TYPE_FLOAT) =>
        val float = nd.toFloat
        if (float == floatNODATA) FloatConstantNoDataCellType
        else FloatUserDefinedNoDataCellType(float)
      case (Some(nd), DataBuffer.TYPE_DOUBLE) =>
        val double = nd.toDouble
        if (double == doubleNODATA) DoubleConstantNoDataCellType
        else DoubleUserDefinedNoDataCellType(double)
      case _ => throw new Exception(s"Unable to convert GridCoverage2D: Unknown or Undefined CellType (NoData=$noDataValue  TypeEnum=${typeEnum}")
    }
  }

  /**
    * This function extracts a Geotrellis [[Extent]] from the extent
    * information stored in the given [[GridCoverage2D]].
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
    * This function extracts a Geotrellis [[CRS]] from the CRS
    * information stored in the given [[GridCoverage2D]].
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
    * A function to produce a GeoTools [[Envelope2D]] from a
    * Geotrellis Extent and CRS.
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

  /**
    * A function to produce a GeoTools [[Envelope2D]] from a
    * Geotrellis Extent and CRS. If the CRS cannot be converted,
    * a null GeoTools CRS is used.
    *
    * @param  extent  The Geotrellis Extent
    * @param  crs     The CRS of the raster
    *
    * @return         A GeoTools Envelope2D
    */
  def getEnvelope2D(extent: Extent, crs: CRS): Envelope2D = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    val geoToolsCRS: CoordinateReferenceSystem =
      crs.epsgCode match {
        case Some(code) =>
          GeoToolsCRS.decode(s"EPSG:${code}")
        case _ =>
          null
      }

    new Envelope2D(geoToolsCRS, xmin, ymin, (xmax - xmin), (ymax - ymin))
  }

  def convertToTile(buffer: DataBuffer, sampleModel: SampleModel, cellType: CellType, cols: Int, rows: Int, bandIndex: Int): Tile = {
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
              case ct: UByteCells =>
                PixelInterleaveBandArrayTile(UByteArrayTile(data, innerCols, rows, ct), numBands, bandIndex)
              case _ =>
                PixelInterleaveBandArrayTile(UByteArrayTile(data, innerCols, rows, UByteCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = byteBuffer.getData(bandIndex)
            cellType match {
              case ct: UByteCells =>
                UByteArrayTile(data, cols, rows, ct)
              case _ =>
                UByteArrayTile(data, cols, rows, UByteCellType).convert(cellType).toArrayTile
            }

          case _ =>
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
              case _ =>
                PixelInterleaveBandArrayTile(IntArrayTile(data, innerCols, rows, IntCellType).convert(cellType).toArrayTile, numBands, bandIndex)
            }

          case _: BandedSampleModel =>
            val data = intBuffer.getData(bandIndex)
            cellType match {
              case ct: IntCells =>
                IntArrayTile(data, cols, rows, ct)
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

    val (dataBuffer, dataType, noDataValue, sampleModel) =
      tile.cellType match {
        case BitCellType =>
          // If it's only one band, we can represent this using a MultiPixelPackedSampleModel.
          // Otherwise, there is no way to correctly represent this in the GridCoverage2D model,
          // so just upcast to byte.
          if(tile.bandCount == 1) {
            val banks = Array.ofDim[Array[Byte]](1)
            val bankSize = tile.cols * tile.rows

            cfor(0)(_ < bandCount, _ + 1) { b =>
              tile.band(b) match {
                case arrayTile: BitArrayTile =>
                  banks(b) = arrayTile.array
                case band =>
                  val bitArrayTile = BitArrayTile.empty(band.cols, band.rows)
                  band.foreach { (col, row, z) =>
                    bitArrayTile.set(col, row, z)
                  }

                  banks(b) = bitArrayTile.array
              }

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
          // DataBuffer does not have a type for signed Bytes, so upcast to a short.
          val banks = Array.ofDim[Array[Short]](bandCount)
          val bankSize = tile.cols * tile.rows

          val noDataByte =
            byteCellType match {
              case ByteCellType => None
              case ByteConstantNoDataCellType => Some(byteNODATA)
              case ByteUserDefinedNoDataCellType(nd) => Some(nd)
            }

          cfor(0)(_ < bandCount, _ + 1) { b =>
            val band = tile.band(b)
            val cols = band.cols
            val bandValues = Array.ofDim[Short](bankSize)
            noDataByte match {
              case Some(nd) if nd != byteNODATA =>
                band.foreach { (col, row, z) =>
                  if(isData(z)) {
                    bandValues(row * cols + col) = z.toShort
                  } else {
                    bandValues(row * cols + col) = nd.toShort
                  }
                }
              case _ =>
                band.foreach { (col, row, z) =>
                  bandValues(row * cols + col) = z.toShort
                }
            }

            banks(b) = bandValues
          }

          val dataBuffer = new DataBufferShort(banks, bankSize)
          val dataType = DataBuffer.TYPE_SHORT
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
                  case Some(nd) if nd != ubyteNODATA =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toByte
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case _ =>
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
                  case Some(nd) if nd != shortNODATA =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toShort
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case _ =>
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
                  case Some(nd) if nd != ushortNODATA =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toShort
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case _ =>
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
                  case Some(nd) if java.lang.Float.isNaN(nd) =>
                    band.foreach { (col, row, z) =>
                      if(isData(z)) {
                        bandValues(row * cols + col) = z.toFloat
                      } else {
                        bandValues(row * cols + col) = nd
                      }
                    }
                  case _ =>
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
        bands(b) = new GridSampleDimension(s"Band $b", categories, null)
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
      null,
      image,
      envelope,
      gridSampleDimensions,
      null, // sources
      properties
    )
  }
}
