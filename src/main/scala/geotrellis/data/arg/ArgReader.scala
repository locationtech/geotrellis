package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.util.Filesystem

import java.nio.ByteBuffer

object ArgReader {
  final def read(path:String,typ:RasterType,rasterExtent:RasterExtent):Raster =
    Raster(readData(path,typ,rasterExtent),rasterExtent)

  final def readData(path:String,typ:RasterType,rasterExtent:RasterExtent):RasterData = {
    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    RasterData.fromArrayByte(Filesystem.slurp(path),typ,cols,rows)
  }

  final def read(path:String,typ:RasterType,rasterExtent:RasterExtent,targetExtent:RasterExtent):Raster =
    Raster(readData(path,typ,rasterExtent,targetExtent),targetExtent)

  final def readData(path:String,typ:RasterType,re:RasterExtent,targetRe:RasterExtent):RasterData = {
    val size = typ.numBytes(re.size)

    val cols = re.cols
    // Find the top-left most and bottom-right cell coordinates
    val GridBounds(colMin,rowMin,colMax,rowMax) = re.gridBoundsFor(targetRe.extent)

    // Get the indices, buffer one col and row on each side
    val startIndex = math.max(typ.numBytes((rowMin-1) * cols + colMin - 1),0)
    val length = math.min(size-startIndex, typ.numBytes((rowMax+1) * cols + colMax+1) - startIndex)

    if(length > 0) {
      val bytes = Array.ofDim[Byte](size)
      Filesystem.mapToByteArray(path,bytes,startIndex,length)

      warpBytes(bytes,typ,re,targetRe)
    } else {
      RasterData.emptyByType(typ,targetRe.cols, targetRe.rows)
    }
  }

  final def warpBytes(bytes:Array[Byte],typ:RasterType,re:RasterExtent,targetRe:RasterExtent):RasterData = {
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
