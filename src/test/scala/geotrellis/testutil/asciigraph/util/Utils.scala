package com.github.mdr.ascii.util

import scala.annotation.tailrec

object Utils {

  def transformValues[K, V, V2](map: Map[K, V])(f: V ⇒ V2): Map[K, V2] = map.map { case (k, v) ⇒ (k, f(v)) }

  def withPrevious[T](iterable: Iterable[T]): List[(Option[T], T)] =
    withPreviousAndNext(iterable).map { case (a, b, c) ⇒ (a, b) }

  def withPreviousAndNext[T](iterable: Iterable[T]): List[(Option[T], T, Option[T])] = {
    if (iterable.isEmpty)
      Nil
    else {
      val previous = None :: (iterable.init map Some[T]).toList
      val next = (iterable.tail map Some[T]).toList ::: List(None)
      previous zip iterable zip next map { case ((a, b), c) ⇒ (a, b, c) }
    }
  }

  def adjacentPairs[T](xs: List[T]): List[(T, T)] = xs zip xs.drop(1)

  def adjacentTriples[T](xs: List[T]): List[(T, T, T)] = xs zip xs.drop(1) zip xs.drop(2) map { case ((x, y), z) ⇒ (x, y, z) }

  @tailrec
  def iterate[T](t: T, f: T ⇒ Option[T]): T = f(t) match {
    case Some(t2) ⇒ iterate(t2, f)
    case None     ⇒ t
  }

}