package geotrellis.spark.store.hadoop.util

import geotrellis.store.hadoop.util.HdfsRangeReader
import geotrellis.util.RangeReader

import org.scalatest._

import java.net.URI


class HdfsRangeReaderProviderSpec extends FunSpec with Matchers {
  describe("HdfsRangeReaderProviderSpec") {
    val uri = new java.net.URI("hdfs+file:/tmp/catalog")

    it("should create a HdfsRangeReader from a URI") {
      val reader = RangeReader(uri)

      assert(reader.isInstanceOf[HdfsRangeReader])
    }
  }
}
