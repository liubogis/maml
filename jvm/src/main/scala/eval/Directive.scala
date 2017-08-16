package maml.eval

import maml.ast._

import cats._
import cats.data._
import cats.data.Validated._
import cats.implicits._

import scala.reflect.ClassTag


object Directive {
  def apply(ruleFn: PartialFunction[Expression, Interpreted[Result]]): Directive = ruleFn
}

