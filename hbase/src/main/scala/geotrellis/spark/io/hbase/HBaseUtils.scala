package geotrellis.spark.io.hbase

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.Filter

object HBaseUtils {
  def buildScan(table: String, family: Array[Byte], start: Array[Byte], stop: Array[Byte], filter: Filter, caching: Int = ConfigFactory.load().getInt("geotrellis.hbase.scanner.caching")): Scan = {
    val scan = new Scan(start, stop)
    scan.setCaching(caching)
    scan.setFilter(filter)
    scan.addFamily(family)
    scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, table)
    scan
  }
}
