package geotrellis.source

import geotrellis._

import scala.language.higherKinds

trait DataSourceLike[+T,+V,+Repr <: DataSource[T,V]] { self:Repr =>
  def elements():Op[Seq[Op[T]]]
  def get():Op[V]
  def converge() = ValueDataSource(get)
  def converge[B](f:Seq[T]=>B):ValueDataSource[B] =
    ValueDataSource( logic.Collect(elements).map(f) )

  /** apply a function to elements, and return the appropriate datasource **/
  def map[B,That](f:T => B)(implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _mapOp(op(f(_)),bf.apply(this))

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapOp[B,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  
    _mapOp(f,bf.apply(this))

  private def _mapOp[B,That](f:Op[T]=>Op[B],builder:SourceBuilder[B,That]) = {
    val newOp = elements.map(_.map(f)).withName("DataSourceMap")
    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def reduce[T1 >: T](reducer:(T1,T1) => T1):ValueDataSource[T1] = 
    reduceLeft(reducer)

  def reduceLeft[T1 >: T](reducer:(T1,T) => T1):ValueDataSource[T1] = 
    converge(_.reduceLeft(reducer))

  def reduceRight[T1 >: T](reducer:(T,T1) => T1):ValueDataSource[T1] = 
    converge(_.reduceRight(reducer))

  def foldLeft[B](z:B)(folder:(B,T)=>B):ValueDataSource[B] =
    converge(_.foldLeft(z)(folder))

  def foldRight[B](z:B)(folder:(T,B)=>B):ValueDataSource[B] =
    converge(_.foldRight(z)(folder))
}
