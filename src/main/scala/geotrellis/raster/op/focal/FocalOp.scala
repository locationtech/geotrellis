 package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._

/* Two arguments (the raster and neighborhoood) */

abstract class FocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n))
  def productArity = 2
  def canEqual(other:Any) = other.isInstanceOf[FocalOp[_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: Nil => 
      init(r)
      execute(r,n)
      Result(getResult)
  }
  
  def init(r:Raster):Unit
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

abstract class CursorFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[T](r,n) {
  def execute(r:Raster,n:Neighborhood) = CursorStrategy.execute(r,Cursor(r,n))(calc)
  def calc(r:Raster,cursor:Cursor):Unit
}

abstract class CellwiseFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) 
         extends FocalOp[T](r,n) with CellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
}

/* Three arguments (the raster and neighborhoood and another) */

abstract class FocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[FocalOp1[_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: Nil => 
      init(r,a.asInstanceOf[A])
      execute(r,n)
      Result(getResult)
  }
  
  def init(r:Raster,a:A):Unit
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

abstract class CursorFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) {
  def execute(r:Raster,n:Neighborhood) = CursorStrategy.execute(r,Cursor(r,n))(calc)
  def calc(r:Raster,cursor:Cursor):Unit
}

abstract class CellwiseFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) with CellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
}

/* Four arguments (the raster and neighborhoood and two others) */

abstract class FocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b))
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[FocalOp2[_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: Nil => 
      init(r,a.asInstanceOf[A],b.asInstanceOf[B])
      execute(r,n)
      Result(getResult)
  }
  
  def init(r:Raster,a:A,b:B):Unit
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

abstract class CursorFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) {
  def execute(r:Raster,n:Neighborhood) = CursorStrategy.execute(r,Cursor(r,n))(calc)
  def calc(r:Raster,cursor:Cursor):Unit
}

abstract class CellwiseFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) with CellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
}

/* Five arguments (the raster and neighborhoood and three others) */

abstract class FocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[FocalOp3[_,_,_,_]]
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
      init(r,a.asInstanceOf[A],b.asInstanceOf[B],c.asInstanceOf[C])
      execute(r,n)
      Result(getResult)
  }
  
  def init(r:Raster,a:A,b:B,c:C):Unit
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

abstract class CursorFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) {
  def execute(r:Raster,n:Neighborhood) = CursorStrategy.execute(r,Cursor(r,n))(calc)
  def calc(r:Raster,cursor:Cursor):Unit
}

abstract class CellwiseFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) with CellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
}

/* Six arguments (the raster and neighborhoood and four others) */

abstract class FocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c,d))
  def productArity = 6
  def canEqual(other:Any) = other.isInstanceOf[FocalOp4[_,_,_,_,_]]
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
      init(r,a.asInstanceOf[A],b.asInstanceOf[B],c.asInstanceOf[C],d.asInstanceOf[D])
      execute(r,n)
      Result(getResult)
  }
  
  def init(r:Raster,a:A,b:B,c:C,d:D):Unit
  def execute(r:Raster, n:Neighborhood):Unit
  def getResult:T
}

abstract class CursorFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D ]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) {
  def execute(r:Raster,n:Neighborhood) = CursorStrategy.execute(r,Cursor(r,n))(calc)
  def calc(r:Raster,cursor:Cursor):Unit
}

abstract class CellwiseFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) with CellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = CellwiseStrategy.execute(r,n,this)
}

