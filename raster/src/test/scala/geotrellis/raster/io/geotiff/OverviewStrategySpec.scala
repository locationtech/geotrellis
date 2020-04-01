package geotrellis.raster.io.geotiff

import geotrellis.raster._

import org.scalatest._

class OverviewStrategySpec extends FunSpec with Matchers {
  val availableResolutions = List(
    CellSize(1, 1),
    CellSize(2, 2),
    CellSize(4, 4),
    CellSize(8, 8),
    CellSize(16, 16)
  )
  describe("Auto") {
    it("auto0 should select the closest resolution") {
      val strategy = Auto(0)
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(5,5), strategy)
      selected should be (2)
    }
    it("auto1 should select the one after the closest") {
      val strategy = Auto(1)
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(5,5), strategy)
      selected should be (3)
    }
    it("auto2 should select the one after the one after closest") {
      val strategy = Auto(2)
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(5,5), strategy)
      selected should be (4)
    }
    it ("should select the last overview if out of bounds high") {
      val strategy = Auto(8)
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(1,1), strategy)
      selected should be (4)
    }
  }
  describe("Level") {
    it("should select the nth overview") {
      val strategy = Level(2)
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(5,5), strategy)
      selected should be (2)
    }
  }
  describe("Base") {
    it("should select the highest/base resolution overview") {
      val strategy = Base
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(5,5), strategy)
      selected should be (0)
    }
  }
  describe("AutoHigherResolution") {
    val strategy = AutoHigherResolution

    it("should select the nearest overview - without rounding down") {
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(7,7), strategy)
      selected should be (2)
    }

    it("should select the base overview if out of bounds") {
      val selected =
        OverviewStrategy.selectOverview(availableResolutions, CellSize(32,32), strategy)
      selected should be (0)
    }
  }
}
