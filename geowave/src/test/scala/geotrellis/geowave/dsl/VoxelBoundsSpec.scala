package geotrellis.geowave.dsl

import cats.syntax.option._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class VoxelBoundsSpec extends AnyFunSpec with Matchers {
  describe("VoxelSpec") {
    it("should build split VoxelBounds2D") {
      val bounds = VoxelDimensions2D(100, 100).toVoxelBounds
      val splits = bounds.split(VoxelDimensions2D(10, 10))

      splits.length shouldBe 10 * 10
      splits.foreach { vb =>
        (vb.colMax - vb.colMin) shouldBe 10 - 1
        (vb.rowMax - vb.rowMin) shouldBe 10 - 1
      }
    }

    it("should build split VoxelBounds3D") {
      val bounds = VoxelDimensions3D(100, 100, 100).toVoxelBounds
      val splits = bounds.split(VoxelDimensions3D(10, 10, 10))

      splits.length shouldBe 10 * 10 * 10
      splits.foreach { vb =>
        (vb.colMax - vb.colMin) shouldBe 10 - 1
        (vb.rowMax - vb.rowMin) shouldBe 10 - 1
        (vb.depthMax - vb.depthMin) shouldBe 10 - 1
      }
    }

    it("should build split VoxelBounds4D") {
      val bounds = VoxelDimensions4D(100, 100, 100, 100).toVoxelBounds
      val splits = bounds.split(VoxelDimensions4D(10, 10, 10, 10))

      splits.length shouldBe 10 * 10 * 10 * 10
      splits.foreach { vb =>
        (vb.colMax - vb.colMin) shouldBe 10 - 1
        (vb.rowMax - vb.rowMin) shouldBe 10 - 1
        (vb.depthMax - vb.depthMin) shouldBe 10 - 1
        (vb.spissitudeMax - vb.spissitudeMin) shouldBe 10 - 1
      }
    }

    it("should create valid VoxelDimensions from TilingBounds") {
      val tb = TilingBounds(depth = 1.some)
      val bounds = VoxelBounds3D(0, 100, 0, 100, 0, 100).toVoxelDimensions
      bounds.withTilingBounds(tb) shouldBe VoxelDimensions3D(100, 100, 1)
    }
  }
}
