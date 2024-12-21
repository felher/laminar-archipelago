package org.felher.lamicache

import scala.language.experimental.namedTuples
import org.scalajs.dom.ImageBitmap
import org.scalajs.dom.OffscreenCanvas

object MapGen:
  private val sand      = "oklch(94.81% 0.1164 110.03)"
  private val land      = List(
    "blue"      -> 11.0,
    sand        -> 0.5,
    "green"     -> 2.0,
    "darkgray"  -> 2.0,
    "lightgray" -> 2.0,
    "white"     -> 1.0
  )
  private val landColor = stepper(land*)

  def gen(seed: Int): ImageBitmap =
    val sideLength = 300
    val initFreq   = scala.util.Random(seed).nextInt(5) + 1
    val canvas     = new OffscreenCanvas(sideLength, sideLength)
    val ctx        = canvas.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
    val p          =
      Perlin.generatePerlin(
        seed,
        sideLength,
        //@formatter:off
        (frequency = initFreq, persistence = 0.5  ),
        (frequency =       10, persistence = 0.25 ),
        (frequency =       20, persistence = 0.125),
        (frequency =       40, persistence = 0.125)
        //@formatter:on
      )

    for
      y <- 0 until sideLength
      x <- 0 until sideLength
    do
      val value = (p.values(y)(x) - p.min) / (p.max - p.min)
      ctx.fillStyle = landColor(value)
      ctx.fillRect(x, y, 1, 1)

    for
      cellY <- 0 until 5
      cellX <- 0 until 5
    do
      val cellWidth    = p.numPixels / 5
      val xStart       = cellX * cellWidth
      val yStart       = cellY * cellWidth
      val coordsInCell = for
        y <- yStart until yStart + cellWidth
        x <- xStart until xStart + cellWidth
      yield (x, y)

      val top   = coordsInCell.maxBy(c => p.values(c._2)(c._1))
      val river = followGradient(p, top._1, top._2)

      ctx.strokeStyle = "transparent"
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(top._1, top._2)
      river.foreach(v => ctx.lineTo(v.x, v.y))
      ctx.stroke()

    canvas.transferToImageBitmap()

  private def followGradient(p: Perlin.Perlin, x: Int, y: Int): List[Perlin.Vec] =
    def go(x: Int, y: Int, acc: List[Perlin.Vec]): List[Perlin.Vec] =
      val neighbours = List(
        (x - 1, y),
        (x + 1, y),
        (x, y - 1),
        (x, y + 1),
        (x - 1, y - 1),
        (x + 1, y - 1),
        (x - 1, y + 1),
        (x + 1, y + 1)
      )

      extension (t: (Int, Int))
        def xx: Int = t._1
        def yy: Int = t._2

      val next = neighbours
        .filter: n =>
          n.xx < p.numPixels && n.yy < p.numPixels && n.xx >= 0 && n.yy >= 0 &&
            p.values(n.yy)(n.xx) < p.values(y)(x)
        .minByOption(n => p.values(n.yy)(n.xx))

      next match
        case None    => acc.reverse
        case Some(n) => go(n.xx, n.yy, Perlin.Vec(x, y) :: acc)

    go(x, y, Nil)

  private def stepper(values: (String, Double)*)(t: Double): String =
    val totalWeight = values.map(_._2).sum
    val tScaled     = t * totalWeight

    def go(remaining: List[(String, Double)], acc: Double): String =
      remaining match
        case Nil                     => throw new Exception("stepper: t out of bounds")
        case (value, weight) :: tail =>
          if acc + weight >= tScaled then value
          else go(tail, acc + weight)

    go(values.toList, 0.0)
