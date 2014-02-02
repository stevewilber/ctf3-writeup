package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._

import com.twitter.concurrent.Broker

abstract class SearchResult
case class Match(path: String, line: String) extends SearchResult
case class Done() extends SearchResult

class Searcher(indexPath : String)  {
  val index : Index = readIndex(indexPath)
  val root = FileSystems.getDefault().getPath(index.path)
  var fileMap = scala.collection.mutable.Map[String, String]()

  def search(needle : String, b : Broker[SearchResult]) = {
    println("needle: " + needle)
    for (key <- index.subStringsMap) {
      if (key.contains(needle)) {
        for (m <- tryPath(key)) {
          b !! m
        }
      }
    }

    b !! new Done()
  }

  def tryPath(key: String) : Iterable[SearchResult] = {
    println("Searching for: " + key)
    if (index.wordMap contains key) { 
      println("in index: " + index.wordMap(key))
      return index.wordMap(key).split(',').
        map { matchLocation => new Match(index.fileIds(matchLocation.split(":")(0).toInt), matchLocation.split(":")(1))}.toList
    } else {
      println("not in index")
    }

    return Nil;    
  }

  def readIndex(path: String) : Index = {
    new ObjectInputStream(new FileInputStream(new File(path))).readObject.asInstanceOf[Index]
  }
}
