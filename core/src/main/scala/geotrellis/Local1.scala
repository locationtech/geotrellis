package geotrellis

import scala.reflect.ClassTag
import scala.{specialized => sp}

import simulacrum._
import spire.algebra._
import spire.std.any._
import spire.syntax.field._
import spire.syntax.order._

// --- //

/*
@typeclass trait Local1[F[_]] {

  def lmap[@sp(Int, Double) A](self: F[A], f: A => A): F[A]

  def zipWith[@sp(Int, Double) A](self: F[A], other: => F[A], f: (A, A) => A): F[A]

  /** @group local */
  // def classify(self: A, f: Int => Int): A = map(self, f)

  @inline def +[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ + _ })

  @inline def -[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ - _ })

  @inline def *[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ * _ })

  @inline def /[@sp(Int, Double) A: Field](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ / _ })

  @inline def min[@sp(Int, Double) A: Order](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ min _ })

  @inline def max[@sp(Int, Double) A: Order](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ max _ })

}
 */

trait Local1[F[_]] extends _root_.scala.Any with _root_.scala.Serializable {
  def lmap[@sp(Int, Double) A](self: F[A], f: _root_.scala.Function1[A, A]): F[A];
  def zipWith[@sp(Int, Double) A](self: F[A], other: => F[A], f: _root_.scala.Function2[A, A, A]): F[A];
  @inline def +[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$1: Ring[A]): F[A] = zipWith(self, other, ((x$1, x$2) => x$1.+(x$2)));
  @inline def -[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$2: Ring[A]): F[A] = zipWith(self, other, ((x$3, x$4) => x$3.-(x$4)));
  @inline def *[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$3: Ring[A]): F[A] = zipWith(self, other, ((x$5, x$6) => x$5.*(x$6)));
  @inline def /[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$4: Field[A]): F[A] = zipWith(self, other, ((x$7, x$8) => x$7./(x$8)));
  @inline def min[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$5: Order[A]): F[A] = zipWith(self, other, ((x$9, x$10) => x$9.min(x$10)));
  @inline def max[@sp(Int, Double) A](self: F[A], other: => F[A])(implicit evidence$6: Order[A]): F[A] = zipWith(self, other, ((x$11, x$12) => x$11.max(x$12)))
}

object Local1 {
  @scala.inline def apply[F[_]](implicit instance: Local1[F]): Local1[F] = instance;
  trait Ops[F[_], A] {
    type TypeClassType <: Local1[F];
    val typeClassInstance: TypeClassType;
    def self: F[A];
    def lmap(f: _root_.scala.Function1[A, A]) = typeClassInstance.lmap[A](self, f);
    def zipWith(other: => F[A], f: _root_.scala.Function2[A, A, A]) = typeClassInstance.zipWith[A](self, other, f);
    def +(other: => F[A])(implicit evidence$1: Ring[A]) = typeClassInstance.+[A](self, other)(evidence$1);
    def -(other: => F[A])(implicit evidence$2: Ring[A]) = typeClassInstance.-[A](self, other)(evidence$2);
    def *(other: => F[A])(implicit evidence$3: Ring[A]) = typeClassInstance.*[A](self, other)(evidence$3);
    def /(other: => F[A])(implicit evidence$4: Field[A]) = typeClassInstance./[A](self, other)(evidence$4);
    def min(other: => F[A])(implicit evidence$5: Order[A]) = typeClassInstance.min[A](self, other)(evidence$5);
    def max(other: => F[A])(implicit evidence$6: Order[A]) = typeClassInstance.max[A](self, other)(evidence$6)
  }

  trait ToLocal1Ops {
    @java.lang.SuppressWarnings(scala.Array("org.wartremover.warts.ExplicitImplicitTypes", "org.wartremover.warts.ImplicitConversion")) implicit def toLocal1Ops[F[_], A](target: F[A])(implicit tc: Local1[F]): Ops[F, A] {
      type TypeClassType = Local1[F]
    } = {
      final class $anon extends Ops[F, A] {
        type TypeClassType = Local1[F];
        val self = target;
        val typeClassInstance: TypeClassType = tc
      };
      new $anon()
    }
  }

  object nonInheritedOps extends ToLocal1Ops;
  trait AllOps[F[_], A] extends Ops[F, A] {
    type TypeClassType <: Local1[F];
    val typeClassInstance: TypeClassType
  }

  object ops {
    @java.lang.SuppressWarnings(scala.Array("org.wartremover.warts.ExplicitImplicitTypes", "org.wartremover.warts.ImplicitConversion")) implicit def toAllLocal1Ops[F[_], A](target: F[A])(implicit tc: Local1[F]): AllOps[F, A] {
      type TypeClassType = Local1[F]
    } = {
      final class $anon extends AllOps[F, A] {
        type TypeClassType = Local1[F];
        val self = target;
        val typeClassInstance: TypeClassType = tc
      };
      new $anon()
    }
  }
}

private[geotrellis] trait LocalInstances {

  implicit val arrayLocal: Local1[Array] = new Local1[Array] {

    def lmap[@sp(Int, Double) A](self: Array[A], f: A => A): Array[A] = {
      val len: Int = self.size
      val res: Array[A] = self.clone
      var i: Int = 0

      while (i < len) {
        res(i) = f(self(i))
        i += 1
      }

      res
    }

    def zipWith[@sp(Int, Double) A](self: Array[A], other: => Array[A], f: (A, A) => A): Array[A] = {

      if (self.isEmpty) self else {
        val len: Int = if (self.size < other.size) self.size else other.size
        val res: Array[A] = if (self.size < other.size) self.clone else other.clone
        var i: Int = 0

        while(i < len) {
          res(i) = f(self(i), other(i))

          i += 1
        }

        res
      }
    }

  }
}
