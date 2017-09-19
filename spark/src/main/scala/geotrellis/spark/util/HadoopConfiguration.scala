package geotrellis.spark.util

import org.apache.hadoop.conf.Configuration

import java.io.{ObjectInputStream, ObjectOutputStream}

/**
  * Serializable [[Configuration]] wrapper
  * @param conf Hadoop Configuration
  */

case class HadoopConfiguration(var conf: Configuration) extends Serializable {
  def get: Configuration = conf

  private def writeObject(out: ObjectOutputStream): Unit =
    conf.write(out)

  private def readObject(in: ObjectInputStream): Unit = {
    conf = new Configuration()
    conf.readFields(in)
  }

  private def readObjectNoData(): Unit =
    conf = new Configuration()
}