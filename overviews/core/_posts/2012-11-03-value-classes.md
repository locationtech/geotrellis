---
layout: overview
title: Value Classes and Universal Traits
label-color: success
label-text: New in 2.10
overview: value-classes
languages: [ja]
---

**Mark Harrah**

## Introduction

Value classes are a new mechanism in Scala to avoid allocating runtime objects.
This is accomplished through the definition of new `AnyVal` subclasses.
They were proposed in [SIP-15](http://docs.scala-lang.org/sips/pending/value-classes.html).
The following shows a very minimal value class definition:

    class Wrapper(val underlying: Int) extends AnyVal

It has a single, public `val` parameter that is the underlying runtime representation.
The type at compile time is `Wrapper`, but at runtime, the representation is an `Int`.
A value class can define `def`s, but no `val`s, `var`s, or nested `traits`s, `class`es or `object`s:

    class Wrapper(val underlying: Int) extends AnyVal {
      def foo: Wrapper = new Wrapper(underlying * 19)
    }

A value class can only extend *universal traits* and cannot be extended itself.
A *universal trait* is a trait that extends `Any`, only has `def`s as members, and does no initialization.
Universal traits allow basic inheritance of methods for value classes, but they *incur the overhead of allocation*.
For example,

    trait Printable extends Any {
      def print(): Unit = println(this)
    }
    class Wrapper(val underlying: Int) extends AnyVal with Printable

    val w = new Wrapper(3)
    w.print() // actually requires instantiating a Wrapper instance

The remaining sections of this documentation show use cases, details on when allocations do and do not occur, and concrete examples of limitations of value classes.

## Extension methods

One use case for value classes is to combine them with implicit classes ([SIP-13](http://docs.scala-lang.org/sips/pending/implicit-classes.html)) for allocation-free extension methods.  Using an implicit class provides a more convenient syntax for defining extension methods, while value classes remove the runtime overhead. A good example is the `RichInt` class in the standard library.  `RichInt` extends the `Int` type with several methods.  Because it is a value class, an instance of `RichInt` doesn't need to be created when using `RichInt` methods.

The following fragment of `RichInt` shows how it extends `Int` to allow the expression `3.toHexString`:

    class RichInt(val self: Int) extends AnyVal {
      def toHexString: String = java.lang.Integer.toHexString(self)
    }

At runtime, this expression `3.toHexString` is optimised to the equivalent of a method call on a static object
(`RichInt$.MODULE$.extension$toHexString(3)`), rather than a method call on a newly instantiated object.

## Correctness

Another use case for value classes is to get the type safety of a data type without the runtime allocation overhead.
For example, a fragment of a data type that represents a distance might look like:

    class Meter(val value: Double) extends AnyVal {
      def +(m: Meter): Meter = new Meter(value + m.value)
    }

Code that adds two distances, such as

    val x = new Meter(3.4)
    val y = new Meter(4.3)
    val z = x + y

will not actually allocate any `Meter` instances, but will only use primitive doubles at runtime.

*Note: You can use case classes and/or extension methods for cleaner syntax in practice.*

## When Allocation Is Necessary

Because the JVM does not support value classes, Scala sometimes needs to actually instantiate a value class.
Full details may be found in [SIP-15](http://docs.scala-lang.org/sips/pending/value-classes.html).

### Allocation Summary

A value class is actually instantiated when:

1. a value class is treated as another type.
2. a value class is assigned to an array.
3. doing runtime type tests, such as pattern matching.

### Allocation Details

Whenever a value class is treated as another type, including a universal trait, an instance of the actual value class must be instantiated.
As an example, consider the `Meter` value class:

    trait Distance extends Any
    case class Meter(val value: Double) extends AnyVal with Distance

A method that accepts a value of type `Distance` will require an actual `Meter` instance.
In the following example, the `Meter` classes are actually instantiated:

    def add(a: Distance, b: Distance): Distance = ...
    add(Meter(3.4), Meter(4.3))

If the signature of `add` were instead:

    def add(a: Meter, b: Meter): Meter = ...

then allocations would not be necessary.
Another instance of this rule is when a value class is used as a type argument.
For example, the actual Meter instance must be created for even a call to identity:

    def identity[T](t: T): T = t
    identity(Meter(5.0))

Another situation where an allocation is necessary is when assigning to an array, even if it is an array of that value class.
For example,

    val m = Meter(5.0)
    val array = Array[Meter](m)

The array here contains actual `Meter` instances and not just the underlying double primitives.

Lastly, type tests such as those done in pattern matching or `asInstanceOf` require actual value class instances:

    case class P(val i: Int) extends AnyVal

    val p = new P(3)
    p match { // new P instantiated here
      case P(3) => println("Matched 3")
      case P(x) => println("Not 3")
    }

## Limitations

Value classes currently have several limitations, in part because the JVM does not natively support the concept of value classes.
Full details on the implementation of value classes and their limitations may be found in [SIP-15](http://docs.scala-lang.org/sips/pending/value-classes.html).

### Summary of Limitations

A value class ...

1. ... must have only a primary constructor with exactly one public, val parameter whose type is not a value class.
2. ... may not have specialized type parameters.
3. ... may not have nested or local classes, traits, or objects
4. ... may not define a equals or hashCode method.
5. ... must be a top-level class or a member of a statically accessible object
6. ... can only have defs as members.  In particular, it cannot have lazy vals, vars, or vals as members.
7. ... cannot be extended by another class.

### Examples of Limitations

This section provides many concrete consequences of these limitations not already described in the necessary allocations section.

Multiple constructor parameters are not allowed:

    class Complex(val real: Double, val imag: Double) extends AnyVal

and the Scala compiler will generate the following error message:

    Complex.scala:1: error: value class needs to have exactly one public val parameter
    class Complex(val real: Double, val imag: Double) extends AnyVal
          ^

Because the constructor parameter must be a `val`, it cannot be a by-name parameter:

    NoByName.scala:1: error: `val' parameters may not be call-by-name
    class NoByName(val x: => Int) extends AnyVal
                          ^

Scala doesn't allow lazy val constructor parameters, so that isn't allowed either.
Multiple constructors are not allowed:

    class Secondary(val x: Int) extends AnyVal {
      def this(y: Double) = this(y.toInt)
    }

    Secondary.scala:2: error: value class may not have secondary constructors
      def this(y: Double) = this(y.toInt)
          ^

A value class cannot have lazy vals or vals as members and cannot have nested classes, traits, or objects:

    class NoLazyMember(val evaluate: () => Double) extends AnyVal {
      val member: Int = 3
      lazy val x: Double = evaluate()
      object NestedObject
      class NestedClass
    }

    Invalid.scala:2: error: this statement is not allowed in value class: private[this] val member: Int = 3
      val member: Int = 3
          ^
    Invalid.scala:3: error: this statement is not allowed in value class: lazy private[this] var x: Double = NoLazyMember.this.evaluate.apply()
      lazy val x: Double = evaluate()
               ^
    Invalid.scala:4: error: value class may not have nested module definitions
      object NestedObject
             ^
    Invalid.scala:5: error: value class may not have nested class definitions
      class NestedClass
            ^

Note that local classes, traits, and objects are not allowed either, as in the following:

    class NoLocalTemplates(val x: Int) extends AnyVal {
      def aMethod = {
        class Local
        ...
      }
    }

A current implementation restriction is that value classes cannot be nested:

    class Outer(val inner: Inner) extends AnyVal
    class Inner(val value: Int) extends AnyVal

    Nested.scala:1: error: value class may not wrap another user-defined value class
    class Outer(val inner: Inner) extends AnyVal
                    ^

Additionally, structural types cannot use value classes in method parameter or return types:

    class Value(val x: Int) extends AnyVal
    object Usage {
      def anyValue(v: { def value: Value }): Value =
        v.value
    }

    Struct.scala:3: error: Result type in structural refinement may not refer to a user-defined value class
      def anyValue(v: { def value: Value }): Value =
                                   ^

A value class may not extend a non-universal trait and a value class may not itself be extended:

    trait NotUniversal
    class Value(val x: Int) extends AnyVal with notUniversal
    class Extend(x: Int) extends Value(x)

    Extend.scala:2: error: illegal inheritance; superclass AnyVal
     is not a subclass of the superclass Object
     of the mixin trait NotUniversal
    class Value(val x: Int) extends AnyVal with NotUniversal
                                                ^
    Extend.scala:3: error: illegal inheritance from final class Value
    class Extend(x: Int) extends Value(x)
                                 ^

The second error messages shows that although the `final` modifier is not explicitly specified for a value class, it is assumed.

Another limitation that is a result of supporting only one parameter to a class is that a value class must be top-level or a member of a statically accessible object.
This is because a nested value class would require a second parameter that references the enclosing class.
So, this is not allowed:

    class Outer {
      class Inner(val x: Int) extends AnyVal
    }

    Outer.scala:2: error: value class may not be a member of another class
    class Inner(val x: Int) extends AnyVal
          ^

but this is allowed because the enclosing object is top-level:

    object Outer {
      class Inner(val x: Int) extends AnyVal
    }
