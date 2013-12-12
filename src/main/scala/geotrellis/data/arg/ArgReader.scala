package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.process._

import java.nio.ByteBuffer

object ArgReader {
  def read(path:String,typ:RasterType,rasterExtent:RasterExtent):Raster =
    Raster(readData(path,typ,rasterExtent),rasterExtent)

  def readData(path:String,typ:RasterType,rasterExtent:RasterExtent):RasterData = {
    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    RasterData.fromArrayByte(util.Filesystem.slurp(path),typ,cols,rows)
  }

  def read(path:String,typ:RasterType,rasterExtent:RasterExtent,targetExtent:RasterExtent):Raster =
    Raster(readData(path,typ,rasterExtent,targetExtent),targetExtent)

  def readData(path:String,typ:RasterType,re:RasterExtent,targetRe:RasterExtent):RasterData =
    if(targetRe.extent.containsExtent(re.extent)) {
      // Based on benchmarks, if its the case that the target encompasses the
      // existing RasterExtent, it's faster to just read and warp
      readData(path,typ,re).warp(re,targetRe)
    } else {
      val bytes = util.Filesystem.slurp(path)
      warpBytes(bytes,typ,re,targetRe)
    }

  def warpBytes(bytes:Array[Byte],typ:RasterType,re:RasterExtent,targetRe:RasterExtent):RasterData = {
    val cols = targetRe.cols
    val rows = targetRe.rows

    typ match {
      case TypeBit =>
        val warped = Array.ofDim[Byte]((cols*rows + 7)/8)
        Warp(re,targetRe,new BitWarpAssign(bytes,warped))
        ByteArrayRasterData(warped,cols,rows)
      case TypeByte =>
        // ByteBuffer assign benchmarked faster than just using Array[Byte] for source.
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
        Warp(re,targetRe,new ByteBufferWarpAssign(buffer,warped))
        ByteArrayRasterData(warped,cols,rows)
      case TypeShort =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Short](cols*rows).fill(shortNODATA)
        Warp(re,targetRe,new ShortBufferWarpAssign(buffer,warped))
        ShortArrayRasterData(warped,cols,rows)
      case TypeInt =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Int](cols*rows).fill(NODATA)
        Warp(re,targetRe,new IntBufferWarpAssign(buffer,warped))
        IntArrayRasterData(warped,cols,rows)
      case TypeFloat =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Float](cols*rows).fill(Float.NaN)
        Warp(re,targetRe,new FloatBufferWarpAssign(buffer,warped))
        FloatArrayRasterData(warped,cols,rows)
      case TypeDouble =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Double](cols*rows).fill(Double.NaN)
        Warp(re,targetRe,new DoubleBufferWarpAssign(buffer,warped))
        DoubleArrayRasterData(warped,cols,rows)
    }
  }
}
