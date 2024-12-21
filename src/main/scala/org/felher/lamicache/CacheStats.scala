package org.felher.lamicache

final case class CacheStats(
    misses: Int,
    hits: Int,
    preEvicted: Int,
    evicted: Int,
    sizes: List[Int]
):
  def incMisses: CacheStats           = copy(misses = misses + 1)
  def incHits: CacheStats             = copy(hits = hits + 1)
  def incPreEvicted: CacheStats       = copy(preEvicted = preEvicted + 1)
  def incEvicted: CacheStats          = copy(evicted = evicted + 1)
  def pushSize(size: Int): CacheStats = copy(sizes = size :: sizes)

  def prettyMisses     = misses.toString
  def prettyHits       = hits.toString
  def prettyPreEvicted = preEvicted.toString
  def prettyEvicted    = evicted.toString

  def prettyHitRatio =
    if hits + misses == 0 then "0"
    else
      val ratio   = hits.toDouble / (hits + misses)
      val percent = (ratio * 100).toInt
      s"$percent%"

  def prettySize =
    val current = sizes.headOption.getOrElse(0)
    val total   = if sizes.isEmpty then 0 else sizes.sum / sizes.size

    String.format("%03d (%03d)", current, total)

object CacheStats:
  def zero: CacheStats = CacheStats(0, 0, 0, 0, Nil)
