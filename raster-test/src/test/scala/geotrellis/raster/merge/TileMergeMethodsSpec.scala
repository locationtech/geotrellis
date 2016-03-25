package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest._

class TileMergeMethodsSpec extends FunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers {
  describe("SinglebandTileMergeMethods") {

    it("should merge prototype for each cell type") {
      val cellTypes: Seq[CellType] =
        Seq(
          BitCellType,
          ByteCellType,
          ByteConstantNoDataCellType,
          ByteUserDefinedNoDataCellType(1.toByte),
          UByteCellType,
          UByteConstantNoDataCellType,
          UByteUserDefinedNoDataCellType(1.toByte),
          ShortCellType,
          ShortConstantNoDataCellType,
          ShortUserDefinedNoDataCellType(1.toShort),
          UShortCellType,
          UShortConstantNoDataCellType,
          UShortUserDefinedNoDataCellType(1.toShort),
          IntCellType,
          IntConstantNoDataCellType,
          IntUserDefinedNoDataCellType(1),
          FloatCellType,
          FloatConstantNoDataCellType,
          FloatUserDefinedNoDataCellType(1.0f),
          DoubleCellType,
          DoubleConstantNoDataCellType,
          DoubleUserDefinedNoDataCellType(1.0)
        )

      for(ct <- cellTypes) {
        val arr = Array.ofDim[Double](100).fill(5.0)
        arr(50) = 1.0
        arr(55) = 0.0
        arr(60) = Double.NaN

        val tile =
          DoubleArrayTile(arr, 10, 10, DoubleCellType).convert(ct)

        val proto = tile.prototype(ct, tile.cols, tile.rows)
        val merged = proto merge tile
        withClue(s"Failing on cell type $ct: ") {
          assertEqual(merged, tile)
        }
      }
    }

    it("should merge offset prototype for each cell type") {
      val cellTypes: Seq[CellType] =
        Seq(
          BitCellType,
          ByteCellType,
          ByteConstantNoDataCellType,
          ByteUserDefinedNoDataCellType(1.toByte),
          UByteCellType,
          UByteConstantNoDataCellType,
          UByteUserDefinedNoDataCellType(1.toByte),
          ShortCellType,
          ShortConstantNoDataCellType,
          ShortUserDefinedNoDataCellType(1.toShort),
          UShortCellType,
          UShortConstantNoDataCellType,
          UShortUserDefinedNoDataCellType(1.toShort),
          IntCellType,
          IntConstantNoDataCellType,
          IntUserDefinedNoDataCellType(1),
          FloatCellType,
          FloatConstantNoDataCellType,
          FloatUserDefinedNoDataCellType(1.0f),
          DoubleCellType,
          DoubleConstantNoDataCellType,
          DoubleUserDefinedNoDataCellType(1.0)
        )

      for(ct <- cellTypes) {
        val arr = Array.ofDim[Double](100).fill(5.0)
        arr(50) = 1.0
        arr(55) = 0.0
        arr(60) = Double.NaN

        val tile =
          DoubleArrayTile(arr, 10, 10, DoubleCellType).convert(ct)

        val smallerExtent = Extent(2.0, 2.0, 8.0, 8.0)
        val largerExtent = Extent(0.0, 0.0, 10.0, 10.0)

        val expected =
          tile.resample(largerExtent, RasterExtent(smallerExtent, 10, 10))

        val proto = tile.prototype(ct, tile.cols, tile.rows)
        val merged = proto.merge(smallerExtent, largerExtent, tile)
        withClue(s"Failing on cell type $ct: ") {
          assertEqual(merged, expected)
        }
      }
    }
  }
}
