package geotrellis.spark.old
import geotrellis.spark.cmd.CommandArguments
import geotrellis.spark.ingest.HdfsImageInputStreamSpi
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
  private val formats = loadFormats()
  
  def main(args: Array[String]): Unit = {
    val conf = SparkUtils.createHadoopConfiguration
    val input = new Path("hdfs://localhost:9000/tmp/262359N0530600E_V1.tif")
    val reader = openHdfsImage(input, conf)
    logInfo("Done")
  }
  
  
  def  openHdfsImage(input: Path, conf: Configuration): AbstractGridCoverage2DReader = {

    logInfo("Loading Image file: " + input.toString());

    val stream = input.getFileSystem(conf).open(input)
logInfo("before fastFormatFinder")
    val format = fastFormatFinder(stream) match {
      case Some(f) => f
      case None => sys.error("Couldn't find format")
    }
    logInfo("before getReader")
    format.getReader(stream, new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785")))   
  }
  
  
  def fastFormatFinder(obj: Object): Option[AbstractGridFormat] = 
    formats.find(_.accepts(obj, new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))))
    
  
  def loadFormats(): Set[AbstractGridFormat] = {
    HdfsImageInputStreamSpi.orderInputStreamProviders
    
    val spis = GridFormatFinder.getAvailableFormats()
    spis.map(_.createFormat())
      
  }
}