---
layout: overview-large
title: Quasiquotes

disqus: true

partof: macros
num: 4
outof: 4
---
<span class="label warning" style="float: right;">MACRO PARADISE</span>

**Eugene Burmako**

Quasiquotes are a pre-release feature included in so-called macro paradise, an experimental branch in the official Scala repository. Follow the instructions at the ["Macro Paradise"](/overviews/macros/paradise.html) page to download and use our nightly builds.

## Intuition

Consider the `Lifter` [type macro](/overviews/macros/typemacros.html), which takes a template of a class or an object and duplicates all the methods in the template with their asynchronous counterparts wrapped in `future`.

    class D extends Lifter {
      def x = 2
      // def asyncX = future { 2 }
    }

    val d = new D
    d.asyncX onComplete {
      case Success(x) => println(x)
      case Failure(_) => println("failed")
    }

An implementation of such macro might look as the code at the snippet below. This routine - acquire, destructure, wrap in generated code, restructure again - is quite familiar to macro writers.

    case ClassDef(_, _, _, Template(_, _, defs)) =>
      val defs1 = defs collect {
        case DefDef(mods, name, tparams, vparamss, tpt, body) =>
          val tpt1 = if (tpt.isEmpty) tpt else AppliedTypeTree(
            Ident(newTermName("Future")), List(tpt))
          val body1 = Apply(
            Ident(newTermName("future")), List(body))
          val name1 = newTermName("async" + name.capitalize)
          DefDef(mods, name1, tparams, vparamss, tpt1, body1)
      }
      Template(Nil, emptyValDef, defs ::: defs1)

However even seasoned macro writers will admit that this code, even though it's quite simple, is exceedingly verbose, requiring one to understand the details internal representation of code snippets, e.g. the difference between `AppliedTypeTree` and `Apply`. Quasiquotes provide a neat domain-specific language that represents parameterized Scala snippets with Scala:

    val q"class $name extends Liftable { ..$body }" = tree

    val newdefs = body collect {
      case q"def $name[..$tparams](...$vparamss): $tpt = $body" =>
        val tpt1 = if (tpt.isEmpty) tpt else tq"Future[$tresult]"
        val name1 = newTermName("async" + name.capitalize)
        q"def $name1[..$tparams](...$vparamss): $tpt1 = future { $body }"
    }

    q"class $name extends AnyRef { ..${body ++ newdefs} }"

At the moment quasiquotes suffer from [SI-6842](https://issues.scala-lang.org/browse/SI-6842), which doesn't let one write the code as concisely as mentioned above. A [series of casts](https://gist.github.com/7ab617d054f28d68901b) has to be applied to get things working.

## Details

Quasiquotes are implemented as a part of the `scala.reflect.api.Universe` cake, which means that it is enough to do `import c.universe._` to use quasiquotes in macros. Exposed API provides `q` and `tq` [string interpolators](/overviews/core/string-interpolation.html) (corresponding to term and type quasiquotes), which support both construction and deconstruction, i.e. can be used both in normal code and on the left-hand side of a pattern case.

| Interpolator | Works with | Construction         | Deconstruction                |
|--------------|------------|----------------------|-------------------------------|
| `q`          | Term trees | `q"future{ $body }"` | `case q"future{ $body }"` =>  |
| `tq`         | Type trees | `tq"Future[$t]"`     | `case tq"Future[$t]"` =>      |

Unlike regular string interpolators, quasiquotes support multiple flavors of splices in order to distinguish between inserting/extracting single trees, lists of trees and lists of lists of trees. Mismatching cardinalities of splicees and splice operators results in a type error.

    scala> val name = TypeName("C")
    name: reflect.runtime.universe.TypeName = C

    scala> val q"class $name1" = q"class $name"
    name1: reflect.runtime.universe.Name = C

    scala> val args = List(Literal(Constant(2)))
    args: List[reflect.runtime.universe.Literal] = List(2)

    scala> val q"foo(..$args1)" = q"foo(..$args)"
    args1: List[reflect.runtime.universe.Tree] = List(2)

    scala> val argss = List(List(Literal(Constant(2))), List(Literal(Constant(3))))
    argss: List[List[reflect.runtime.universe.Literal]] = List(List(2), List(3))

    scala> val q"foo(...$argss1)" = q"foo(...$argss)"
    argss1: List[List[reflect.runtime.universe.Tree]] = List(List(2), List(3))

Unfortunately current implementation of quasiquotes is affected by [SI-6858](https://issues.scala-lang.org/browse/SI-6858), which renders triple-dot splices unusable.
We're doing our best to fix this issue as soon as possible.

## Tips and tricks

### Liftable

To simplify splicing of non-trees, quasiquotes provide the `Liftable` type class, which defines how values are transformed into trees when spliced in. We provide instances of `Liftable` for primitives and strings, which wrap those in `Literal(Constant(...))`. You might want to define your own instances for simple case classes and lists (also see [SI-6839](https://issues.scala-lang.org/browse/SI-6839)).

    trait Liftable[T] {
      def apply(universe: api.Universe, value: T): universe.Tree
    }
