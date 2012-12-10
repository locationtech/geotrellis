package geotrellis.raster.op.focal

import geotrellis._
import scala.math._

class FocalOp[T](r:Op[Raster],n:Op[Neighborhood])
                (getCalc:(Raster,Neighborhood)=>CalculationResult[T] with Initialization 
                                                                     with FocalCalculation)
  extends FocalOperation[T](r,n) {
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

class FocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A])
                   (getCalc:(Raster,Neighborhood)=>CalculationResult[T] with Initialization1[A]
                                                                        with FocalCalculation) 
  extends FocalOperation1[A,T](r,n,a){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

class FocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B])
                     (getCalc:(Raster,Neighborhood)=>CalculationResult[T] with Initialization2[A,B]
                                                                          with FocalCalculation) 
  extends FocalOperation2[A,B,T](r,n,a,b){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

class FocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C])
                       (getCalc:(Raster,Neighborhood)=>CalculationResult[T] with Initialization3[A,B,C]
                                                                            with FocalCalculation) 
  extends FocalOperation3[A,B,C,T](r,n,a,b,c){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

class FocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D])
                         (getCalc:(Raster,Neighborhood)=>CalculationResult[T] with Initialization4[A,B,C,D]
                                                                              with FocalCalculation) 
  extends FocalOperation4[A,B,C,D,T](r,n,a,b,c,d){
  def getCalculation(r:Raster,n:Neighborhood) = { getCalc(r,n) }
}

/* Two arguments (the raster and neighborhoood) */

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
      Result(calc.getResult)
  }
  
  def getCalculation(r:Raster,n:Neighborhood):CalculationResult[T] with Initialization 
                                                                   with FocalCalculation
}

/* Three arguments (the raster and neighborhoood and another) */

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
      Result(calc.getResult)
  }
  
  def getCalculation(r:Raster,n:Neighborhood):CalculationResult[T] with Initialization1[A]
                                                                   with FocalCalculation
}

/* Four arguments (the raster and neighborhoood and two others) */

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
      Result(calc.getResult)
  }
  
  def getCalculation(r:Raster,n:Neighborhood):CalculationResult[T] with Initialization2[A,B]
                                                                   with FocalCalculation
}

/* Five arguments (the raster and neighborhoood and three others) */

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
      Result(calc.getResult)
  }
  
  def getCalculation(r:Raster,n:Neighborhood):CalculationResult[T] with Initialization3[A,B,C]
                                                                   with FocalCalculation
}

/* Six arguments (the raster and neighborhoood and four others) */

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
      Result(calc.getResult)
  }
  
  def getCalculation(r:Raster,n:Neighborhood):CalculationResult[T] with Initialization4[A,B,C,D]
                                                                   with FocalCalculation
}
