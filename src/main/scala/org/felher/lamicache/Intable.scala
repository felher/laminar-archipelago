package org.felher.lamicache

trait Intable[V]:
  def toInt(v: V): Int

object Intable:
  given Intable[Int]:
    def toInt(v: Int): Int = v
