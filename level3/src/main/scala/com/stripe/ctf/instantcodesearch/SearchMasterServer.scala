package com.stripe.ctf.instantcodesearch

import scala.collection.mutable._
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.util.CharsetUtil.UTF_8
import scala.collection.JavaConversions._

class SearchMasterServer(port: Int, id: Int) extends AbstractSearchServer(port, id) {
  val NumNodes = 3
  val random = new scala.util.Random();

  def this(port: Int) { this(port, 0) }

  val clients = (1 to NumNodes)
    .map { id => new SearchServerClient(port + id, id)}
    .toArray

  override def isIndexed() = {
    val responsesF = Future.collect(clients.map {client => client.isIndexed()})
    val successF = responsesF.map {responses => responses.forall { response =>

        (response.getStatus() == HttpResponseStatus.OK
          && response.getContent.toString(UTF_8).contains("true"))
      }
    }
    successF.map {success =>
      if (success) {
        successResponse()
      } else {
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "Nodes are not indexed")
      }
    }.rescue {
      case ex: Exception => Future.value(
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "Nodes are not indexed")
      )
    }
  }

  override def healthcheck() = {
    val responsesF = Future.collect(clients.map {client => client.healthcheck()})
    val successF = responsesF.map {responses => responses.forall { response =>
        response.getStatus() == HttpResponseStatus.OK
      }
    }
    successF.map {success =>
      if (success) {
        successResponse()
      } else {
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      }
    }.rescue {
      case ex: Exception => Future.value(
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      )
    }
  }

  override def index(path: String) = {
    System.err.println(
      "[master] Requesting " + NumNodes + " nodes to index path: " + path
    )

    val responses = Future.collect(clients.map {client => client.index(path)})
    responses.map {_ => successResponse()}
  }

  override def query(q: String) = {
    val results : Set[Match] = Set[Match]()
    val pattern = """(?s).*\[(.+)\].*""".r

    val responsesF = Future.collect(clients.map {client => client.query(q)})

    //merge results from the 3 nodes
    val successF = responsesF.map {responses => responses.forall { 
        response => //println("response: " + response.getContent.toString(UTF_8)); 
          pattern.findAllIn(response.getContent.toString(UTF_8)).matchData foreach {
            m => //println("match: " + m.group(1)); 
              results ++= createMatches(m.group(1)).toSet //TODO why are there dups?
        }
        response.getStatus() == HttpResponseStatus.OK
      }
    }
    successF.map {success =>
      if (success) {
        querySuccessResponse(results.toList.sortWith(sortPaths)) //TODO need to sort?
      } else {
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      }
    }.rescue {
      case ex: Exception => Future.value(
        errorResponse(HttpResponseStatus.BAD_GATEWAY, "All nodes are not up")
      )
    }


  }

  def createMatches(str : String) : List[Match] = {
    str.replaceAll("[\"\n]", "").split(",").toList.map(m => new Match(m.split(":")(0), m.split(":")(1)))
  }

  def sortPaths(s1: Match, s2: Match) : Boolean = {
    val s1Paths = s1.path.split("/");
    val s2Paths = s2.path.split("/")
    val paths : Array[(String, String)] = s1Paths.zip(s2Paths)

    for (path <- paths) {
      if(path._1 != path._2) {
        return path._1 < path._2
      }
    }

    if (s1Paths.length == s2Paths.length) {
      return s1.line < s2.line
    }

    return s1Paths.length < s2Paths.length
  }

}
