package geotrellis.source

import geotrellis._

import scala.language.higherKinds

trait DataSourceLike[+T,+V,+Repr <: DataSource[T,V]] { self:Repr =>
  def elements():Op[Seq[Op[T]]]
  private[geotrellis] def convergeOp():Op[V]
  def converge() = ValueSource(convergeOp.withName("Converge"))
  def converge[B](f:Seq[T]=>B):ValueSource[B] =
    ValueSource( logic.Collect(elements).map(f).withName("Converge") )

  /** apply a function to elements, and return the appropriate datasource **/
  def map[B,That](f:T => B)(implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _mapOp(op(f(_)),bf.apply(this))

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapOp[B,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  
    _mapOp(f,bf.apply(this))

  private def _mapOp[B,That](f:Op[T]=>Op[B],builder:SourceBuilder[B,That]) = {
    val newOp = elements.map(_.map(f)).withName(s"${self.getClass.getSimpleName} map")
    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def reduce[T1 >: T](reducer:(T1,T1) => T1):ValueSource[T1] = 
    reduceLeft(reducer)

  def reduceLeft[T1 >: T](reducer:(T1,T) => T1):ValueSource[T1] = 
    converge(_.reduceLeft(reducer))

  def reduceRight[T1 >: T](reducer:(T,T1) => T1):ValueSource[T1] = 
    converge(_.reduceRight(reducer))

  def foldLeft[B](z:B)(folder:(B,T)=>B):ValueSource[B] =
    converge(_.foldLeft(z)(folder))

  def foldRight[B](z:B)(folder:(T,B)=>B):ValueSource[B] =
    converge(_.foldRight(z)(folder))

  def distribute[T1 >: T,That](implicit bf:CanBuildSourceFrom[Repr,T1,That]):That =
    _mapOp(RemoteOperation(_, None),bf.apply(this))

  def distribute[T1 >: T,That](cluster:akka.actor.ActorRef)
                              (implicit bf:CanBuildSourceFrom[Repr,T1,That]):That =
    _mapOp(RemoteOperation(_, Some(cluster)),bf.apply(this))

  def combineOp[B,C,That](ds:DataSource[B,_])
                         (f:(Op[T],Op[B])=>Op[C])
                         (implicit bf:CanBuildSourceFrom[Repr,C,That]):That = {
    val newElements:Op[Seq[Op[C]]] =
      (elements,ds.elements).map { (e1,e2) =>
        e1.zip(e2).map { case (a,b) => 
          f(a,b) 
        }
      }

    val builder = bf.apply(this)
    builder.setOp(newElements)
    builder.result
  }

  def combineOp[T1 >: T,B,That](dss:Seq[DataSource[T1,_]])
                               (f:Seq[Op[T1]]=>Op[B])
                               (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val newElements:Op[Seq[Op[B]]] =
      (elements +: dss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(f)
      }

    val builder = bf.apply(this)
    builder.setOp(newElements)
    builder.result
  }

  def combine[B,C,That](ds:DataSource[B,_])
                       (f:(T,B)=>C)
                       (implicit bf:CanBuildSourceFrom[Repr,C,That]):That = {
    val newElements:Op[Seq[Op[C]]] =
      (elements,ds.elements).map { (e1,e2) =>
        e1.zip(e2).map { case (a,b) => 
          (a,b).map(f(_,_))
        }
      }

    val builder = bf.apply(this)
    builder.setOp(newElements)
    builder.result
  }

  def combine[T1 >: T,B,That](dss:Seq[DataSource[T1,_]])
                             (f:Seq[T1]=>B)
                             (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val newElements:Op[Seq[Op[B]]] =
      (elements +: dss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(_.mapOps(f(_)))
      }

    val builder = bf.apply(this)
    builder.setOp(newElements)
    builder.result
  }

  def run(implicit server:process.Server) = server.run(this)
  def get(implicit server:process.Server) = server.get(this)
}
