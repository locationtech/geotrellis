package geotrellis.layers.hadoop.util

import geotrellis.layers.hadoop.HadoopCollectionLayerProvider
import geotrellis.util.RangeReaderProvider

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI


class HdfsRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => HadoopCollectionLayerProvider.schemes contains str.toLowerCase
    case null => false
  }

  def rangeReader(uri: URI): HdfsRangeReader = {
    val conf = new Configuration()

    HdfsRangeReader(new Path(uri), conf)
  }
}
