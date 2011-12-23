package trellis.operation
/**
  * Perform a function on every cell in a raster with the values from another raster.
  *
  * For example,
  * <pre>
  * val A = LoadFile(a)
  * val B = LoadFile(b)
  * 
  * // Generate a raster by adding the values of each cell in A and B 
  * val D = BinaryDoCell(R, (a, b) => a + b )
  * </pre>
  */

case class BinaryDoCell(r1:IntRasterOperation, r2:IntRasterOperation,
                        localFunction: (Int, Int) => Int ) extends BinaryLocal {
  val identity1 = 0
  val identity2 = 0

  @inline
  def handleCells(z1:Int, z2:Int) = { localFunction(z1, z2) }
}
