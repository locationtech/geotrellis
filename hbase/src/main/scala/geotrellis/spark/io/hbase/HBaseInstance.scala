package geotrellis.spark.io.hbase

import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
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
  def getAdmin: Admin = getConnection.getAdmin

  @transient lazy val connection: Connection = getConnection
  @transient lazy val admin: Admin = getAdmin

  /** Without connection close, for a custom connection close */
  def withConnection[T](block: Connection => T): T = block(connection)
  def withAdmin[T](block: Admin => T): T = block(admin)

  /** With connection close */
  def withConnectionDo[T](block: Connection => T): T = {
    val connection = getConnection
    try block(connection) finally connection.close()
  }

  def withTableConnectionDo[T](tableName: TableName)(block: Table => T): T = {
    val connection = getConnection
    val tableConnection = connection.getTable(tableName)
    try block(tableConnection) finally {
      tableConnection.close()
      connection.close()
    }
  }

  def withAdminDo[T](block: Admin => T): T = {
    val connection = getConnection
    val admin = connection.getAdmin
    try block(admin) finally {
      admin.close()
      connection.close()
    }
  }

  def close = {
    admin.close()
    connection.close()
  }
}
