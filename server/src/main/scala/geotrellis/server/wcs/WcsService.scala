package geotrellis.server.wcs

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.scalaxml._

import cats.data.Validated
import cats.effect._
import Validated._
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.ConfigFactory

import geotrellis.spark.io.AttributeStore
import geotrellis.server.wcs.params._
import geotrellis.server.wcs.ops._
import geotrellis.spark._
import geotrellis.spark.io._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.xml.NodeSeq
import java.net.URI

object WcsService {
  type MetadataCatalog = Map[String, (Seq[Int], Option[TileLayerMetadata[SpatialKey]])]
}

class WcsService(catalog: URI) extends Http4sDsl[IO] with LazyLogging {

  val catalogMetadata = {
    val as: AttributeStore = AttributeStore(catalog)

    logger.info(s"Loading metadata for catalog at ${catalog} ...")
    as
      .layerIds
      .sortWith{ (a, b) => a.name < b.name || (a.name == b.name && a.zoom > b.zoom) }
      .groupBy(_.name)
      .mapValues(_.map(_.zoom))
      .map{ case (name, zooms) => {
        println(s"  -> $name @ zoom=${zooms.head}")
        val metadata = Try(as.readMetadata[TileLayerMetadata[SpatialKey]](LayerId(name, zooms.head))).toOption
        name -> (zooms, metadata)
      }}
  }

  def routes: HttpService[IO] = HttpService[IO] {
    case req @ GET -> Root =>
      logger.info(s"Request received: ${req.uri}")
      WCSParams(req.multiParams) match {
        case Invalid(errors) =>
          val msg = WCSParamsError.generateErrorMessage(errors.toList)
          logger.debug(s"""Error parsing parameters: ${msg}""")
          Ok(s"""Error parsing parameters: ${msg}""")
        case Valid(wcsParams) =>
          wcsParams match {
            case p: GetCapabilitiesWCSParams =>
              val link = s"${req.uri.scheme}://${req.uri.authority}${req.uri.path}?"
              println(s"GetCapabilities request arrived at $link")
              Ok(GetCapabilities.build(link, catalogMetadata, p))
            case p: DescribeCoverageWCSParams =>
              println(s"DescribeCoverage request arrived at $req.uri")
              Ok(DescribeCoverage.build(catalogMetadata, p))
            case p: GetCoverageWCSParams =>
              println(s"GetCoverage request arrived at $req.uri")
              Ok(GetCoverage.build(catalogMetadata, p))
          }
      }
  }
}
