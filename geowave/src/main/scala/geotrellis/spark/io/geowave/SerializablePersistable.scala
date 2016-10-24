package geotrellis.spark.io.geowave

import geotrellis.util.annotations.experimental

import mil.nga.giat.geowave.core.index.Persistable
import mil.nga.giat.geowave.core.index.PersistenceUtils
import org.apache.hadoop.io.ObjectWritable
import org.apache.spark.util.Utils

import java.io.{ObjectInputStream, ObjectOutputStream}


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class SerializablePersistable[T <: Persistable](@transient var t: T)
    extends Serializable {

  /** $experimental */
  @experimental def value: T = t

  /** $experimental */
  @experimental override def toString: String = t.toString

  @experimental private def writeObject(out: ObjectOutputStream): Unit = {
    val bytes = PersistenceUtils.toBinary(t)
    out.writeInt(bytes.length)
    out.write(bytes)
  }

  @experimental private def readObject(in: ObjectInputStream): Unit = {
    val length = in.readInt()
    val bytes = new Array[Byte](length)
    in.readFully(bytes)
    t=PersistenceUtils.fromBinary(bytes, classOf[Persistable]).asInstanceOf[T]
  }
}
