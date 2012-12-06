package geotrellis.raster.op.focal

import geotrellis._

/*
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
trait FocalCalculation {
  def execute(r:Raster, n:Neighborhood):Unit
}

/*
 * A focal calculation that uses the Cursor focal strategy.
 */
trait CursorCalculation extends FocalCalculation {
  def traversalStrategy:Option[TraversalStrategy] = None
  def execute(r:Raster,n:Neighborhood):Unit = {
    traversalStrategy match {
      case Some(t) => CursorStrategy.execute(r,Cursor(r,n),this,t)
      case None => CursorStrategy.execute(r,Cursor(r,n),this)
    }
  }
  def calc(r:Raster,cur:Cursor):Unit
}

/*
 * A focal calculation that uses the Cellwise focal strategy
 */
trait CellwiseCalculation extends FocalCalculation {
  def execute(r:Raster,n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset():Unit
  def setValue(x:Int,y:Int)
}

/*
 * Trait defining the ability to initialize the focal calculation
 * with a range of variables.
 */

trait Initialization          { def init(r:Raster):Unit }
trait Initialization1[A]       { def init(r:Raster,a:A):Unit }
trait Initialization2[A,B]     { def init(r:Raster,a:A,b:B):Unit }
trait Initialization3[A,B,C]   { def init(r:Raster,a:A,b:B,c:C):Unit }
trait Initialization4[A,B,C,D] { def init(r:Raster,a:A,b:B,c:C,d:D):Unit }

/*
 * Trait defining the type and method for getting a result out
 * of a focal calculation.
 */

trait CalculationResult[T] { def getResult:T }

/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array data through the 
 * 'data' member. Initialize the data through the
 * initData method.
 */

trait BitRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:BitArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = BitArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

trait ByteRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:ByteArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ByteArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

trait ShortRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:ShortArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ShortArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

trait IntRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:IntArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = IntArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

trait FloatRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:FloatArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = FloatArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

trait DoubleRasterDataResult extends CalculationResult[Raster] with Initialization {
  var data:DoubleArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = DoubleArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}
