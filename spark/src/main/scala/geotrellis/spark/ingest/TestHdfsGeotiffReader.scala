package geotrellis.spark.ingest

import geotrellis.spark.cmd.CommandArguments
import com.quantifind.sumac.ArgMain
import java.io.IOException
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import org.geotools.coverage.grid.io.AbstractGridFormat
import scala.collection.mutable.Set
import org.geotools.coverage.grid.io.GridFormatFinder
import scala.collection.JavaConversions._
import org.geotools.factory.Hints
import org.geotools.referencing.CRS
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.Logging

object TestHdfsGeotiffReader extends Logging {

  def main(args: Array[String]): Unit = {
    val conf = SparkUtils.createHadoopConfiguration
    val input = new Path("file:///home/akini/test/costdistance/testCostDistance.tif")
    val input2 = new Path("file:///home/akini/test/costdistance/testCostDistanceWithBounds.tif")
    val input3 = new Path("file:///home/akini/test/costdistance/README.md")
    val start = System.currentTimeMillis()

    GeoTiff.getMetadata(input, conf) match {
      case None    => sys.error("Couldn't find metadata")
      case Some(m) => println("found: " + m)
    }
    GeoTiff.getMetadata(input2, conf) match {
      case None    => sys.error("Couldn't find metadata")
      case Some(m) => println("found: " + m)
    }
    GeoTiff.getMetadata(input3, conf) match {
      case None    => println("As expected, couldn't find metadata for " + input3)
      case Some(m) => sys.error("wtf!")
    }
    val end = System.currentTimeMillis()
    logInfo(s"Ran in ${end - start} ms")
    logInfo("Done")
  }
}