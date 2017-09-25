package com.azavea.maml.spark.eval

import com.azavea.maml.ast._
import com.azavea.maml.error._
import com.azavea.maml.eval._
import com.azavea.maml.eval.tile._
import com.azavea.maml.spark.ast._

import geotrellis.raster._
import geotrellis.spark._

import cats.data.{NonEmptyList => NEL, _}
import Validated._

import org.apache.spark.rdd._

import scala.reflect.ClassTag


case class SpatialRDDResult(res: TileLayerRDD[SpatialKey]) extends Result {
  def as[T](implicit ct: ClassTag[T]): Interpreted[T] = {
    val cls = ct.runtimeClass
    if (classOf[ContextRDD[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]] isAssignableFrom cls)
      Valid(res.asInstanceOf[T])
    else
      Invalid(NEL.of(EvalTypeError(cls.getName, List("SpatialRDD"))))
  }
  def kind: MamlKind = MamlKind.Tile
}
