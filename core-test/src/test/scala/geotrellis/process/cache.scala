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


package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import scala.concurrent.Lock
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.ExecutionContextExecutorService

class LRUCacheSpec extends FunSpec with MustMatchers {
  
  def f(x: Int):Int = x*100 

  def hashCacheTest(mkCache: Int => HashBackedCache[Int,Int]) {
    describe("Hash Cache Test Cache") {
      
      def getCache(max:Int = 5) = 
        mkCache(max)
      
      it("should be able to handle simple set/get case") {
        val cache = getCache()
        
        for(j <- 1 to 5)
          cache.getOrInsert(j,f(j))
        
        for(j <- 1 to 5)
          cache.lookup(j) must equal(Some(f(j)))
      }
      
      it("should not update if using getOrInsert multiple times") {
        val cache = getCache()
        
        for(j <- 1 to 100)
          cache.getOrInsert(1,f(j))
        
        cache.cache.size must equal(1)
        cache.lookup(1) must equal(Some(f(1))) // Pairs are assume to be immutable
      }
    }
  }

  def boundedCacheTest(mkCache: Int => BoundedCache[Int,Int]) {

    describe("bounded caches") {

      it("should evict from cache") {
        val cache = mkCache(1) // Max size 1
        
        cache.getOrInsert(1, f(1))
        cache.lookup(1) must equal(Some(f(1)))
        
        cache.getOrInsert(2, f(2))
        cache.lookup(2) must equal(Some(f(2)))
        cache.lookup(1) must equal(None)
      }

    }

  }


  describe("LRU cache") {
    
    def getCache(max: Int) = 
      new LRUCache[Int,Int](max, (v: Int) => 1)
     
    hashCacheTest(getCache _)
    boundedCacheTest(getCache _)
 
    it("should be contain at most maxSize number of items") {
      val cache = getCache(5)
      
      for(j <- 1 to 100)
        cache.getOrInsert(j,f(j))
      
        cache.cache.size must equal(5)
      
      for(j <- 1 to 95)
        cache.lookup(j) must equal(None)
      for(j <- 96 to 100)
        cache.lookup(j) must equal(Some(f(j)))
    }
    
    it("should evict the item accessed longest ago") {
      val cache = getCache(2)
      
      cache.insert(1, f(1))
      cache.insert(2, f(2))
      cache.lookup(1) must equal(Some(f(1)))
      cache.lookup(2) must equal(Some(f(2)))
        
      cache.lookup(1) // Make 1 newer than 2
        
      cache.insert(3, f(3))
      cache.lookup(1) must equal(Some(f(1)))
      cache.lookup(2) must equal(None)
      cache.lookup(3) must equal(Some(f(3)))
      
      cache.insert(4, f(4))
      cache.lookup(1) must equal(None)
      cache.lookup(2) must equal(None)
      cache.lookup(3) must equal(Some(f(3)))
      cache.lookup(4) must equal(Some(f(4)))
      
    }
  }

  describe("MRU cache") {
    
    def getCache(max: Int) = 
      new MRUCache[Int,Int](max, (v: Int) => 1)
     
    hashCacheTest(getCache _)
    boundedCacheTest(getCache _)
 
    it("should be contain at most maxSize number of items") {
      val cache = getCache(5)
      
      for(j <- 1 to 100)
        cache.getOrInsert(j,f(j))
      
        cache.cache.size must equal(5)
      
      for(j <- 5 to 99)
        cache.lookup(j) must equal(None)
      for(j <- 1 to 4)
        cache.lookup(j) must equal(Some(f(j)))
      cache.lookup(100) must equal(Some(f(100)))
    }
    
    it("should evict the item accessed most recently") {
      val cache = getCache(2)
      
      cache.insert(1, f(1))
      cache.insert(2, f(2))
      cache.lookup(1) must equal(Some(f(1)))
      cache.lookup(2) must equal(Some(f(2)))
        
      cache.lookup(1) // Make 1 newer than 2
        
      cache.insert(3, f(3))
      cache.lookup(1) must equal(None)
      cache.lookup(2) must equal(Some(f(2)))
      cache.lookup(3) must equal(Some(f(3)))
      
      cache.insert(4, f(4))
      cache.lookup(1) must equal(None)
      cache.lookup(2) must equal(Some(f(2)))
      cache.lookup(3) must equal(None)
      cache.lookup(4) must equal(Some(f(4)))
      
    }
  }



  describe("atomic cache") {
    
    val cache = new LRUCache[Int,Int](10, (v: Int) => 1)
  
    var i = 1
    var counter = 3
    var brokenEval = false
    val lock = new Lock()

    val x = 5

    def mkTask(t: () => Unit) = new Runnable() {
      def run() = {
        t()
        lock.acquire
        counter -= 1
        lock.release
      }
    }
  
    def taskA = mkTask (() => {
      def execA() = { 
        Thread.sleep(3000 / x)
        2
      }
      cache.getOrInsert(1, execA())
    })
  
    def taskB = mkTask(() => { 
      def execB() = { brokenEval = true; 3 }
      Thread.sleep(1000 / x)
      cache.getOrInsert(1, execB())
    })
  
    def taskC = mkTask(() => {
      Thread.sleep(500 / x)
      cache.getOrInsert(10, 20)
    })
  
    it("should work") {
      val t = scala.concurrent.ExecutionContext.global
      t.execute(taskA)
      t.execute(taskB)
      t.execute(taskC)
  
      var time = 0
      while(counter > 0 && time < 15000) {
        Thread.sleep(100)
        time += 100
      }
  
      cache.lookup(1) must equal(Some(2))
      cache.lookup(10) must equal(Some(20))
      counter must equal(0)
      brokenEval must equal(false)
      
      //t.shutdown()
    }
  }
}
