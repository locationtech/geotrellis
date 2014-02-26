package geotrellis.spark.testfiles
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.TmsTilingConvert

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

class TestFiles(file: Path, conf: Configuration) {

  val (meta, rasterDefinition) = setup
  val path = new Path(file, meta.maxZoomLevel.toString)
  
  def tileCount = rasterDefinition.tileLayout.tileCols * rasterDefinition.tileLayout.tileRows
  
  private def setup = {
    val meta = PyramidMetadata(file, conf)
    (meta, TmsTilingConvert.rasterDefinition(meta.maxZoomLevel, meta))
  }
}

object AllOnes {
  def apply(prefix: Path, conf: Configuration) = new TestFiles(new Path(prefix, "all-ones"), conf)
}

object AllTwos {
  def apply(prefix: Path, conf: Configuration) = new TestFiles(new Path(prefix, "all-twos"), conf)
}

object AllHundreds {
  def apply(prefix: Path, conf: Configuration) = new TestFiles(new Path(prefix, "all-hundreds"), conf)
}