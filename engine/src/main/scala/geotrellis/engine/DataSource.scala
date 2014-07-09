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

import scala.language.higherKinds

/**
 * DataSource[T, V]esents a data source that may be distributed across machines (logical data source) 
 * or loaded in memory on a specific machine. 
  */
trait DataSource[T, +V] extends OpSource[V] {
  def elements(): Op[Seq[Op[T]]]
  def converge() = ValueSource(convergeOp.withName("Converge"))
  def converge[B](f: Seq[T]=>B): ValueSource[B] =
    ValueSource( logic.Collect(elements).map(f).withName("Converge") )

  def withConverge[B](f: Seq[T] => B): DataSource[T, B] = 
    new DataSource[T, B] {
      def withElements(ops: Op[Seq[Op[T]]]): DataSource[T, B] =
        DataSource(ops).withConverge(f)

      def elements() = DataSource.this.elements

      def convergeOp: Op[B] = logic.Collect(elements).map(f)
    }

  def withElements(newElements: Op[Seq[Op[T]]]): DataSource[T, V]

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
    DataSource(elements.map(_.map(f)).withName(name))

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

    DataSource(newElements)
  }

  def combineOp[T1 >: T, B](dss: Seq[DataSource[T1, _]], name: String)
                                 (f: Seq[Op[T1]]=>Op[B]): SeqSource[B] = {
    val newElements: Op[Seq[Op[B]]] =
      (elements +: dss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(f)
      }.withName(name)

    DataSource(newElements)
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

  def distribute[T1 >: T]: DataSource[T, V] =
    withElements(elements.map { seq => seq.map(RemoteOperation(_, None)) })

  def distribute[T1 >: T](cluster: akka.actor.ActorRef): DataSource[T, V] =
    withElements(elements.map { seq => seq.map(RemoteOperation(_, Some(cluster))) })

  def cached[R1 >: DataSource[T, V], T1 >: T](implicit engine: Engine): DataSource[T, V] =
    withElements(Literal(engine.get(elements).map { op => Literal(engine.get(op)) }))
}

object DataSource {
  def convergeSeq[A](elementOps: (Op[Seq[Op[A]]])) = {
    logic.Collect(elementOps)
  }

  def fromValues[T](elements: T*): SeqSource[T] =
    fromValues(elements)

  def fromValues[T](elements: Seq[T])(implicit d: DI): SeqSource[T] =
    apply(Literal(elements.map(Literal(_))))

  def fromSources[T](sources: Seq[DataSource[_, T]]): SeqSource[T] =
    apply(Literal(sources.map(_.convergeOp)))

  def apply[T](es: Op[Seq[Op[T]]]): SeqSource[T] =
    new DataSource[T, Seq[T]] {
      def withElements(newElements: Op[Seq[Op[T]]]) = DataSource(newElements)
      val elements = es
      def convergeOp() = logic.Collect(elements)
    }
}
