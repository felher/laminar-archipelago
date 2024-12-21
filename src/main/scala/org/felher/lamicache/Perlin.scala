package org.felher.lamicache

import scala.language.experimental.namedTuples

object Perlin:
  type Layer = (frequency: Double, persistence: Double)

  def generatePerlin(
      seed: Int,
      sideLength: Int,
      layer: Layer,
      layers: Layer*
  ): Perlin =
    val random = new scala.util.Random(seed)

    val initial = (
      perlin = noise(layer.frequency, sideLength, random) * layer.persistence,
      persistence = layer.persistence
    )

    layers
      .foldLeft(initial)((acc, layer) =>
        val newPerlin =
          acc.perlin +
            noise(layer.frequency, sideLength, random) * layer.persistence * acc.persistence

        (
          perlin = newPerlin,
          persistence = acc.persistence * layer.persistence
        )
      )
      .perlin

  final case class Vec(x: Double, y: Double):
    def dot(that: Vec): Double = x * that.x + y * that.y

  final case class Perlin(
      numPixels: Int,
      cornerVectors: Array[Array[Vec]],
      values: Array[Array[Double]]
  ):
    lazy val min = values.flatten.min
    lazy val max = values.flatten.max

    def +(that: Perlin): Perlin =
      Perlin(
        numPixels,
        cornerVectors,
        Array.tabulate(numPixels, numPixels): (y, x) =>
          values(y)(x) + that.values(y)(x)
      )

    def *(factor: Double): Perlin =
      Perlin(
        numPixels,
        cornerVectors,
        Array.tabulate(numPixels, numPixels): (y, x) =>
          values(y)(x) * factor
      )

  var min = 0.0
  var max = 0.0

  val rand = new scala.util.Random()

  def noise(frequency: Double, numPixels: Int, random: scala.util.Random): Perlin =

    val cornerVectors =
      val numGridLines = frequency.ceil.toInt + 1

      Array.tabulate(numGridLines, numGridLines): (y, x) =>
        val angle = random.nextDouble() * 2 * Math.PI
        Vec(Math.cos(angle), Math.sin(angle))

    val values = Array.tabulate(numPixels, numPixels): (y, x) =>
      val cornerX = (x * frequency / numPixels).toInt
      val cornerY = (y * frequency / numPixels).toInt

      val relCellX  = (x - cornerX * (numPixels / frequency)) / (numPixels / frequency).toDouble
      val relCellY  = (y - cornerY * (numPixels / frequency)) / (numPixels / frequency).toDouble
      val relCellXF = fade(relCellX)
      val relCellYF = fade(relCellY)
      // println(s"relCellX: $relCellX")

      val nwCornerVec = cornerVectors(cornerY)(cornerX)
      val neCornerVec = cornerVectors(cornerY)(cornerX + 1)
      val swCornerVec = cornerVectors(cornerY + 1)(cornerX)
      val seCornerVec = cornerVectors(cornerY + 1)(cornerX + 1)

      val nwInfluence = nwCornerVec.dot(Vec(relCellX, relCellY))
      val neInfluence = neCornerVec.dot(Vec(relCellX - 1, relCellY))
      val swInfluence = swCornerVec.dot(Vec(relCellX, relCellY - 1))
      val seInfluence = seCornerVec.dot(Vec(relCellX - 1, relCellY - 1))

      val topInfluence    = lerp(nwInfluence, neInfluence, relCellXF)
      val bottomInfluence = lerp(swInfluence, seInfluence, relCellXF)
      val influence       = lerp(topInfluence, bottomInfluence, relCellYF)

      val newMin = Math.min(min, influence)
      val newMax = Math.max(max, influence)
      if newMin != min || newMax != max then
        min = newMin
        max = newMax
        // println(s"min: $min, max: $max")

      influence

    Perlin(
      numPixels,
      cornerVectors,
      values
    )

  private def lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
  private def fade(t: Double): Double                       = 6 * Math.pow(t, 5) - 15 * Math.pow(t, 4) + 10 * Math.pow(t, 3)

  def step(value: Double, steps: Double): Double =
    val stepSize = 1 / steps
    val step     = (value / stepSize).toInt
    step * stepSize
