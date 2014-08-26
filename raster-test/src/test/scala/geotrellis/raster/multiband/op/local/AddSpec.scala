package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._
import geotrellis.vector.Extent

import org.scalatest._
import geotrellis.testkit._

class AddSpec extends FunSpec
  with Matchers
  with TestEngine
  with MultiBandTileBuilder {

  describe("Add MultiBandTile") {
    it("add an int constant value to each cell of an int valued multiband raster") {
      val m: MultiBandTile = intMultiBand
      val result = m + 10
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).get(col, row) should be(m.getBand(band).get(col, row) + 10)
          }
        }
      }
    }

    it("adds an int constant value to each cell of an double valued multiband raster") {
      val m = doubleMultiBand
      val result = m + 88
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).getDouble(col, row) should be(m.getBand(band).getDouble(col, row) + 88)
          }
        }
      }
    }

    it("add a double constant value to each cell of an int valued multiband raster") {
      val m: MultiBandTile = intMultiBand
      val result = m + .98
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).get(col, row) should be((m.getBand(band).get(col, row) + .98).toInt)
          }
        }
      }
    }

    it("adds a double constant value to each cell of an double valued multiband raster") {
      val m = doubleMultiBand
      val result = m + 58.34236
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).getDouble(col, row) should be(m.getBand(band).getDouble(col, row) + 58.34236)
          }
        }
      }
    }

    it("add int valued multiband raster it self") {
      val m = intMultiBand
      val result = m + m
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).get(col, row) should be(m.getBand(band).get(col, row) * 2)
          }
        }
      }
    }

    it("add double valued multiband raster it self") {
      val m = doubleMultiBand
      val result = m + m
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).getDouble(col, row) should be(m.getBand(band).getDouble(col, row) * 2)
          }
        }
      }
    }

    it("add first 3 bands together in int valued multiband raster") {
      val m = intMultiBand
      val result = m.localAdd(0, 2)
      for (col <- 0 until m.cols) {
        for (row <- 0 until m.rows) {
          result.get(col, row) should be(m.getBand(0).get(col, row) + m.getBand(1).get(col, row) + m.getBand(2).get(col, row))
        }
      }
    }

    it("add first 2 bands together in double valued multiband raster") {
      val m = doubleMultiBand
      val result = m.localAdd(0, 1)
      for (col <- 0 until m.cols) {
        for (row <- 0 until m.rows) {
          result.getDouble(col, row) should be(m.getBand(0).getDouble(col, row) + m.getBand(1).getDouble(col, row))
        }
      }
    }

    it("add all bands together in int multiband raster"){
      val m = intConstMB 
      val result = m.localAdd
      for (col <- 0 until m.cols) {
        for (row <- 0 until m.rows) {
          result.get(col, row) should be(6)
        }
      }
    }
    
    it("add all bands together in double multiband raster"){
      val m = doubleConstMB 
      val result = m.localAdd
      for (col <- 0 until m.cols) {
        for (row <- 0 until m.rows) {
          result.getDouble(col, row) should be(6.0)
        }
      }
    }
    
  }
}
