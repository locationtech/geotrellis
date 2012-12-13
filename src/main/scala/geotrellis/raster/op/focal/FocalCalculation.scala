package geotrellis.raster.op.focal

import geotrellis._

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
trait FocalCalculation[T] {
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
trait CursorCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy:Option[TraversalStrategy] = None
  def execute(r:Raster,n:Neighborhood):Unit = {
    traversalStrategy match {
      case Some(t) => CursorStrategy.execute(r,Cursor(r,n),this,t)
      case None => CursorStrategy.execute(r,Cursor(r,n),this)
    }
  }
  def calc(r:Raster,cur:Cursor):Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
trait CellwiseCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy:Option[TraversalStrategy] = None
  def execute(r:Raster,n:Neighborhood) = {
    n match {
      case s:Square => 
        traversalStrategy match {
          case Some(t) => CellwiseStrategy.execute(r,s,this,t)
          case None => CellwiseStrategy.execute(r,s,this)
        }
      case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
    }
  }
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset():Unit
  def setValue(x:Int,y:Int)
}

/** Trait defining the ability to initialize the focal calculation with a raster. */
trait Initialization           { def init(r:Raster):Unit }
/** Trait defining the ability to initialize the focal calculation with a raster and one other parameter. */
trait Initialization1[A]       { def init(r:Raster,a:A):Unit }
/** Trait defining the ability to initialize the focal calculation with a raster and two other parameters. */
trait Initialization2[A,B]     { def init(r:Raster,a:A,b:B):Unit }
/** Trait defining the ability to initialize the focal calculation with a raster and three other parameters. */
trait Initialization3[A,B,C]   { def init(r:Raster,a:A,b:B,c:C):Unit }
/** Trait defining the ability to initialize the focal calculation with a raster and four other parameters. */
trait Initialization4[A,B,C,D] { def init(r:Raster,a:A,b:B,c:C,d:D):Unit }

/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array data through the 
 * 'data' member.
 */

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[BitArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait BitRasterDataResult extends Initialization {
  /** [[BitArrayRasterData]] that will be returned by the focal calculation */
  var data:BitArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = BitArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[ByteArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait ByteRasterDataResult extends Initialization {
  /** [[ByteArrayRasterData]] that will be returned by the focal calculation */
  var data:ByteArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ByteArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[ShortArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait ShortRasterDataResult extends Initialization {
  /** [[ShortArrayRasterData]] that will be returned by the focal calculation */
  var data:ShortArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ShortArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[IntArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait IntRasterDataResult extends Initialization {
  /** [[IntArrayRasterData]] that will be returned by the focal calculation */
  var data:IntArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = IntArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[FloatArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait FloatRasterDataResult extends Initialization {
  /** [[FloatArrayRasterData]] that will be returned by the focal calculation */
  var data:FloatArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = FloatArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[DoubleArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait DoubleRasterDataResult extends Initialization {
  /** [[DoubleArrayRasterData]] that will be returned by the focal calculation */
  var data:DoubleArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = DoubleArrayRasterData.ofDim(rasterExtent.cols,rasterExtent.rows)
  }

  def getResult = Raster(data,rasterExtent)
}
