package geotrellis.layers.hadoop

import org.apache.hadoop.conf.Configuration

import java.io.{ObjectInputStream, ObjectOutputStream}

/** From Spark codebase. Allows us to use the Hadoop Configuration in serializable tasks */
case class SerializableConfiguration(@transient var value: Configuration) extends Serializable {
  private def writeObject(out: ObjectOutputStream): Unit = {
    out.defaultWriteObject()
    value.write(out)
  }

  private def readObject(in: ObjectInputStream): Unit = {
    value = new Configuration(false)
    value.readFields(in)
  }
}
