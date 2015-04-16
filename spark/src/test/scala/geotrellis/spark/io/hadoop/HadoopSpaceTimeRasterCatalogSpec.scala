package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.raster._
import geotrellis.vector._

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.proj4.LatLng
import geotrellis.spark.testfiles._

import org.scalatest._
import org.apache.hadoop.fs.Path
import com.github.nscala_time.time.Imports._

class HadoopSpaceTimeRasterCatalogSpec extends FunSpec
    with Matchers
    with RasterRDDMatchers
    with TestEnvironment
    with TestFiles
    with OnlyIfCanRunSpark
{

  describe("HadoopRasterCatalog with SpaceTimeKey Rasters") {
    ifCanRunSpark {
      val catalogPath = new Path(inputHome, ("st-catalog-spec"))
      val fs = catalogPath.getFileSystem(sc.hadoopConfiguration)
      HdfsUtils.deletePath(catalogPath, sc.hadoopConfiguration)
      val catalog = HadoopRasterCatalog(catalogPath)

      it("ZCurveKeyIndexMethod.byYear") {
        val coordST = CoordinateSpaceTime
        catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear).write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.reader[SpaceTimeKey].read(LayerId("coordST", 10)), coordST)
      }

      it("ZCurveKeyIndexMethod.by(DateTime => Int)") {
        val coordST = CoordinateSpaceTime
        val tIndex = (x: DateTime) =>  if (x < DateTime.now) 1 else 0

        catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.by(tIndex)).write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.reader[SpaceTimeKey].read(LayerId("coordST", 10)), coordST)
      }

      it("HilbertKeyIndexMethod with min, max, and resolution") {
        val coordST = CoordinateSpaceTime
        val now = DateTime.now

        catalog.writer[SpaceTimeKey](HilbertKeyIndexMethod(now - 20.years, now, 4)).write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.reader[SpaceTimeKey].read(LayerId("coordST", 10)), coordST)
      }
      it("HilbertKeyIndexMethod with only resolution") {
        val coordST = CoordinateSpaceTime
        val now = DateTime.now

        catalog.writer[SpaceTimeKey](HilbertKeyIndexMethod(2)).write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.reader[SpaceTimeKey].read(LayerId("coordST", 10)), coordST)
      }
    }
  }
}
