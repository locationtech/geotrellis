---
layout: overview-large
title: Maps

disqus: true

partof: collections
num: 7
languages: [ja]
---

A [Map](http://www.scala-lang.org/api/current/scala/collection/Map.html) is an [Iterable](http://www.scala-lang.org/api/current/scala/collection/Iterable.html) consisting of pairs of keys and values (also named _mappings_ or _associations_). Scala's [Predef](http://www.scala-lang.org/api/current/scala/Predef$.html) class offers an implicit conversion that lets you write `key -> value` as an alternate syntax for the pair `(key, value)`. For instance `Map("x" -> 24, "y" -> 25, "z" -> 26)` means exactly the same as `Map(("x", 24), ("y", 25), ("z", 26))`, but reads better.

The fundamental operations on maps are similar to those on sets. They are summarized in the following table and fall into the following categories:

* **Lookup** operations `apply`, `get`, `getOrElse`, `contains`, and `isDefinedAt`. These turn maps into partial functions from keys to values. The fundamental lookup method for a map is: `def get(key): Option[Value]`. The operation "`m get key`" tests whether the map contains an association for the given `key`. If so, it returns the associated value in a `Some`. If no key is defined in the map, `get` returns `None`. Maps also define an `apply` method that returns the value associated with a given key directly, without wrapping it in an `Option`. If the key is not defined in the map, an exception is raised.
* **Additions and updates** `+`, `++`, `updated`, which let you add new bindings to a map or change existing bindings.
* **Removals** `-`, `--`, which remove bindings from a map.
* **Subcollection producers** `keys`, `keySet`, `keysIterator`, `values`, `valuesIterator`, which return a map's keys and values separately in various forms.
* **Transformations** `filterKeys` and `mapValues`, which produce a new map by filtering and transforming bindings of an existing map.

### Operations in Class Map ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Lookups:**             |						     |
|  `ms get k`  	            |The value associated with key `k` in map `ms` as an option, `None` if not found.|
|  `ms(k)`  	            |(or, written out, `ms apply k`) The value associated with key `k` in map `ms`, or exception if not found.|
|  `ms getOrElse (k, d)`    |The value associated with key `k` in map `ms`, or the default value `d` if not found.|
|  `ms contains k`  	    |Tests whether `ms` contains a mapping for key `k`.|
|  `ms isDefinedAt k`  	    |Same as `contains`.                             |    
| **Additions and Updates:**|						     |
|  `ms + (k -> v)`          |The map containing all mappings of `ms` as well as the mapping `k -> v` from key `k` to value `v`.|
|  `ms + (k -> v, l -> w)`  |The map containing all mappings of `ms` as well as the given key/value pairs.|
|  `ms ++ kvs`              |The map containing all mappings of `ms` as well as all key/value pairs of `kvs`.|
|  `ms updated (k, v)`      |Same as `ms + (k -> v)`.|
| **Removals:**             |						     |
|  `ms - k`  	            |The map containing all mappings of `ms` except for any mapping of key `k`.|  
|  `ms - (k, 1, m)`  	    |The map containing all mappings of `ms` except for any mapping with the given keys.|    
|  `ms -- ks`  	            |The map containing all mappings of `ms` except for any mapping with a key in `ks`.|    
|   **Subcollections:**     |						     |
|  `ms.keys`  	            |An iterable containing each key in `ms`.        |
|  `ms.keySet`              |A set containing each key in `ms`.              |
|  `ms.keyIterator`         |An iterator yielding each key in `ms`.          |
|  `ms.values`      	    |An iterable containing each value associated with a key in `ms`.|
|  `ms.valuesIterator`      |An iterator yielding each value associated with a key in `ms`.|
|   **Transformation:**     |						     |
|  `ms filterKeys p`        |A map view containing only those mappings in `ms` where the key satisfies predicate `p`.|
|  `ms mapValues f`         |A map view resulting from applying function `f` to each value associated with a key in `ms`.|

Mutable maps support in addition the operations summarized in the following table.


### Operations in Class mutable.Map ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Additions and Updates:**|						     |
|  `ms(k) = v`              |(Or, written out, `ms.update(x, v)`). Adds mapping from key `k` to value `v` to map ms as a side effect, overwriting any previous mapping of `k`.|
|  `ms += (k -> v)`         |Adds mapping from key `k` to value `v` to map `ms` as a side effect and returns `ms` itself.|
|  `ms += (k -> v, l -> w)` |Adds the given mappings to `ms` as a side effect and returns `ms` itself.|
|  `ms ++= kvs`             |Adds all mappings in `kvs` to `ms` as a side effect and returns `ms` itself.|
|  `ms put (k, v)`          |Adds mapping from key `k` to value `v` to `ms` and returns any value previously associated with `k` as an option.|
|  `ms getOrElseUpdate (k, d)`|If key `k` is defined in map `ms`, return its associated value. Otherwise, update `ms` with the mapping `k -> d` and return `d`.|
|  **Additions and Updates:**|						     |
|  `ms -= k`                |Removes mapping with key `k` from ms as a side effect and returns `ms` itself.|
|  `ms -= (k, l, m)`        |Removes mappings with the given keys from `ms` as a side effect and returns `ms` itself.|
|  `ms --= ks`              |Removes all keys in `ks` from `ms` as a side effect and returns `ms` itself.|
|  `ms remove k`            |Removes any mapping with key `k` from `ms` and returns any value previously associated with `k` as an option.|
|  `ms retain p`            |Keeps only those mappings in `ms` that have a key satisfying predicate `p`.|
|  `ms.clear()`             |Removes all mappings from `ms`.                 |
|  **Transformation:**      |						     |
|  `ms transform f`         |Transforms all associated values in map `ms` with function `f`.|
|  **Cloning:**             |						     |
|  `ms.clone`               |Returns a new mutable map with the same mappings as `ms`.|

The addition and removal operations for maps mirror those for sets. As is the for sets, mutable maps also support the non-destructive addition operations `+`, `-`, and `updated`, but they are used less frequently because they involve a copying of the mutable map. Instead, a mutable map `m` is usually updated "in place", using the two variants `m(key) = value` or `m += (key -> value)`. There are is also the variant `m put (key, value)`, which returns an `Option` value that contains the value previously associated with `key`, or `None` if the `key` did not exist in the map before.

The `getOrElseUpdate` is useful for accessing maps that act as caches. Say you have an expensive computation triggered by invoking a function `f`:

    scala> def f(x: String) = { 
           println("taking my time."); sleep(100)
           x.reverse }
    f: (x: String)String

Assume further that `f` has no side-effects, so invoking it again with the same argument will always yield the same result. In that case you could save time by storing previously computed bindings of argument and results of `f` in a map and only computing the result of `f` if a result of an argument was not found there. One could say the map is a _cache_ for the computations of the function `f`.

    val cache = collection.mutable.Map[String, String]()
    cache: scala.collection.mutable.Map[String,String] = Map()

You can now create a more efficient caching version of the `f` function:

    scala> def cachedF(s: String) = cache.getOrElseUpdate(s, f(s))
    cachedF: (s: String)String
    scala> cachedF("abc")
    taking my time.
    res3: String = cba
    scala> cachedF("abc")
    res4: String = cba

Note that the second argument to `getOrElseUpdate` is "by-name", so the computation of `f("abc")` above is only performed if `getOrElseUpdate` requires the value of its second argument, which is precisely if its first argument is not found in the `cache` map. You could also have implemented `cachedF` directly, using just basic map operations, but it would take more code to do so:

    def cachedF(arg: String) = cache get arg match {
      case Some(result) => result
      case None => 
        val result = f(x)
        cache(arg) = result
        result
    }

### Synchronized Sets and Maps ###

To get a thread-safe mutable map, you can mix the `SynchronizedMap` trait trait into whatever particular map implementation you desire. For example, you can mix `SynchronizedMap` into `HashMap`, as shown in the code below. This example begins with an import of two traits, `Map` and `SynchronizedMap`, and one class, `HashMap`, from package `scala.collection.mutable`. The rest of the example is the definition of singleton object `MapMaker`, which declares one method, `makeMap`. The `makeMap` method declares its result type to be a mutable map of string keys to string values.

      import scala.collection.mutable.{Map,
          SynchronizedMap, HashMap}
      object MapMaker {
        def makeMap: Map[String, String] = {
            new HashMap[String, String] with
                SynchronizedMap[String, String] {
              override def default(key: String) =
                "Why do you want to know?"
            }
        }
      }

<center>Mixing in the `SynchronizedMap` trait.</center>

The first statement inside the body of `makeMap` constructs a new mutable `HashMap` that mixes in the `SynchronizedMap` trait:

    new HashMap[String, String] with
      SynchronizedMap[String, String]

Given this code, the Scala compiler will generate a synthetic subclass of `HashMap` that mixes in `SynchronizedMap`, and create (and return) an instance of it. This synthetic class will also override a method named `default`, because of this code:

    override def default(key: String) =
      "Why do you want to know?"

If you ask a map to give you the value for a particular key, but it doesn't have a mapping for that key, you'll by default get a `NoSuchElementException`. If you define a new map class and override the `default` method, however, your new map will return the value returned by `default` when queried with a non-existent key. Thus, the synthetic `HashMap` subclass generated by the compiler from the code in the synchronized map code will return the somewhat curt response string, `"Why do you want to know?"`, when queried with a non-existent key.

Because the mutable map returned by the `makeMap` method mixes in the `SynchronizedMap` trait, it can be used by multiple threads at once. Each access to the map will be synchronized. Here's an example of the map being used, by one thread, in the interpreter:

    scala> val capital = MapMaker.makeMap  
    capital: scala.collection.mutable.Map[String,String] = Map()
    scala> capital ++ List("US" -> "Washington",
            "Paris" -> "France", "Japan" -> "Tokyo")
    res0: scala.collection.mutable.Map[String,String] =
      Map(Paris -> France, US -> Washington, Japan -> Tokyo)
    scala> capital("Japan")
    res1: String = Tokyo
    scala> capital("New Zealand")
    res2: String = Why do you want to know?
    scala> capital += ("New Zealand" -> "Wellington")
    scala> capital("New Zealand")                    
    res3: String = Wellington

You can create synchronized sets similarly to the way you create synchronized maps. For example, you could create a synchronized `HashSet` by mixing in the `SynchronizedSet` trait, like this:

    import scala.collection.mutable
    val synchroSet =
      new mutable.HashSet[Int] with
          mutable.SynchronizedSet[Int]

Finally, if you are thinking of using synchronized collections, you may also wish to consider the concurrent collections of `java.util.concurrent` instead.
