==========================
Typeclasses via Simulacrum
==========================

We've decided to use `Simulacrum <https://github.com/mpilquist/simulacrum>`__
to provide new GeoTrellis *typeclasses* around which we expect significant
code reorganization and clean up.

Typeclasses Defined
-------------------

Typeclasses are powerful programming abstractions that relate data types
which have common behaviour. They describe how a type should behave, as opposed
to what a data type is (re: Object Oriented programming).

Typeclasses expose one or more functions and *must* have associated mathematical "Laws".

.. code-block:: scala

   import simulacrum._

   /**
    * LAW: Associativity
    * (a <> b) <> c == a <> (b <> c)
    */
   @typeclass trait Semigroup[A] {
     @op("<>") def combine(x: A, y: A): A
   }

For any given "instance" implementation of ``Semigroup`` (say ``Int``),
the law is unchecked by the compiler. Making sure an
instance upholds the laws is left up to the instance author, say by
writing appropriate unit tests.

In the case of Scala with ``simulacrum``, macro mechanics are employed to
provide the compile-time boilerplate necessary for method injection to occur.
Say given a type ``Foo`` which has a ``Semigroup`` instance, the following
method would be automatically injected:

.. code-block:: scala

   def combine(y: Foo): Foo

where the original ``x`` is supplied by ``this`` / ``self``.

What a Typeclass is not
^^^^^^^^^^^^^^^^^^^^^^^

.. pull-quote::

   (1) *A typeclass without laws is an obfuscation, not an abstraction.*

If a law cannot be found for the proposed fundamental functions,
then the functions must be changed or the attempt at writing the typeclass
must be abandoned.

.. pull-quote::

   (2) *A typeclass with one instance and used in one place is not an abstraction.*

Writing "one-off" typeclasses needlessly increases API surface area. We should consider if the
same functionality can't be written directly on the type itself.

.. pull-quote::

   Corollary from (1) and (2): *A typeclass is not an excuse to inject methods.*

If it comes to this, we must reconsider our designs.

.. pull-quote::

   (3) *A typeclass is not a description of class fields.*

We should avoid definining ``val`` fields on our typeclasses, as this is closer
to the "what" of OOP as opposed to the "how" of typeclasses. Further, a typeclass
having a ``.combine`` method, say, does not mean we expect the implementing class
to directly contain a ``.combine`` method. ``Foo`` does not extend ``Semigroup``,
it provides an *instance* of ``Semigroup`` in its companion object.

What a Typeclass is
^^^^^^^^^^^^^^^^^^^

.. pull-quote::

   (1) *A typeclass is an abstraction over types who share lawful behaviour.*

So somewhere in our code, we could write the function:

.. code-block:: scala

   def foo[A: Semigroup](a: A, foo: Foo): Bar = { ... }

and use ``.combine`` on ``a`` safely, knowing that it will behave sanely no matter
which ``A`` we actually choose at the call-site. This is different from just knowing
that our ``A`` contains a ``.foobar`` method through the usual OOP inheritance
mechanisms (``A <: FooBar``). Typeclasses bring together types that otherwise share
no supertype/subtype relationship, so laws are what guarantee sane behaviour now
and in the future.

Best Practices
--------------

Try to find the minimal set of operations and laws that describe the fundamentals.
No orphans!!!!!
Scaladocs (i.e. where to look for instances, etc). Scala has no "reverse instance lookup"
like Haskell.
Mention laws in typeclass docstrings.

Usage
-----

Proposed Typeclasses
--------------------

Further Work
------------

Removal of custom ``MethodExtension`` mechanics.
