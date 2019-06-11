package geotrellis.store.hadoop.util

import geotrellis.store.hadoop.{HadoopCollectionLayerProvider, SCHEMES}
import geotrellis.util.RangeReaderProvider

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI


class HdfsRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => SCHEMES contains str.toLowerCase
    case null => false
  }

  def rangeReader(uri: URI): HdfsRangeReader = {
    val conf = new Configuration()

    HdfsRangeReader(new Path(HdfsUtils.trim(uri)), conf)
  }
}
