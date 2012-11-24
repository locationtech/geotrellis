package geotrellis.raster.op.focal

import scala.math.{min,max}

/*
 * CursorMask structure:
 * A one dimension array of size d x d+1, masquerading as 2d array,
 * with the first element in each row being the number of values for
 * that row. Those values represent the unmasked indexes.
 * A lot of precomputation of different mask and unmask values are
 * used so that determining what has been masked or unmasked by a cursor
 * move can be fast.
 */
class CursorMask(d:Int,f:(Int,Int)=>Boolean) {
  val maskFunc = f

  private val m = Array.ofDim[Int](d*(d+1))

  val maskedUp = Array.ofDim[Int](d*d)
  val unmaskedUp = Array.ofDim[Int](d*d)
  val maskedLeft = Array.ofDim[Int](d*d)
  val unmaskedLeft = Array.ofDim[Int](d*d)
  
  // Record the mask values
  var x = 0
  var y = 0
  var len = 0
  var isMasked = false
  var leftIsMasked = false
  while(y < d) {
    x = 0
    len = 0
    while(x < d) {
      isMasked = maskFunc(x,y)
      if(!isMasked) {
        len += 1
        m(y*(d+1) + len) = x
      }

      // Check if moving left yeilds a different mask result,
      // so we can tell if moving horizontally has changed the
      // masked value for a cell.
      if(x > 0) {
        if(leftIsMasked && !isMasked) {
          // Moving left will unmask
          val len2 = unmaskedLeft(y*d) + 1
          unmaskedLeft(y*d + len2) = x
          unmaskedLeft(y*d) = len2
        } else if(!leftIsMasked && isMasked) {
          // Moving left will mask
          val len2 = maskedLeft(y*d) + 1
          maskedLeft(y*d + len2) = x
          maskedLeft(y*d) = len2
        }
      }
      
      //Check if moving up yeilds different mask result
      if(y > 0) {
        val upIsMasked = maskFunc(x,y-1)
        
        if(upIsMasked && !isMasked) {
          // Moving up will unmask
          val len2 = unmaskedUp((y-1)*d) + 1
          unmaskedUp((y-1)*d + len2) = x
          unmaskedUp((y-1)*d) = len2
        } else if(!upIsMasked && isMasked) {
          // Moving up will mask
          val len2 = maskedUp((y-1)*d) + 1
          maskedUp((y-1)*d + len2) = x
          maskedUp((y-1)*d) = len2
        }
      }
      m(y*(d+1)) = len
      leftIsMasked = isMasked
      x += 1
    }
    y += 1
  }
 
  def foreachX(row:Int,xmin:Int,xdelta:Int)(f:Int=>Unit) {
    val len = m(row*(d+1))
    if(len > 0) {
      var x = 0
      while(m(row*(d+1) + x + 1) < xmin && x < len) {
        x += 1
      }
      while(x < len && m(row*(d+1) + x + 1) <= xmin + xdelta) {
        f(m(row*(d+1) + x + 1))
        x += 1
      }
    }
  }

  def foreachMaskedUp(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(1,ymin)
    var x = 0
    var len = 0
    while(y <= ymin+ydelta) {
      len = maskedUp((y-1)*d)
      if(len > 0) {
        x = 0
        while(maskedUp((y-1)*d + x + 1) < xmin && x < len) {
          x += 1
        }
        while(x < len && maskedUp((y-1)*d + x + 1) <= xmin + xdelta) {
          f(maskedUp((y-1)*d + x + 1), y)
          x += 1
        }
      }
      y += 1
    }
  }

  def foreachUnmaskedUp(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(1,ymin)
    var x = 0
    var len = 0
    while(y <= ymin+ydelta) {
      len = unmaskedUp((y-1)*d)
      if(len > 0) {
        x = 0
        while(unmaskedUp((y-1)*d + x + 1) < xmin && x < len) {
          x += 1
        }
        while(x < len && unmaskedUp((y-1)*d + x + 1) <= xmin + xdelta) {
          f(unmaskedUp((y-1)*d + x + 1), y)
          x += 1
        }
      }
      y += 1
    }
  }

  def foreachMaskedDown(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(0,ymin)
    var ymax = min(ymin+ydelta, d - 2)
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = unmaskedUp(y*d)
      x = 0
      while(unmaskedUp(y*d + x + 1) < xmin && x < len) {
        x += 1
      }
      while(x < len && unmaskedUp(y*d + x + 1) <= xmin + xdelta) {
        f(unmaskedUp(y*d + x + 1), y)
        x += 1
      }
      y += 1
    }
  }

  def foreachUnmaskedDown(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(0,ymin)
    var ymax = min(ymin+ydelta, d - 2)
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = maskedUp(y*d)
      x = 0
      while(maskedUp(y*d + x + 1) < xmin && x < len) {
        x += 1
      }
      while(x < len && maskedUp(y*d + x + 1) <= xmin + xdelta) {
        f(maskedUp(y*d + x + 1), y)
        x += 1
      }
      y += 1
    }
  }  

  def foreachMaskedLeft(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(0,ymin)
    var ymax = ymin+ydelta
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = maskedLeft(y*d)
      if(len > 0) {
        x = 0
        while(maskedLeft(y*d + x + 1) <= xmin && x < len) {
          x += 1
        }
        while(x < len && maskedLeft(y*d + x + 1) <= xmin + xdelta) {
          f(maskedLeft(y*d + x + 1), y)
          x += 1
        }
      }
      y += 1
    }
  }

  def foreachUnmaskedLeft(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(ymin,0)
    var ymax = ymin+ydelta
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = unmaskedLeft(y*d)
      if(len > 0) {
        x = 0
        while(unmaskedLeft(y*d + x + 1) <= xmin && x < len) {
          x += 1
        }
        while(x < len && unmaskedLeft(y*d + x + 1) <= xmin + xdelta) {
          f(unmaskedLeft(y*d + x + 1), y)
          x += 1
        }
      }
      y += 1
    }
  }  

  def foreachMaskedRight(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(0,ymin)
    var ymax = ymin+ydelta
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = unmaskedLeft(y*d)
      if(len > 0) {
        x = 0
        while(unmaskedLeft(y*d + x + 1)-1 < xmin && x < len) {
          x += 1
        }
        while(x < len && unmaskedLeft(y*d + x + 1) - 1 <= xmin + xdelta) {
          f(unmaskedLeft(y*d + x+1)-1, y)
          x += 1
        }
      }
      y += 1
    }
  }

  def foreachUnmaskedRight(xmin:Int,xdelta:Int,ymin:Int,ydelta:Int)(f:(Int,Int)=>Unit) {
    var y = max(ymin,0)
    var ymax = ymin+ydelta
    var x = 0
    var len = 0
    while(y <= ymax) {
      len = maskedLeft(y*d)
      if(len > 0) {
        x = 0
        while(maskedLeft(y*d + x + 1)-1 < xmin && x < len) {
          x += 1
        }
        while(x < len && maskedLeft(y*d + x + 1)-1 <= xmin + xdelta) {
          f(maskedLeft(y*d + x + 1)-1, y)
          x += 1
        }
      }
      y += 1
    }
  }

  def asciiDraw:String = {
    var r = ""
    for(y <- 0 to d - 1) {
      val l = for(i <- 1 to m(y*(d+1))) yield m(y*(d+1) +  i)
      for(x <- 0 to d - 1) {
        if(l.contains(x)) { r += " O " } else { r += " X " }
      }
      r += "\n"
    }
    r
  }
}

