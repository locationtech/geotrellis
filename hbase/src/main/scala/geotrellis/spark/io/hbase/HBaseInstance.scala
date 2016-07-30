package geotrellis.spark.io.hbase

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client._

case class HBaseInstance(zookeepers: Seq[String], master: String, clientPort: String = "2181") extends Serializable {
  @transient lazy val conf = {
    val c = HBaseConfiguration.create
    c.set("hbase.zookeeper.quorum", zookeepers.mkString(","))
    c.set("hbase.zookeeper.property.clientPort", clientPort)
    c.set("hbase.master", master)
    c
  }

  def getConnection: Connection = ConnectionFactory.createConnection(conf)
  @transient lazy val getAdmin: Admin = getConnection.getAdmin
}
