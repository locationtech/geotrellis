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
  }
  
  def init(r:Raster) = { }
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class IntFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[T](r,n) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getInt(r,n))(calc))

  def createBuilder(r:Raster):IntResultBuilder[T]
  def calc(cursor:IntCursor):Int
}

abstract class DoubleFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[T](r,n) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getDouble(r,n))(calc))

  def createBuilder(r:Raster):DoubleResultBuilder[T]

  def calc(cursor:DoubleCursor):Double
}

abstract class IntCellwiseFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) 
         extends FocalOp[T](r,n) with IntCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):IntResultBuilder[T]
}

abstract class DoubleCellwiseFocalOp[T](r:Op[Raster],n:Op[Neighborhood]) 
         extends FocalOp[T](r,n) with DoubleCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):DoubleResultBuilder[T]
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
      execute(r,n,a.asInstanceOf[A])
  }
  
  def init(r:Raster,a:A)
  def execute(r:Raster, n:Neighborhood, a:A):Result[T]
}

abstract class IntFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) {
  def execute(r:Raster,n:Neighborhood,a:A) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getInt(r,n)) {
             cursor => calc(cursor)
           })
 
  def createBuilder(r:Raster):IntResultBuilder[T]
  def calc(cursor:IntCursor):Int
}

abstract class DoubleFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) {
  def execute(r:Raster,n:Neighborhood,a:A) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getDouble(r,n)) {
             cursor => calc(cursor)
           })
  def createBuilder(r:Raster):DoubleResultBuilder[T]
  def calc(cursor:DoubleCursor):Double
}

abstract class IntCellwiseFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) with IntCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = 
    Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):IntResultBuilder[T]
}

abstract class DoubleCellwiseFocalOp1[A,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A]) 
         extends FocalOp1[A,T](r,n,a) with DoubleCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = 
    Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):DoubleResultBuilder[T]
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
  }
  
  def init(r:Raster,a:A,b:B)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class IntFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getInt(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):IntResultBuilder[T]
  def calc(cursor:IntCursor):Int
}

abstract class DoubleFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getDouble(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):DoubleResultBuilder[T]
  def calc(cursor:DoubleCursor):Double
}

abstract class IntCellwiseFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) with IntCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):IntResultBuilder[T]
}

abstract class DoubleCellwiseFocalOp2[A,B,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B]) 
         extends FocalOp2[A,B,T](r,n,a,b) with DoubleCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):DoubleResultBuilder[T]
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
  }
  
  def init(r:Raster,a:A,b:B,c:C)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class IntFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getInt(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):IntResultBuilder[T]
  def calc(cursor:IntCursor):Int
}

abstract class DoubleFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getDouble(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):DoubleResultBuilder[T]
  def calc(cursor:DoubleCursor):Double
}

abstract class IntCellwiseFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) with IntCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):IntResultBuilder[T]
}

abstract class DoubleCellwiseFocalOp3[A,B,C,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C]) 
         extends FocalOp3[A,B,C,T](r,n,a,b,c) with DoubleCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):DoubleResultBuilder[T]
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
  }
  
  def init(r:Raster,a:A,b:B,c:C,d:D)
  def execute(r:Raster, n:Neighborhood):Result[T]
}

abstract class IntFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D ]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getInt(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):IntResultBuilder[T]
  def calc(cursor:IntCursor):Int
}

abstract class DoubleFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) {
  def execute(r:Raster,n:Neighborhood) = 
    Result(CursorStrategy.execute(r,createBuilder(r),Cursor.getDouble(r,n)) {
             cursor => calc(cursor)
           })

  def createBuilder(r:Raster):DoubleResultBuilder[T]
  def calc(cursor:DoubleCursor):Double
}

abstract class IntCellwiseFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) with IntCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):IntResultBuilder[T]
}

abstract class DoubleCellwiseFocalOp4[A,B,C,D,T](r:Op[Raster],n:Op[Neighborhood],a:Op[A],b:Op[B],c:Op[C],d:Op[D]) 
         extends FocalOp4[A,B,C,D,T](r,n,a,b,c,d) with DoubleCellwiseCalculator {
  def execute(r:Raster, n:Neighborhood) = Result(CellwiseStrategy.execute(r,n,createBuilder(r),this))

  def createBuilder(r:Raster):DoubleResultBuilder[T]
}

