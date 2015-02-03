package geotrellis.spark.ingest

import  geotrellis.spark.cmd.args._
import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required
import geotrellis.proj4.CRS
import org.apache.hadoop.fs._


trait IngestArgs extends FieldArgs {
  /** Hadoop url for layer file or directory (file, hdfs, s3n, ..) */
  @Required var input: String = _
  
  /** Layer name to be used in the catalog */
  @Required var layerName: String = _
  
  /** Reproject the layre to this CRS  */
  var crs: String = "EPSG:4326"

  /** Pyramid the layer from determined zoom level (based on resolution) to zoom level 1  */
  var pyramid: Boolean = false
  
  /** Clobber layer if it already exists in the catalog */
  var clobber: Boolean = false
  
  /** Partition the records imediatly after input, before ingest.
   * In case of input being a single file this has dramatic impacts on performance. */
  var partitions: Int = 24

  def destCrs: CRS = CRS.fromName(crs)
  def inPath: Path = new Path(input)
}