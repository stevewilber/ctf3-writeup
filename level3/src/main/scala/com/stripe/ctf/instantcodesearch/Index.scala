package com.stripe.ctf.instantcodesearch

import java.io._
import java.nio.file._
import scala.collection.mutable._
import scala.collection.JavaConversions._
import scala.util.control.Breaks._

class Index(repoPath: String, id: Int) extends Serializable {
  var fileIds: Map[Int, String] = Map[Int, String]()
  var wordMap: Map[String, String] = Map[String, String]()
  var subStringsMap: Set[String] = findSubstrings()

  var fileId = 0;

  def path() = repoPath

  def addFile(file: String, text: String) {
    println("adding file: " + file + ", " + fileId)
    fileIds(fileId) = file;

    text.split("\n").zipWithIndex.
      map { case (l, n) => mapLine(fileId, l, n+1) }

    fileId += 1
  }

  def mapLine(fileId: Int, line: String, lineNum: Int) {
    line.split("[\\s\\.]+").
      map (word => mapWord(word, fileId, lineNum))
  }

  def mapWord(word: String, fileId: Int, line: Int) {
    if(subStringsMap.contains(word)) {
      if (!(wordMap contains word)) {
        wordMap(word) = fileId + ":" + line
      } else {
        wordMap(word) = wordMap(word) + "," + fileId + ":" + line
      }
    }
  }

  def findSubstrings() : Set[String] = {
    println("building substrings")
    val subStrings = Set[String]()
    var lines = Files.readAllLines(FileSystems.getDefault().getPath("/usr/share/dict/words"), java.nio.charset.Charset.forName("UTF-8"));
    lines.map(word => {
        subStrings.add(word)
      }
    )    
    println("done building substrings")
    return subStrings
  }

  def write(out: File) {
    val stream = new FileOutputStream(out)
    write(stream, this)
    stream.close
  }

  def write(out: OutputStream, obj: Object) {
    val w = new ObjectOutputStream(out)
    w.writeObject(obj)
    w.close
  }
}

