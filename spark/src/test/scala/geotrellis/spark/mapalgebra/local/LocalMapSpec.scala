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

package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.TileLayerRDD
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import java.time.ZonedDateTime

import org.scalatest._

class LocalMapSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Local Map Operations") {
    it("should map an integer function over an integer raster rdd") {
      val arr: Array[Int] =
        Array(NODATA, 1, 1, 1,
              1, 1, 1, 1,

              1, 1, 1, 1,
              1, 1, 1, NODATA)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMap((z: Int) => if (isNoData(z)) 0 else z + 1)
      rasterShouldBeInt(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) 0 else 2)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should map an integer function over an integer spacetime raster rdd") {
      val arr: Array[Int] =
        Array(NODATA, 1, 1, 1,
              1, 1, 1, 1,

              1, 1, 1, 1,
              1, 1, 1, NODATA)

      val tile = ArrayTile(arr, 4, 4)  // size should be 4
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createSpaceTimeTileLayerRDD(Array((tile, ZonedDateTime.now())), tileLayout)

      val result = rdd.localMap((z: Int) => if (isNoData(z)) 42 else 42)  // All values are 4
      rasterShouldBe(result, 42, 4)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should map an integer function over a double raster rdd") {
      val arr: Array[Double] =
        Array(Double.NaN, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, 1.5,

              1.5, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, Double.NaN)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMap((z: Int) => if (isNoData(z)) 0 else z + 1)
      rasterShouldBe(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) 0.0 else 2.0)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should map an integer function over a double spacetime raster rdd") {
      val arr: Array[Double] =
        Array(NODATA, 1.3, 1.5, 1.5,
              1.1, 1.5, 1.9, NODATA,

              1.2, 1.2, NODATA, 1.2,
              1.2, NODATA, 1.2, 1.5)

      val tile = ArrayTile(arr, 4, 4)  // size should be 4
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createSpaceTimeTileLayerRDD(Array((tile, ZonedDateTime.now())), tileLayout)

      val result = rdd.localMap((z: Int) => if (isNoData(z)) 1000 else z * 10)  // All values are 4
      rasterShouldBe(result, minMax=(10, 1000))
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should map a double function over an integer raster rdd") {
      val arr: Array[Int] =
        Array(NODATA, 1, 1, 1,
              1, 1, 1, 1,

              1, 1, 1, 1,
              1, 1, 1, NODATA)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(sc, tile, tileLayout)

      val result = rdd.localMapDouble((z: Double) => if (isNoData(z)) 0.0 else z + 1.0)
      rasterShouldBeInt(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) 0 else 2)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should map a double function over a double raster rdd") {
      val arr: Array[Double] =
        Array(Double.NaN, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, 1.5,

              1.5, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, Double.NaN)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMapDouble((z: Double) => if (isNoData(z)) 0.0 else z + 0.3)
      rasterShouldBe(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) 0 else 1.8)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    // TODO figure this out
    it("should mapIfSet an integer function over an integer raster rdd") {
      val arr: Array[Int] =
        Array(NODATA, 1, 1, 1,
              1, 1, 1, 1,

              1, 1, 1, 1,
              1, 1, 1, NODATA)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMapIfSet((z: Int) => z + 1)
      rasterShouldBeInt(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) NODATA else 2)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should mapIfSet a integer function over a double raster rdd") {
      val arr: Array[Double] =
        Array(Double.NaN, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, 1.5,

              1.5, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, Double.NaN)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMapIfSet((z: Int) => z + 1)
      // for some reason this is being converted to a double raster tile
      rasterShouldBe(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) Double.NaN else 2.0)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should mapIfSet an double function over an integer raster rdd") {
      val arr: Array[Int] =
        Array(NODATA, 1, 1, 1,
              1, 1, 1, 1,

              1, 1, 1, 1,
              1, 1, 1, NODATA)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMapIfSetDouble((z: Double) => z + 1.0)
      rasterShouldBeInt(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) NODATA else 2)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

    it("should mapIfSet a double function over a double raster rdd") {
      val arr: Array[Double] =
        Array(Double.NaN, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, 1.5,

              1.5, 1.5, 1.5, 1.5,
              1.5, 1.5, 1.5, Double.NaN)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(tile, tileLayout)

      val result = rdd.localMapIfSetDouble((z: Double) => z + 0.3)
      rasterShouldBe(result, (x: Int, y: Int) => if ((x == 0 && y == 0) || (x == 3 && y == 3)) Double.NaN else 1.8)
      rastersShouldHaveSameIdsAndTileCount(rdd, result)
    }

  }
}
