package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

/**
 * Declares that implementers have a result
 */
trait Resulting[T] {
  def result:T
}

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
trait FocalCalculation[T] extends Resulting[T] {
  /**
   * @param re	Optional extent of the analysis area (where the focal operation will be executed)
   */
  def execute(r:Raster, n:Neighborhood, neighbors:Seq[Option[Raster]]):Unit
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
trait CursorCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy:Option[TraversalStrategy] = None
  def execute(r:Raster,n:Neighborhood,neighbors:Seq[Option[Raster]]):Unit = 
    CursorStrategy.execute(r,n,this,traversalStrategy,neighbors)
  
  def calc(r:Raster,cur:Cursor):Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
trait CellwiseCalculation[T] extends FocalCalculation[T] {
  def traversalStrategy:Option[TraversalStrategy] = None
  def execute(r:Raster,n:Neighborhood, neighbors:Seq[Option[Raster]]) = n match {
      case s:Square => CellwiseStrategy.execute(r,s,this,traversalStrategy,neighbors)
      case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
    }
  
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset():Unit
  def setValue(x:Int,y:Int)
}

/*
 * Trait defining the ability to initialize the focal calculation
 * with a range of variables.
 */

/** Trait defining the ability to initialize the focal calculation with a raster. */
trait Initialization { 
  def init(r:Raster):Unit 

  // def getRasterExtent(r:Raster, reOpt:Option[RasterExtent]):RasterExtent = reOpt match {
  // 	case None => r.rasterExtent
  // 	case Some(re) => re 
  // }  
}

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
trait BitRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[BitArrayRasterData]] that will be returned by the focal calculation */
  var data:BitArrayRasterData = null
  var rasterExtent:RasterExtent = null


  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = BitArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[ByteArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait ByteRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[ByteArrayRasterData]] that will be returned by the focal calculation */
  var data:ByteArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ByteArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[ShortArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait ShortRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[ShortArrayRasterData]] that will be returned by the focal calculation */
  var data:ShortArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ShortArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[IntArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait IntRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[IntArrayRasterData]] that will be returned by the focal calculation */
  var data:IntArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = IntArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[FloatArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait FloatRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[FloatArrayRasterData]] that will be returned by the focal calculation */
  var data:FloatArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = FloatArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}

/**
 * Defines a focal calculation as returning
 * a [[Raster]] with [[DoubleArrayRasterData]], and defines
 * the [[Initialization]].init function for setting up the data.
 */
trait DoubleRasterDataResult extends Initialization with Resulting[Raster] {
  /** [[DoubleArrayRasterData]] that will be returned by the focal calculation */
  var data:DoubleArrayRasterData = null
  var rasterExtent:RasterExtent = null

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = DoubleArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)
  }

  def result = Raster(data,rasterExtent)
}
