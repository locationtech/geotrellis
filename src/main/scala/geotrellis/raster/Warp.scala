package geotrellis.raster

import geotrellis._

import spire.syntax.cfor._

import java.nio.ByteBuffer

trait WarpAssign {
  def apply(srcIndex:Int,dstIndex:Int):Unit
}

class ArrayWarpAssign[@specialized(Byte,Short,Int,Float,Double) T](src:Array[T], dst:Array[T]) 
    extends WarpAssign {
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src(srcIndex)
  }
}

final class BitWarpAssign(src:Array[Byte],dst:Array[Byte])
      extends WarpAssign {
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    val i = srcIndex
    val z = (src(i >> 3) >> (i & 7)) & 1
    val div = dstIndex >> 3
    if ((z & 1) == 0) {
      // unset the nth bit
      dst(div) = (dst(div) & ~(1 << (dstIndex & 7))).toByte
    } else {
      // set the nth bit
      dst(div) = (dst(div) | (1 << (dstIndex & 7))).toByte
    }
  }
}

class ByteBufferWarpAssign(src:ByteBuffer,dst:Array[Byte])
    extends WarpAssign {
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src.get(srcIndex)
  }
}

class ShortBufferWarpAssign(src:ByteBuffer,dst:Array[Short])
    extends WarpAssign {
  final val width = TypeShort.bytes
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src.getShort(srcIndex*width)
  }
}

class IntBufferWarpAssign(src:ByteBuffer,dst:Array[Int])
    extends WarpAssign {
  final val width = TypeInt.bytes
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src.getInt(srcIndex*width)
  }
}

class FloatBufferWarpAssign(src:ByteBuffer,dst:Array[Float])
    extends WarpAssign {
  final val width = TypeFloat.bytes
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src.getFloat(srcIndex*width)
  }
}

class DoubleBufferWarpAssign(src:ByteBuffer,dst:Array[Double])
    extends WarpAssign {
  final val width = TypeDouble.bytes
  final def apply(srcIndex:Int,dstIndex:Int):Unit = {
    dst(dstIndex) = src.getDouble(srcIndex*width)
  }
}

object Warp {
  def apply[@specialized(Byte,Short,Int,Float,Double) T](current:RasterExtent,target:RasterExtent,source:Array[T],result:Array[T]):Unit =
    apply(current,target,new ArrayWarpAssign(source,result))

  def apply(current:RasterExtent,target:RasterExtent,assign:WarpAssign):Unit = {
    if(!current.extent.intersects(target.extent)) {
      return
    }
    // keep track of cell size in our source raster
    val src_cellwidth =  current.cellwidth
    val src_cellheight = current.cellheight
    val src_cols = current.cols
    val src_rows = current.rows
    val src_xmin = current.extent.xmin
    val src_ymin = current.extent.ymin
    val src_xmax = current.extent.xmax
    val src_ymax = current.extent.ymax


    val dst_cols = target.cols
    val dst_rows = target.rows

    val dst_cellwidth  = target.cellwidth
    val dst_cellheight = target.cellheight

    // save "normalized map coordinates" for destination cell (0, 0)
    var xbase = target.extent.xmin - src_xmin + (dst_cellwidth / 2)
    val ybase = target.extent.ymax - src_ymin - (dst_cellheight / 2)

    val src_map_width  = src_xmax - src_xmin
    val src_map_height = src_ymax - src_ymin

    val src_size = src_rows * src_cols

    val dst_size = dst_cols * dst_rows

    // these are the min and max columns we will access on this row
    val min_col = (xbase / src_cellwidth).toInt
    val max_col = ((xbase + dst_cols * dst_cellwidth) / src_cellwidth).toInt

    val startCol =
      if(target.extent.xmin < src_xmin) {
        val delta = src_xmin - target.extent.xmin
        val startCol = (delta / dst_cellwidth).toInt
        xbase += (dst_cellwidth * startCol)
        if(xbase < 0.0) {
          xbase += dst_cellwidth
          startCol + 1
        } else {
          startCol
        }
      } else {
        0
      }

    // start at the Y-center of the first dst grid cell
    var y = ybase

      cfor(0)(_ < dst_rows, _ + 1) { dst_row =>
        // calculate the Y grid coordinate to read from
        val src_row = (src_rows - (y / src_cellheight).toInt - 1)
  
        // pre-calculate some spans we'll use a bunch
        val src_span = src_row * src_cols
        val dst_span = dst_row * dst_cols

        if (src_span + min_col < src_size && src_span + max_col >= 0) {

          // start at the X-center of the first dst grid cell
          var x = xbase
          
          // loop over cols
          cfor(startCol)(_ < dst_cols, _ + 1) { dst_col =>
            // calculate the X grid coordinate to read from
            val src_col = (x / src_cellwidth).toInt
            
            // compute src and dst indices and ASSIGN!
            val src_i = src_span + src_col

            if (src_col >= 0 && src_col < src_cols &&
                src_i < src_size && src_i >= 0) {
              val dst_i = dst_span + dst_col
              assign(src_i,dst_i)
            }
            
            // increase our X map coordinate
            x += dst_cellwidth
          }
        }

        // decrease our Y map coordinate
        y -= dst_cellheight
      }
  }
}
