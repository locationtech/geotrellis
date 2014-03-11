/***
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

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
    _mapOp(op(f(_)), None, bf.apply(this))

  /** apply a function to elements, and return the appropriate datasource **/
  def map[B,That](f:T => B, label: String)(implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _mapOp(op(f(_)), Some(label), bf.apply(this))

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapOp[B,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  
    _mapOp(f, None, bf.apply(this))

  private def _mapOp[B,That](f:Op[T]=>Op[B], label: Option[String], builder:SourceBuilder[B,That]) = {
    val name = 
      label match {
        case Some(l) => s"${self.getClass.getSimpleName} map[$l]"
        case None => s"${self.getClass.getSimpleName} map"
      }

    val newOp = elements.map(_.map(f)).withName(name)
    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def combine[B,C,That](ds:DataSource[B,_])
                       (f:(T,B)=>C)
                       (implicit bf:CanBuildSourceFrom[Repr,C,That]):That = 
    _combineOp(ds)( { (a,b) => (a,b).map(f(_,_)) }, bf.apply(this), None)

  def combine[B,C,That](ds:DataSource[B,_], label: String)
                       (f:(T,B)=>C)
                       (implicit bf:CanBuildSourceFrom[Repr,C,That]):That = 
    _combineOp(ds)( { (a,b) => (a,b).map(f(_,_)) }, bf.apply(this), Some(label))

  def combine[T1 >: T,B,That](dss:Seq[DataSource[T1,_]])
                             (f:Seq[T1]=>B)
                             (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _combineOp(dss)(_.mapOps(f(_)), bf.apply(this), None)

  def combine[T1 >: T,B,That](dss:Seq[DataSource[T1,_]], label: String)
                             (f:Seq[T1]=>B)
                             (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _combineOp(dss)(_.mapOps(f(_)), bf.apply(this), Some(label))

  def combineOp[B,C,That](ds:DataSource[B,_])
                         (f:(Op[T],Op[B])=>Op[C])
                         (implicit bf:CanBuildSourceFrom[Repr,C,That]):That =
    _combineOp(ds)(f, bf.apply(this), None)

  def combineOp[T1 >: T,B,That](dss:Seq[DataSource[T1,_]])
                               (f:Seq[Op[T1]]=>Op[B])
                               (implicit bf:CanBuildSourceFrom[Repr,B,That]):That =
    _combineOp(dss)(f, bf.apply(this), None)

  private 
  def _combineOp[T1 >: T,B,That](dss:Seq[DataSource[T1,_]])
                                (f:Seq[Op[T1]]=>Op[B], builder: SourceBuilder[B, That], 
                                                       label: Option[String]): That = {
    val name = 
      label match {
        case Some(l) => s"${self.getClass.getSimpleName} combine[$l]"
        case None => s"${self.getClass.getSimpleName} combine"
      }

    val newElements:Op[Seq[Op[B]]] =
      (elements +: dss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(f)
      }.withName(name)

    builder.setOp(newElements)
    builder.result
  }

  private 
  def _combineOp[B, C, That](ds: DataSource[B, _])
                            (f:(Op[T],Op[B])=>Op[C], builder: SourceBuilder[C, That],
                                                     label: Option[String]): That = {
    val name = 
      label match {
        case Some(l) => s"${self.getClass.getSimpleName} combine[$l]"
        case None => s"${self.getClass.getSimpleName} combine"
      }

    val newElements:Op[Seq[Op[C]]] =
      ((elements,ds.elements).map { (e1,e2) =>
        e1.zip(e2).map { case (a,b) => 
          f(a,b) 
        }
      }).withName(name)

    builder.setOp(newElements)
    builder.result
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
    _mapOp(RemoteOperation(_, None), None, bf.apply(this))

  def distribute[T1 >: T,That](cluster:akka.actor.ActorRef)
                              (implicit bf:CanBuildSourceFrom[Repr,T1,That]):That =
    _mapOp(RemoteOperation(_, Some(cluster)), None, bf.apply(this))

  def run(implicit server:process.Server) = server.run(this)
  def get(implicit server:process.Server) = server.get(this)

  def cached[R1 >: Repr,T1 >: T](implicit server:process.Server, bf:CanBuildSourceFrom[Repr,T1,R1]) = {
    val elementOps = server.get(elements) 
    val newElements = Literal(elementOps.map { op => Literal(server.get(op)) })
    val builder = bf.apply(this)
    builder.setOp(newElements)
    builder.result
  }
}
