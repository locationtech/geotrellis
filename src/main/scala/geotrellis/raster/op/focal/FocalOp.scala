package geotrellis.raster.op.focal

import geotrellis._
import scala.math._

/**
 * Focal Operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
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
class FocalOp[T](r:Op[Raster],n:Op[Neighborhood])
                (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization)                  
  extends FocalOperation[T](r,n) {
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A])
                   (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization1[A])
  extends FocalOperation1[A,T](r,n,a){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and two other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B])
                     (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization2[A,B])
  extends FocalOperation2[A,B,T](r,n,a,b){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/**
 * Focal Operation that takes a raster, a neighborhood, and three other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C])
                       (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization3[A,B,C])
  extends FocalOperation3[A,B,C,T](r,n,a,b,c){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}



/**
 * Focal Operation that takes a raster, a neighborhood, and four other arguments.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 * @param        getCalc     See notes for same parameter in [[FocalOp]]
 *
 * @tparam       T           Return type of the Operation.
 */
class FocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D])
                         (getCalc:(Raster,Neighborhood)=>FocalCalculation[T] with Initialization4[A,B,C,D])
  extends FocalOperation4[A,B,C,D,T](r,n,a,b,c,d){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/* Two arguments (the raster and neighborhoood) */

/**
 * Base class for a focal operation that takes a raster and a neighborhood.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation[T](r:Op[Raster],n:Op[Neighborhood]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n))
  def productArity = 2
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation[_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r)
      calc.execute(r,n)
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
  def getCalculation(r:Raster,n:Neighborhood):FocalCalculation[T] with Initialization 
}

/**
 * Base class for a focal operation that takes a raster, a neighborhood, and one other argument.
 *
 * @param        r           Raster the focal operation will run against.
 * @param        n           Neighborhood to use with this focal operation.
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation1[_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A])
      calc.execute(r,n)
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
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b))
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation2[_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                  b.asInstanceOf[B])
      calc.execute(r,n)
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
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation3[_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => c
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: c :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                b.asInstanceOf[B],
                c.asInstanceOf[C])
      calc.execute(r,n)
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
 *
 * @tparam       T           Return type of the Operation.
 */
abstract class FocalOperation4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c,d))
  def productArity = 6
  def canEqual(other:Any) = other.isInstanceOf[FocalOperation4[_,_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => c
    case 5 => d
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: c :: d :: Nil => 
      val calc = getCalculation(r,n)
      calc.init(r,a.asInstanceOf[A],
                  b.asInstanceOf[B],
                  c.asInstanceOf[C],
                  d.asInstanceOf[D])
      calc.execute(r,n)
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
