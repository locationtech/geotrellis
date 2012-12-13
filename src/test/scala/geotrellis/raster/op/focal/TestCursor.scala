package geotrellis.raster.op.focal

import geotrellis._

object TestCursor {
  /*
   * Allows you to write a cursor or cursor mask using a string,
   * by using X as the masked values, 0 (or whatever) for the
   * unmasked values.
   * 
   * example:
   * val cursor = TestCursor.maskFromString("""
   *                X 0 X 0 X
   *                0 X 0 X 0
   *                X 0 X 0 X        Can describe the cursor here!
   *                0 X 0 X 0
   *                X 0 X 0 X
   *                                        """)
   */
  private def getActualLines(s:String) = {
    s.split("\n")
     .map { row => row.filter { c => Seq('X','0').contains(c) } }
     .filter { x => x != "" }
  }
  
  def maskFuncFromString(s:String) = {
    val actualLines = getActualLines(s)
    val d = actualLines.length
    val arr = Array.ofDim[Boolean](d,d)
    var x = 0; var y = 0

    for(row <- actualLines) {
      x = 0
      for(ch <- row) {
        if(x >= d) { throw new Exception("You can't have the X's be greater than the Y's!") }
        if(ch == 'X') {
          arr(y)(x) = true
        }
        x += 1
      }
      y += 1
    }
    (x:Int,y:Int) => arr(y)(x)
  }

  def maskFromString(s:String) = {
    new CursorMask(getActualLines(s).length, maskFuncFromString(s))
  }

  def fromString(r:Raster, s:String) = {
    val actualLines = getActualLines(s)
    if(actualLines.length % 2 == 0) {
      throw new Exception("Can't create a cursor with even dimension!")
    }
    val d = ((actualLines.length - 1)/2).toInt
    val c = new Cursor(r.cols,r.rows,d)
    c.setMask(maskFuncFromString(s))
    c
  }
}
