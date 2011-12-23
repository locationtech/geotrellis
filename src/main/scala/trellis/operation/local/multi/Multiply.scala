package trellis.operation

case class Multiply(rs:IntRasterOperation*) extends MultiLocal {
  override val identity = 1
  @inline
  def handleCells2(a:Int, b:Int) = { a * b }
  //@inline
  //override def handleCells3(a:Int, b:Int, c:Int) = { a * b * c }
  //@inline
  //override def handleCells4(a:Int, b:Int, c:Int, d:Int) = { a * b * c * d }
  //@inline
  //override def handleCells5(a:Int, b:Int, c:Int, d:Int, e:Int) = { a * b * c * d * e}
}
