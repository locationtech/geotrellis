package geotrellis.spark.formats
import geotrellis._
import geotrellis.spark._
import geotrellis.spark.metadata._

import org.apache.hadoop.io.Writable

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/*
 * The PayloadArgWritable is a variant of ArgWritable wherein you can store not just the RasterData 
 * but also a "payload", which is any Writable object.
 * 
 * The apply variants handle the serialization - i.e., going from a RasterData and a Writable payload 
 * to a PayloadArgWritable
 * 
 * The toPayloadRasterData handles the deserialization - going from an existing PayloadArgWritable to 
 * a RasterData (the return value) and a payload (mutable argument passed in). The latter is passed in
 * instead of returned, since the typical use case is iterating over some RDD of PayloadArgWritables, 
 * and getting both the RasterData and payload out. The user can then create the Writable object once 
 * and inside the loop iterating over the RDD, pass it into the toPayloadRasterData. This avoids creating
 * Writable objects inside the method
 * 
 * There is a convenience implicit in the package object that wraps toPayloadRasterData  
 * 
 */
class PayloadArgWritable(bytes: Array[Byte]) extends ArgWritable(bytes) {
  def toPayloadRasterData(awType: RasterType, cols: Int, rows: Int, payload: Writable) = {
    val rd = toRasterData(awType, cols, rows)
    val payloadStart = rasterBytes(awType, cols, rows)
    assert(bytes.length > payloadStart)
    // extract the payload
    val bais = new ByteArrayInputStream(bytes, payloadStart, bytes.length - payloadStart)
    val dis = new DataInputStream(bais)
    payload.readFields(dis)
    dis.close()
    rd
  }
}

object PayloadArgWritable {
  def apply(bytes: Array[Byte]) = new PayloadArgWritable(bytes)

  def fromPayloadRasterData(data: RasterData, payload: Writable) = PayloadArgWritable(serialize(data, payload))
  def fromPayloadRaster(raster: Raster, payload: Writable) = fromPayloadRasterData(raster.data, payload)
  
  private def serialize(data: RasterData, payload: Writable): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(baos)
    payload.write(dos)
    val bytes = data.toArrayByte ++ baos.toByteArray
    dos.close()
    bytes
  }
}