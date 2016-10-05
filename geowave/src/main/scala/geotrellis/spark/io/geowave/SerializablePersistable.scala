package geotrellis.spark.io.geowave

import mil.nga.giat.geowave.core.index.Persistable
import mil.nga.giat.geowave.core.index.PersistenceUtils
import org.apache.hadoop.io.ObjectWritable
import org.apache.spark.util.Utils

import java.io.{ObjectInputStream, ObjectOutputStream}


class SerializablePersistable[T <: Persistable](@transient var t: T)
    extends Serializable {

  def value: T = t

  override def toString: String = t.toString

  private def writeObject(out: ObjectOutputStream): Unit = {
    val bytes = PersistenceUtils.toBinary(t)
    out.writeInt(bytes.length)
    out.write(bytes)
  }

  private def readObject(in: ObjectInputStream): Unit = {
    val length = in.readInt()
    val bytes = new Array[Byte](length)
    in.readFully(bytes)
    t=PersistenceUtils.fromBinary(bytes, classOf[Persistable]).asInstanceOf[T]
  }
}
