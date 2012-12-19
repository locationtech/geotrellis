package geotrellis.raster.op.focal

import geotrellis._
import scala.math._
import geotrellis.raster.CroppedRaster

/**
 * Focal Operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        reOpt       Optional raster that represents the analysis area.
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
class FocalOp[T](r:Op[Raster],n:Op[Neighborhood],reOpt:Op[Option[RasterExtent]] = Literal(None))
                (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization)                  
  extends FocalOperation[T](r,n) {
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        a           Argument of type A.
 * @param        reOpt       Optional raster that represents the analysis area.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],reOpt:Op[Option[RasterExtent]] = Literal(None))
                   (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization1[A])
  extends FocalOperation1[A,T](r,n,a){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and two other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        reOpt       Optional raster that represents the analysis area.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],reOpt:Op[Option[RasterExtent]] = Literal(None))
                     (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization2[A,B])
  extends FocalOperation2[A,B,T](r,n,a,b){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and three other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        reOpt       Optional raster that represents the analysis area.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],reOpt:Op[Option[RasterExtent]] = Literal(None))
                       (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization3[A,B,C])
  extends FocalOperation3[A,B,C,T](r,n,a,b,c){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and four other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        d           Argument of type D.
 * @param        reOpt       Optional raster that represents the analysis area.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D],reOpt:Op[Option[RasterExtent]] = Literal(None))
                         (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization4[A,B,C,D])
  extends FocalOperation4[A,B,C,D,T](r,n,a,b,c,d){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

trait HasAnalysisArea[SELF <: FocalOperationBase] extends Cloneable { this:SELF =>
  def makeClone() = super.clone().asInstanceOf[SELF]
  def setAnalysisArea(op:Operation[Option[RasterExtent]]) = {
    val clone = this.makeClone()
    clone.analysisAreaOp = op
    clone
  }
}

/* Two arguments (the raster and neighborhoood) */

trait FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]]
} 

/**
 * Base class for a focal operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation[T](r:Op[Raster],n:Op[Neighborhood],analysisArea:Op[Option[RasterExtent]] = Literal(None)) extends Operation[T] with FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]] = analysisArea
  def _run(context:Context) = runAsync(List('init,r,n,analysisAreaOp))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation[_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => analysisArea
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: (_reOpt:Option[_]) :: Nil => 
      val reOpt = _reOpt.asInstanceOf[Option[RasterExtent]]
      val calc = getCalculation(r,n)
      calc.init(r, reOpt)
      calc.execute(r,n,reOpt)
      Result(calc.getResult)
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

case class AnalysisArea(colMin:Int, rowMin:Int, colMax:Int, rowMax:Int, rasterExtent:RasterExtent)

object FocalOperation {
    def calculateAnalysisArea(r:Raster,reOpt:Option[RasterExtent]) = 
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
        val (colMin, rowMin) = CroppedRaster.findUpperLeft(inputRE, e.xmin, e.ymax) // north-west
        val (colMax, rowMax) = CroppedRaster.findLowerRight(inputRE, e.xmax, e.ymin) // south-east
       
        // colMax & rowMax of analysis area are inclusive, unlike results
        // from CroppedRaster which are exclusive of the max bound,
        // so we subtract 1 from each.
        AnalysisArea(colMin, rowMin, colMax - 1, rowMax - 1, re)
      }
    }  
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        reOpt       Optional raster that represents the analysis area.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],analysisArea:Op[Option[RasterExtent]]=None) extends Operation[T] with FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]] = analysisArea
  def _run(context:Context) = runAsync(List('init,r,n,a,analysisAreaOp))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation1[_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: (_analysisArea:Option[_]) :: Nil => 
      val analysisArea = _analysisArea.asInstanceOf[Option[RasterExtent]]
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],analysisArea)
      calc.execute(r,n,analysisArea)
      Result(calc.getResult)
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
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        reOpt       Optional raster that represents the analysis area.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],analysisArea:Op[Option[RasterExtent]]=None) 
         extends Operation[T] with FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]] = analysisArea
  def _run(context:Context) = runAsync(List('init,r,n,a,b,analysisAreaOp))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation2[_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => analysisArea
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: (_analysisArea:Option[_]) :: Nil => 
      val analysisArea = _analysisArea.asInstanceOf[Option[RasterExtent]]
      val calc = getCalculation(r,n)
      calc.init(r,
                a.asInstanceOf[A],
                b.asInstanceOf[B],
                analysisArea
      )
      calc.execute(r,n,analysisArea)
      Result(calc.getResult)
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
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        reOpt       Optional raster that represents the analysis area.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],analysisArea:Op[Option[RasterExtent]]=None) 
         extends Operation[T] with FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]] = analysisArea
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c,analysisAreaOp))
  def productArity = 6
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation3[_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => c
    case 5 => analysisArea
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: c :: (_analysisArea:Option[_]) :: Nil => 
      val analysisArea = _analysisArea.asInstanceOf[Option[RasterExtent]]
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                b.asInstanceOf[B],
                c.asInstanceOf[C],
                analysisArea)
      calc.execute(r,n,analysisArea)
      Result(calc.getResult)
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
 * @param        a           Argument of type A.
 * @param        b           Argument of type B.
 * @param        c           Argument of type C.
 * @param        d           Argument of type D.
 * @param        reOpt       Optional raster that represents the analysis area.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D],analysisArea:Op[Option[RasterExtent]]=None) 
         extends Operation[T] with FocalOperationBase {
  var analysisAreaOp:Operation[Option[RasterExtent]] = analysisArea
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c,d,analysisAreaOp))
  def productArity = 7
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation4[_,_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => c
    case 5 => d
    case 6 => analysisArea
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: c :: d :: (_analysisArea:Option[_]) :: Nil => 
      val analysisArea = _analysisArea.asInstanceOf[Option[RasterExtent]]
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                  b.asInstanceOf[B],
                  c.asInstanceOf[C],
                  d.asInstanceOf[D],
                  analysisArea)
      calc.execute(r,n,analysisArea)
      Result(calc.getResult)
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
