/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.focal

import scala.math.{min,max}
import scala.collection.mutable.LinkedList

import Movement._

/**
 * A mask over a cursor. The CursorMask
 * helps the cursor keep track of the state of masking
 * and unmasking of cells between moves.
 */
class CursorMask(d:Int,f:(Int,Int)=>Boolean) {

  /** A fast data type that records the unmasked values
   * of the cursor per cursor row */
  private class MaskSet {
    private var data = Array.ofDim[Int](d*(d+1))

    def add(i:Int,v:Int) = {
      val len = data(i*(d+1)) + 1
      data(i*(d+1) + len) = v
      data(i*(d+1)) = len
    }

    def foreachAt(i:Int)(f:Int=>Unit) = {
      val len = data(i*(d+1))
      if(len > 0) {
        var x = 0
        while(x < len) {
          f(data(i*(d+1) + x + 1))
          x += 1
        }
      }
    }
  }
  
  // Holds data about what cells are unmasked
  private val unmasked = new MaskSet
  private var westColumnUnmasked = Array.ofDim[Int](d+1)
  private var eastColumnUnmasked = Array.ofDim[Int](d+1)

  // Holds data about what cells are masked or unmasked by moving the cursor.
  private val maskedAfterMoveUp = new MaskSet
  private val unmaskedAfterMoveUp = new MaskSet
  private val maskedAfterMoveLeft = new MaskSet
  private val unmaskedAfterMoveLeft = new MaskSet

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
      isMasked = f(x,y)
      if(!isMasked) {
        unmasked.add(y,x)

        // Record the border columns, they are a special case to be tracked.
        if(x == 0) {
          val len = westColumnUnmasked(0) + 1
          westColumnUnmasked(len) = y
          westColumnUnmasked(0) = len
        }
        if(x == d - 1) {
          val len = eastColumnUnmasked(0) + 1
          eastColumnUnmasked(len) = y
          eastColumnUnmasked(0) = len
        }
      }

      // Check if moving left yeilds a different mask result,
      // so we can tell if moving horizontally has changed the
      // masked value for a cell.
      if(x > 0) {
        val leftIsMasked = f(x-1,y)

        if(leftIsMasked && !isMasked) {
          // Moving left will unmask
          unmaskedAfterMoveLeft.add(y,x)
        } else if(!leftIsMasked && isMasked) {
          // Moving left will mask
          maskedAfterMoveLeft.add(y,x)
        }
      }
      
      //Check if moving up yeilds different mask result
      if(y > 0) {
        val upIsMasked = f(x,y-1)
        
        if(upIsMasked && !isMasked) {
          // Moving up will unmask
          unmaskedAfterMoveUp.add(y,x)
        } else if(!upIsMasked && isMasked) {
          // Moving up will mask
          maskedAfterMoveUp.add(y,x)
        }
      }
      x += 1
    }
    y += 1
  }
 
  def foreachX(row:Int)(f:Int=>Unit) = {
    unmasked.foreachAt(row)(f)
  }
 
  def foreachWestColumn(f:Int=>Unit) = {
    val len = westColumnUnmasked(0)
    if(len > 0) {
      var y = 1
      while(y <= len) {
        f(westColumnUnmasked(y))
        y += 1
      }
    }
  }

  def foreachEastColumn(f:Int=>Unit) = {
    val len = eastColumnUnmasked(0)
    if(len > 0) {
      var y = 1
      while(y <= len) {
        f(eastColumnUnmasked(y))
        y += 1
      }
    }
  }

  private def foreach(xOffset:Int,yOffset:Int,startY:Int,set:MaskSet)(f:(Int,Int)=>Unit) = {
    var y = startY
    while(y < d) {
      set.foreachAt(y) { x => f(x - xOffset, y - yOffset) }
      y += 1
    }
  }

  def foreachMasked(mv:Movement)(f:(Int,Int)=>Unit) {
    mv match {
      case Left => foreach(0,0,0,maskedAfterMoveLeft)(f)
      case Right => foreach(1,0,0,unmaskedAfterMoveLeft)(f)
      case Up => foreach(0,0,1,maskedAfterMoveUp)(f)
      case Down => foreach(0,1,1,unmaskedAfterMoveUp)(f)
      case _ =>
    }
  }

  def foreachUnmasked(mv:Movement)(f:(Int,Int)=>Unit) {
    mv match {
      case Left => foreach(0,0,0,unmaskedAfterMoveLeft)(f)
      case Right => foreach(1,0,0,maskedAfterMoveLeft)(f)
      case Up => foreach(0,0,1,unmaskedAfterMoveUp)(f)
      case Down => foreach(0,1,1,maskedAfterMoveUp)(f)
      case _ =>
    }
  }

  def asciiDraw:String = {
    import scala.collection.mutable.Set
    val s = Set[Int]()
    var r = ""
    for(y <- 0 to d - 1) {
      s.clear()
      unmasked.foreachAt(y) { x => s += x }
      for(x <- 0 to d - 1) {
        if(s.contains(x)) { r += " O " } else { r += " X " }
      }
      r += "\n"
    }
    r
  }
}

