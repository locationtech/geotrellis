package geotrellis.spark.io.accumulo


import geotrellis.spark.testfiles._

import org.scalatest._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import geotrellis.proj4.LatLng
import geotrellis.spark._
import geotrellis.spark.tiling.FloatingLayoutScheme
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import org.apache.hadoop.fs.Path

class Playground extends FunSpec
  with Matchers
  with TestFiles
  with TestEnvironment
  with OnlyIfCanRunSpark {

  describe("Playground") {
    ifCanRunSpark {
      val accumulo = AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )

      val source = sc.netCdfRDD(new Path("file:/Users/eugene/tmp/gcm/tas-access1-rcp85.nc"))
      val (_, rmd) = source.collectMetaData(LatLng, FloatingLayoutScheme(278))
      val tiled = source.tile[SpaceTimeKey](rmd)

      tiled.map(_._1).foreach(println)
    }
  }
}
