package com.stripe.ctf.instantcodesearch

import java.io._
import java.util.Arrays
import java.nio.file._
import java.nio.charset._
import java.nio.file.attribute.BasicFileAttributes

class Indexer(indexPath: String, id: Int) {
  val root = FileSystems.getDefault().getPath(indexPath)
  val idx = new Index(root.toAbsolutePath.toString, id)

  def index() : Indexer = {
    Files.walkFileTree(root, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir : Path, attrs : BasicFileAttributes) : FileVisitResult = {
        if (Files.isHidden(dir) && dir.toString != ".")
          return FileVisitResult.SKIP_SUBTREE
        return FileVisitResult.CONTINUE
      }
      override def visitFile(file : Path, attrs : BasicFileAttributes) : FileVisitResult = {
        if (Files.isHidden(file))
          return FileVisitResult.CONTINUE
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
          return FileVisitResult.CONTINUE
        if (Files.size(file) > (1 << 20))
          return FileVisitResult.CONTINUE
        val bytes = Files.readAllBytes(file)
        if (Arrays.asList(bytes).indexOf(0) > 0)
          return FileVisitResult.CONTINUE
        val decoder = Charset.forName("UTF-8").newDecoder()
        decoder onMalformedInput CodingErrorAction.REPORT
        decoder onUnmappableCharacter CodingErrorAction.REPORT
        try {
          val r = new InputStreamReader(new ByteArrayInputStream(bytes), decoder)
          val strContents = slurp(r)
          val firstChar = file.toFile().getName().charAt(0)
          //split the files to be indexed across the 3 nodes
          if ((id == 1 && firstChar >= '0' && firstChar <= 'B') ||
              (id == 2 && firstChar >= 'C' && firstChar <= 'N') ||
              (id == 3 && firstChar >= 'O' && firstChar <= 'Z')) {
            idx.addFile(root.relativize(file).toString, strContents)
          }
        } catch {
          case e: IOException => {
            return FileVisitResult.CONTINUE
          }
        }

        return FileVisitResult.CONTINUE
      }
    })

    return this
  }

  def write(path: String) = {
    idx.write(new File(path))
  }
}
