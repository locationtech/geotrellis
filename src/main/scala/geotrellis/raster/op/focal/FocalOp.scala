package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._

/* Two arguments (the raster and neighborhoood) */

abstract class BaseFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n))
  def productArity = 2
  def canEqual(other:Any) = other.isInstanceOf[BaseFocalOp[_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: Nil => 
      init(r)
      execute(r,n)
  }
  
  def init(r:Raster) = { }
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class FocalOp[T,@specialized(Int,Double)D](r:Op[Raster],n:Op[Neighborhood]) extends BaseFocalOp[T](r,n) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n))(calc))

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):ResultBuilder[T,D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[T,Int](r,n) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
}

abstract class DoubleFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[T,Double](r,n) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
}

abstract class CellwiseFocalOp[T,@specialized(Int,Double) D](r:Op[Raster],n:Op[Neighborhood]) 
         extends BaseFocalOp[T](r,n) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):ResultBuilder[T,D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}

/* Three arguments (the raster and neighborhoood and another) */

abstract class BaseFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a))
  def productArity = 3
  def canEqual(other:Any) = other.isInstanceOf[BaseFocalOp1[_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: Nil => 
      init(r,a.asInstanceOf[A])
      execute(r,n,a.asInstanceOf[A])
  }
  
  def init(r:Raster,a:A)
  def execute(r:Raster, n:Neighborhood, a:A):Result[T]
}

abstract class FocalOp1[A,T,@specialized(Int,Double)D](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends BaseFocalOp1[A,T](r,n,a) {
  def execute(r:Raster,n:Neighborhood,a:A) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n)) {
             cursor => calc(cursor)
           })

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):ResultBuilder[T,D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T,Int](r,n,a) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
}

abstract class DoubleFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T,Double](r,n,a) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
}

abstract class CellwiseFocalOp1[A,T,@specialized(Int,Double) D](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends BaseFocalOp1[A,T](r,n,a) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = 
    Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):ResultBuilder[T,D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}

/* Four arguments (the raster and neighborhoood and two others) */

abstract class BaseFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b))
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[BaseFocalOp2[_,_,_]]
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
  }
  
  def init(r:Raster,a:A,b:B)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class FocalOp2[A,B,T,@specialized(Int,Double)D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends BaseFocalOp2[A,B,T](r,n,a,b) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n)) {
             cursor => calc(cursor)
           })

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):ResultBuilder[T,D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T,Int](r,n,a,b) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
}

abstract class DoubleFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T,Double](r,n,a,b) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
}

abstract class CellwiseFocalOp2[A,B,T,@specialized(Int,Double) D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends BaseFocalOp2[A,B,T](r,n,a,b) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):ResultBuilder[T,D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}

/* Five arguments (the raster and neighborhoood and three others) */

abstract class BaseFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c))
  def productArity = 5
  def canEqual(other:Any) = other.isInstanceOf[BaseFocalOp3[_,_,_,_]]
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
  }
  
  def init(r:Raster,a:A,b:B,c:C)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class FocalOp3[A,B,C,T,@specialized(Int,Double)D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends BaseFocalOp3[A,B,C,T](r,n,a,b,c) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n)) {
             cursor => calc(cursor)
           })

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):ResultBuilder[T,D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T,Int](r,n,a,b,c) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
}

abstract class DoubleFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T,Double](r,n,a,b,c) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
}

abstract class CellwiseFocalOp3[A,B,C,T,@specialized(Int,Double) D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends BaseFocalOp3[A,B,C,T](r,n,a,b,c) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):ResultBuilder[T,D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}

/* Six arguments (the raster and neighborhoood and four others) */

abstract class BaseFocalOp4[A,B,C,M,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],m:Op[M]) 
         extends Operation[T] {
  def _run(context:Context) = runAsync(List('init,r,n,a,b,c,m))
  def productArity = 6
  def canEqual(other:Any) = other.isInstanceOf[BaseFocalOp4[_,_,_,_,_]]
  def productElement(n:Int) = n match {
    case 0 => r
    case 1 => n
    case 2 => a
    case 3 => b
    case 4 => c
    case 5 => m
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[T]] = {
    case 'init :: (r:Raster) :: (n:Neighborhood) :: a :: b :: c :: m :: Nil => 
      init(r,a.asInstanceOf[A],b.asInstanceOf[B],c.asInstanceOf[C],m.asInstanceOf[M])
      execute(r,n)
  }
  
  def init(r:Raster,a:A,b:B,c:C,m:M)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class FocalOp4[A,B,C,M,T,@specialized(Int,Double)D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],m:Op[M]) 
         extends BaseFocalOp4[A,B,C,M,T](r,n,a,b,c,m) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),createCursor(r,n)) {
             cursor => calc(cursor)
           })

  def createCursor(r:Raster,n:Neighborhood):Cursor[D]
  def createBuilder(r:Raster):ResultBuilder[T,D]

  def calc(cursor:Cursor[D]):D
}

abstract class IntFocalOp4[A,B,C,M,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],m:Op[M]) 
         extends FocalOp4[A,B,C,M,T,Int](r,n,a,b,c,m) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getInt(r,n)
}

abstract class DoubleFocalOp4[A,B,C,M,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],m:Op[M]) 
         extends FocalOp4[A,B,C,M,T,Double](r,n,a,b,c,m) {
  def createCursor(r:Raster,n:Neighborhood) = Cursor.getDouble(r,n)
}

abstract class CellwiseFocalOp4[A,B,C,M,T,@specialized(Int,Double) D](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],m:Op[M]) 
         extends BaseFocalOp4[A,B,C,M,T](r,n,a,b,c,m) with CellwiseCalculator[D] {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,createBuilder(r),n)(this))

  def createBuilder(r:Raster):ResultBuilder[T,D]

  def add(r:Raster, x:Int, y:Int)
  def remove(r:Raster, x:Int, y:Int)
  def reset()
  def getValue:D
}
