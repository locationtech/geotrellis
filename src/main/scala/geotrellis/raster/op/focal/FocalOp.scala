package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._

abstract class FocalOp1(r:Op[Raster],n:Neighborhood) extends Operation[Raster] {
  def _run(context:Context) = runAsync(List('init,r))
  def productArity = 1
  def canEqual(other:Any) = other.isInstanceOf[FocalOp1]
  def productElement(n:Int) = if (n == 0) r else throw new IndexOutOfBoundsException()
  val nextSteps:PartialFunction[Any,StepOutput[Raster]] = {
    case 'init :: (r:Raster) :: Nil => execute(r,n)
  }
  
  def execute(r:Raster, n:Neighborhood):Result[Raster]
}

abstract class CursorFocalOp1[@specialized(Int,Double)D](r:Op[Raster],n:Neighborhood) extends FocalOp1(r,n) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n))(calc))

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):RasterBuilder[D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntCursorFocalOp1(r:Op[Raster],n:Neighborhood) extends CursorFocalOp1[Int](r,n) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)
}

abstract class DoubleCursorFocalOp1(r:Op[Raster],n:Neighborhood) extends CursorFocalOp1[Double](r,n) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)
}

abstract class CellwiseFocalOp1[@specialized(Int,Double) D](r:Op[Raster],n:Neighborhood) 
         extends FocalOp1(r,n) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):RasterBuilder[D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}

abstract class IntCellwiseFocalOp1(r:Op[Raster],n:Neighborhood) extends CellwiseFocalOp1[Int](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)
}

abstract class DoubleCellwiseFocalOp1(r:Op[Raster],n:Neighborhood) extends CellwiseFocalOp1[Double](r,n) {
  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)
}

object FocalOp {
  /**
   * Allows for easy definition of focal operations using the op syntax. See
   * geotrellis.raster.op.focal.Sum for an example.
   *
   * @param r Input Raster of the operation.
   * @param s FocalStrategyType that should be used.
   * @param n Neighborhood type (e.g. Square(2), Circle(5), etc)
   * @param dfn The FocalOpDefinition that defines the FocalOpData and FocalCalculation
   *            for this operation.
   */
  def getResult(r:Raster, s:FocalStrategyType, n:Neighborhood, dfn: FocalOpDefinition) = {
    dfn match {
      case d: IntFocalOpDefinition =>
	val strategy = FocalStrategy.get[Int](s,n)
	Result(strategy.handle(r, d.newData(r), () => d.newCalc))
      case d: DoubleFocalOpDefinition =>
	val strategy = FocalStrategy.get[Double](s,n)
        Result(strategy.handle(r, d.newData(r), () => d.newCalc))
      case d: MultiTypeFocalOpDefinition => 
	r.data.getType match {
	  case TypeBit | TypeByte | TypeShort | TypeInt =>
	    val strategy = FocalStrategy.get[Int](s,n)
	    Result(strategy.handle(r, d.newIntData(r), () => d.newIntCalc))
	  case _ =>
	    val strategy = FocalStrategy.get[Double](s, n)
	    Result(strategy.handle(r, d.newDoubleData(r), () => d.newDoubleCalc))
	}
    }
  }
  
  /** Convienence function that allows you to just supply a function that supplies
   *  a new FocalCalculation[Int] */
  def getResultInt(r:Raster, 
		 s:FocalStrategyType, 
		 n:Neighborhood, 
		 newCalcFunc: () => FocalCalculation[Int]):Result[Raster] = {
    val dfn = new IntFocalOpDefinition { def newCalc = newCalcFunc() }
    getResult(r,s,n,dfn)
  }

  /** Convienence function that allows you to just supply a function that supplies
   *  a new FocalCalculation[Double] */
  def getResultDouble(r:Raster, 
		 s:FocalStrategyType, 
		 n:Neighborhood, 
		 newCalcFunc: () => FocalCalculation[Double]):Result[Raster] = {
    val dfn = new DoubleFocalOpDefinition { def newCalc = newCalcFunc() }
    getResult(r,s,n,dfn)
  }
}
