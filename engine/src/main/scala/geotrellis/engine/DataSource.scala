/*
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
 */

package geotrellis.engine

import akka.actor.ActorRef

import scala.language.higherKinds

/**
 * DataSource[T, V]esents a data source that may be distributed across machines (logical data source) 
 * or loaded in memory on a specific machine. 
  */
trait DataSource[T, +V] extends OpSource[V] {
  type Self <: DataSource[T, V]

  def elements(): Op[Seq[Op[T]]]
  def converge() = ValueSource(convergeOp.withName("Converge"))
  def converge[B](f: Seq[T]=>B): ValueSource[B] =
    ValueSource( logic.Collect(elements).map(f).withName("Converge") )

  def withConverge[B](f: Seq[T] => B): DataSource[T, B] = 
    new DataSource[T, B] {
      type Self = DataSource[T, B]
      def elements() = DataSource.this.elements

      def convergeOp: Op[B] = logic.Collect(elements).map(f)

      def distribute(cluster: Option[ActorRef]): DataSource[T, B] =
        DataSource.this.distribute(cluster) withConverge(f)

      def cached(implicit engine: Engine): DataSource[T, B] =
        DataSource.this.cached(engine) withConverge(f)
    }

  /** apply a function to elements **/
  def map[B](f: T => B): SeqSource[B] = 
    mapOp(Op(f(_)))

  /** apply a function to elements **/
  def map[B](f: T => B, label: String): SeqSource[B] = 
    mapOp(Op(f(_)), label)

  /** apply a function to element operations **/
  def mapOp[B](f: Op[T] => Op[B]): SeqSource[B] =  
    mapOp(f, s"${getClass.getSimpleName} map")

  /** apply a function to element operations **/
  def mapOp[B](f: Op[T]=>Op[B], name: String): SeqSource[B] =
    SeqSource(elements.map(_.map(f)).withName(name))

  def combine[B, C](ds: DataSource[B, _])(f: (T, B)=>C): SeqSource[C] = 
    combineOp(ds)( { (a, b) => (a, b).map(f(_, _)) })

  def combine[B, C](ds: DataSource[B, _], label: String)
                         (f: (T, B)=>C): SeqSource[C] =
    combineOp(ds, label)( { (a, b) => (a, b).map(f(_, _)) })

  def combine[T1 >: T, B](dss: Seq[DataSource[T1, _]])
                         (f: Seq[T1]=>B): SeqSource[B] = 
    combineOp(dss)(_.mapOps(f(_)))

  def combine[T1 >: T, B](dss: Seq[DataSource[T1, _]], label: String)
                         (f: Seq[T1] => B): SeqSource[B] =
    combineOp(dss, label)(_.mapOps(f(_)))

  def combineOp[B, C](ds: DataSource[B, _])
                           (f: (Op[T], Op[B])=>Op[C]): SeqSource[C] =
    combineOp(ds, s"${getClass.getSimpleName} combine")(f)

  def combineOp[T1 >: T, B](dss: Seq[DataSource[T1, _]])
                           (f: Seq[Op[T1]]=>Op[B]): SeqSource[B] =
    combineOp(dss, s"${getClass.getSimpleName} combine")(f)

  def combineOp[B, C](ds: DataSource[B, _], name: String)
                     (f: (Op[T], Op[B])=>Op[C]): SeqSource[C] = {
    val newElements: Op[Seq[Op[C]]] =
      ((elements, ds.elements).map { (e1, e2) =>
        e1.zip(e2).map { case (a, b) => 
          f(a, b) 
        }
      }).withName(name)

    SeqSource(newElements)
  }

  def combineOp[T1 >: T, B](dss: Seq[DataSource[T1, _]], name: String)
                                 (f: Seq[Op[T1]]=>Op[B]): SeqSource[B] = {
    val newElements: Op[Seq[Op[B]]] =
      (elements +: dss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(f)
      }.withName(name)

    SeqSource(newElements)
  }
  def reduce[T1 >: T](reducer: (T1, T1) => T1): ValueSource[T1] = 
    reduceLeft(reducer)

  def reduceLeft[T1 >: T](reducer: (T1, T) => T1): ValueSource[T1] = 
    converge(_.reduceLeft(reducer))

  def reduceRight[T1 >: T](reducer: (T, T1) => T1): ValueSource[T1] = 
    converge(_.reduceRight(reducer))

  def foldLeft[B](z: B)(folder: (B, T)=>B): ValueSource[B] =
    converge(_.foldLeft(z)(folder))

  def foldRight[B](z: B)(folder: (T, B)=>B): ValueSource[B] =
    converge(_.foldRight(z)(folder))

  def distributeOps(cluster: Option[ActorRef]): Op[Seq[Op[T]]] =
    elements.map { seq => seq.map(RemoteOperation(_, cluster)) }

  def distribute: Self =
    distribute(None)

  def distribute(cluster: ActorRef): Self =
    distribute(Some(cluster))

  def distribute(cluster: Option[ActorRef]): Self

  def cachedOps(implicit engine: Engine): Op[Seq[Op[T]]] = Literal(engine.get(elements).map { op => Literal(engine.get(op)) })

  def cached(implicit engine: Engine): Self
}

object DataSource {
  def convergeSeq[A](elementOps: (Op[Seq[Op[A]]])) =
    logic.Collect(elementOps)

  def fromValues[T](elements: T*): SeqSource[T] =
    fromValues(elements)

  def fromValues[T](elements: Seq[T])(implicit d: DI): SeqSource[T] =
    SeqSource(Literal(elements.map(Literal(_))))

  def fromSources[T](sources: Seq[OpSource[T]]): SeqSource[T] =
    SeqSource(Literal(sources.map(_.convergeOp)))
}
