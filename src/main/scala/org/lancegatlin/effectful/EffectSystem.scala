package org.lancegatlin.effectful

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

/**
  * A type-class for an effect system's monad that can be used to
  * capture and isolate the effects of a computation
  *
  * @tparam E monad type
  */
trait EffectSystem[E[_]] {
  def map[A,B](m: E[A], f: A => B) : E[B]
  def flatMap[A,B](m: E[A], f: A => E[B]) : E[B]

  /**
    * Create an instance of E that can capture the effects of
    * a computation of a value
    *
    * Note1: computation may fail with an exception
    * Note2: parameter must be lazy to allow for capture of effects
    *
    * @param a computation
    * @tparam A type of result of computation
    * @return an instance of E that can capture the effects of the computation
    */
  def apply[A](a: => A) : E[A]

  def sequence[F[AA] <: Traversable[AA],A](fea: F[E[A]]) : E[F[A]]

  def delay(duration: FiniteDuration) : E[Unit]
}