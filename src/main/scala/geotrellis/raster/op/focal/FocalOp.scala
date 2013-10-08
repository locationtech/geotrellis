package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._
import geotrellis.raster.CroppedRaster

/**
 * Focal Operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        getCalc     Function that returns a [[FocalCalculation]] based
 *                           on the raster and neighborhood. This allows flexibility
 *                           in what calculation to use; if some calculations are faster
 *                           for some neighborhoods (e.g., using a [[CellwiseCalculation]]
 *                           for [[Square]] neighborhoods and a [[CursorCalculation]] for
 *                           all other neighborhoods), or if you want to change the calculation
 *                           based on the raster's data type, you can do so by returning the
 *                           correct [[FocalCalculation]] from this function.
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp[T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors])
                (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization)                  
extends FocalOperation0[T](r,n,tns) {
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],a:Op[A])
                   (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization1[A])
extends FocalOperation1[A,T](r,n,tns,a){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and two other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],
                      a:Op[A],b:Op[B])
                     (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization2[A,B])
extends FocalOperation2[A,B,T](r,n,tns,a,b){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and three other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],
                        a:Op[A],b:Op[B],c:Op[C])
                       (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization3[A,B,C])
extends FocalOperation3[A,B,C,T](r,n,tns,a,b,c){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and four other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        d           Argument of type D.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],a:Op[A],b:Op[B],c:Op[C],d:Op[D])
                         (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization4[A,B,C,D])
extends FocalOperation4[A,B,C,D,T](r,n,tns,a,b,c,d){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

case class AnalysisArea(colMin:Int, rowMin:Int, colMax:Int, rowMax:Int, rasterExtent:RasterExtent)

object AnalysisArea {
  def apply(r:Raster,reOpt:Option[RasterExtent]):AnalysisArea = 
    reOpt match {
      case None => {
        AnalysisArea(0, 0, r.cols - 1, r.rows - 1, r.rasterExtent)
      }
      case Some(re) => {
        val inputRE = r.rasterExtent
        val e = re.extent
        // calculate our bounds in terms of parent bounds
        if (re.cellwidth != inputRE.cellwidth || re.cellheight != inputRE.cellheight) {
          throw new Exception("Cell size of analysis area must match the input raster")
        }
        
        // translate the upper-left (xmin/ymax) and lower-right (xmax/ymin) points
        val GridBounds(colMin,rowMin,colMax,rowMax) = inputRE.gridBoundsFor(e)
        AnalysisArea(colMin, rowMin, colMax, rowMax, re)
      }
    }  

  def apply(r:Raster):AnalysisArea = apply(r,None)
}

object TileWithNeighbors {
  def apply(r:Raster,neighbors:Seq[Option[Raster]]) = 
    if(neighbors.isEmpty) {
      r
    } else {
      val re =
        neighbors.flatten
          .map(_.rasterExtent)
          .reduceLeft((re1,re2) => re1.combine(re2))

      val tileCols = 1 +
      // East Column
      neighbors.slice(1,4).map { case Some(_) => 1; case None => 0 }.reduce(_*_) +
      // West Column
      neighbors.slice(5,8).map { case Some(_) => 1; case None => 0 }.reduce(_*_)

      val tileRows = 1 +
      // North Row
        (neighbors(7) :: neighbors.slice(0,2).toList).map { case Some(_) => 1; case None => 0 }.reduce(_*_) +
      // South Row
      neighbors.slice(3,6).map { case Some(_) => 1; case None => 0 }.reduce(_*_)

      val nTileLayout = TileLayout(tileCols, tileRows, r.rasterExtent.cols, r.rasterExtent.rows)

      Raster(new TileArrayRasterData(Array(
        neighbors(7),
        neighbors(0),
        neighbors(1),
        neighbors(6),
        Some(r),
        neighbors(2),
        neighbors(5),
        neighbors(4),
        neighbors(3)
      ).flatten, nTileLayout),re)
    }
}

trait FocalOperation[T] extends Operation[T]

/**
 * Base class for a focal operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation0[T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) 
         extends FocalOperation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,tns.flatMap(_.getNeighbors)))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation0[_]]
  def productElement(index:Int) = index match {
    case 0 => r
    case 1 => n
    case 2 => tns
    case _ => new IndexOutOfBoundsException()
  }


  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (reOpt:Option[_]) :: (neighbors:Seq[_]) :: Nil =>  {
      val calc = getCalculation(r,n)
      calc.init(r)
      calc.execute(r,n,neighbors.asInstanceOf[Seq[Option[Raster]]])
      Result(calc.result)
    }
  }

  /** Gets a calculation to be used with this focal operation for the given raster
   * neighborhood.
   *
   * Choosing the calculation based on on the raster and neighborhood allows flexibility
   * in what calculation to use; if some calculations are faster
   * for some neighborhoods (e.g., using a [[CellwiseCalculation]]
   * for [[Square]] neighborhoods and a [[CursorCalculation]] for
   * all other neighborhoods), or if you want to change the calculation
   * based on the raster's data type, you can do so by returning the
   * correct [[FocalCalculation]] from this function.
   *
   * @param     r       Raster that the focal calculation will run against.
   * @param     n       Neighborhood that will be used in the focal operation.
   */
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization 
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation1[A,T](r:Op[Raster],n:Op[Neighborhood], tns:Op[TileNeighbors], a:Op[A]) 
    extends FocalOperation[T] {
  var rasterOp = r
  def _run(context:Context) = runAsync(List('init,rasterOp,n,tns.flatMap(_.getNeighbors),a))
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation1[_,_]]
  def productElement(index:Int) = index match {
    case 0 => r
    case 1 => n
    case 2 => tns
    case 3 => a
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (neighbors:Seq[_]) :: a :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A])
      calc.execute(r,n,neighbors.asInstanceOf[Seq[Option[Raster]]])
      Result(calc.result)
  }

  /** Gets a calculation to be used with this focal operation for the given raster
   * neighborhood.
   *
   * Choosing the calculation based on on the raster and neighborhood allows flexibility
   * in what calculation to use; if some calculations are faster
   * for some neighborhoods (e.g., using a [[CellwiseCalculation]]
   * for [[Square]] neighborhoods and a [[CursorCalculation]] for
   * all other neighborhoods), or if you want to change the calculation
   * based on the raster's data type, you can do so by returning the
   * correct [[FocalCalculation]] from this function.
   *
   * @param     r       Raster that the focal calculation will run against.
   * @param     n       Neighborhood that will be used in the focal operation.
   */  
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization1[A]
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and two other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation2[A,B,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],
                                      a:Op[A],b:Op[B])
         extends FocalOperation[T] {
  var rasterOp = r
  def _run(context:Context) = runAsync(List('init,rasterOp,n,tns.flatMap(_.getNeighbors),a,b))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation2[_,_,_]]
  def productElement(index:Int) = index match {
    case 0 => r
    case 1 => n
    case 2 => tns
    case 3 => a
    case 4 => b
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (neighbors:Seq[_]) :: a :: b :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,
                a.asInstanceOf[A],
                b.asInstanceOf[B])
      calc.execute(r,n,neighbors.asInstanceOf[Seq[Option[Raster]]])
      Result(calc.result)
  }

  /** Gets a calculation to be used with this focal operation for the given raster
   * neighborhood.
   *
   * Choosing the calculation based on on the raster and neighborhood allows flexibility
   * in what calculation to use; if some calculations are faster
   * for some neighborhoods (e.g., using a [[CellwiseCalculation]]
   * for [[Square]] neighborhoods and a [[CursorCalculation]] for
   * all other neighborhoods), or if you want to change the calculation
   * based on the raster's data type, you can do so by returning the
   * correct [[FocalCalculation]] from this function.
   *
   * @param     r       Raster that the focal calculation will run against.
   * @param     n       Neighborhood that will be used in the focal operation.
   */  
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization2[A,B]
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and three other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],
                                        a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOperation[T] {
  var rasterOp:Operation[Raster] = r 
  def _run(context:Context) = runAsync(List('init,rasterOp,n,tns.flatMap(_.getNeighbors),a,b,c))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation3[_,_,_,_]]
  def productElement(index:Int) = index match {
    case 0 => r
    case 1 => n
    case 2 => tns
    case 3 => a
    case 4 => b
    case 5 => c
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (neighbors:Seq[_]) :: a :: b :: c :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                b.asInstanceOf[B],
                c.asInstanceOf[C])
      calc.execute(r,n,neighbors.asInstanceOf[Seq[Option[Raster]]])
      Result(calc.result)
  }
  
  /** Gets a calculation to be used with this focal operation for the given raster
   * neighborhood.
   *
   * Choosing the calculation based on on the raster and neighborhood allows flexibility
   * in what calculation to use; if some calculations are faster
   * for some neighborhoods (e.g., using a [[CellwiseCalculation]]
   * for [[Square]] neighborhoods and a [[CursorCalculation]] for
   * all other neighborhoods), or if you want to change the calculation
   * based on the raster's data type, you can do so by returning the
   * correct [[FocalCalculation]] from this function.
   *
   * @param     r       Raster that the focal calculation will run against.
   * @param     n       Neighborhood that will be used in the focal operation.
   */
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization3[A,B,C]
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and four other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        tns         TileNeighbors that describe the neighboring tiles.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        d           Argument of type D.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors],
                                          a:Op[A],b:Op[B],c:Op[C],d:Op[D])
         extends FocalOperation[T] {
  var rasterOp = r
  def _run(context:Context) = runAsync(List('init,rasterOp,n,tns.flatMap(_.getNeighbors),a,b,c,d))
  def productArity = 6
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation4[_,_,_,_,_]]
  def productElement(index:Int) = index match {
    case 0 => r
    case 1 => n
    case 2 => tns
    case 3 => a
    case 4 => b
    case 5 => c
    case 6 => d
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (neighbors:Seq[_]) :: a :: b :: c :: d :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                  b.asInstanceOf[B],
                  c.asInstanceOf[C],
                  d.asInstanceOf[D])
      calc.execute(r,n,neighbors.asInstanceOf[Seq[Option[Raster]]])
      Result(calc.result)
  }
  
  /** Gets a calculation to be used with this focal operation for the given raster
   * neighborhood.
   *
   * Choosing the calculation based on on the raster and neighborhood allows flexibility
   * in what calculation to use; if some calculations are faster
   * for some neighborhoods (e.g., using a [[CellwiseCalculation]]
   * for [[Square]] neighborhoods and a [[CursorCalculation]] for
   * all other neighborhoods), or if you want to change the calculation
   * based on the raster's data type, you can do so by returning the
   * correct [[FocalCalculation]] from this function.
   *
   * @param     r       Raster that the focal calculation will run against.
   * @param     n       Neighborhood that will be used in the focal operation.
   */
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization4[A,B,C,D]
}
