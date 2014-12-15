package geotrellis.spark.ingest

import  geotrellis.spark.cmd.args._
import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required
import geotrellis.proj4.CRS
import org.apache.hadoop.fs._


trait IngestArgs extends FieldArgs {
  @Required var input: String = _
  @Required var layerName: String = _
  var crs: String = "EPSG:4326"
  var pyramid: Boolean = false
  var clobber: Boolean = false
  var partitions: Int = 24

  def destCrs: CRS = CRS.fromName(crs)
  def inPath: Path = new Path(input)
}