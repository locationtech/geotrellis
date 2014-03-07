package geotrellis.spark.testfiles
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.Context

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

class TestFiles(file: Path, conf: Configuration) {

  val (meta, opCtx) = setup

  val path = new Path(file, meta.maxZoomLevel.toString)

  def rasterDefinition = opCtx.rasterDefinition
  def rasterExtent = rasterDefinition.rasterExtent
  def tileLayout = rasterDefinition.tileLayout
  def tileCount = opCtx.rasterDefinition.tileLayout.tileCols * opCtx.rasterDefinition.tileLayout.tileRows

  private def setup = {
    val meta = PyramidMetadata(file, conf)
    (meta, Context.fromMetadata(meta.maxZoomLevel, meta))
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