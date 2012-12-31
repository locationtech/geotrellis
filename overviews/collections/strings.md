---
layout: overview-large
title: Strings

disqus: true

partof: collections
num: 11
languages: [ja]
---

Like arrays, strings are not directly sequences, but they can be converted to them, and they also support all sequence operations on strings. Here are some examples of operations you can invoke on strings.

    scala> val str = "hello"
    str: java.lang.String = hello
    scala> str.reverse
    res6: String = olleh
    scala> str.map(_.toUpper)
    res7: String = HELLO
    scala> str drop 3 
    res8: String = lo
    scala> str slice (1, 4)
    res9: String = ell
    scala> val s: Seq[Char] = str
    s: Seq[Char] = WrappedString(h, e, l, l, o)

These operations are supported by two implicit conversions. The first, low-priority conversion maps a `String` to a `WrappedString`, which is a subclass of `immutable.IndexedSeq`, This conversion got applied in the last line above where a string got converted into a Seq. the other, high-priority conversion maps a string to a `StringOps` object, which adds all methods on immutable sequences to strings. This conversion was implicitly inserted in the method calls of `reverse`, `map`, `drop`, and `slice` in the example above.