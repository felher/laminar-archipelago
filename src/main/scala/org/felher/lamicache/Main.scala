package org.felher.lamicache
import com.raquo.laminar.api.L.*
import org.felher.beminar.Bem
import scala.language.experimental.namedTuples
import scala.scalajs.js.annotation.JSImport
import org.scalajs.dom.ImageBitmap
import scala.concurrent.Promise
import scala.concurrent.Future

object Main:
  val bem = Bem("/app")
  val prompt = true

  @scalajs.js.native()
  @JSImport("scalajs:worker.js?worker", JSImport.Default)
  val worker: scalajs.js.Dynamic = scalajs.js.native

  @main def run: Unit =
    renderMain()
    org.scalajs.dom.window.console.log(worker)

  private def renderMain(): Unit =
    val _ = org.scalajs.dom.window.setInterval(() => cache.sampleSize(), 5000)
    val _ = render(
      org.scalajs.dom.document.body,
      div(
        bem,
        Histo.anchor,
        div(
          bem("/stats"),
          List(
            ("Misses", cache.stats.signal.map(_.prettyMisses)),
            ("Hits", cache.stats.signal.map(_.prettyHits)),
            ("HitR", cache.stats.signal.map(_.prettyHitRatio)),
            ("Pre-Evicted", cache.stats.signal.map(_.prettyPreEvicted)),
            ("Evicted", cache.stats.signal.map(_.prettyEvicted)),
            ("Size", cache.stats.signal.map(_.prettySize)),
          ).map((name, sig) =>
            div(
              bem("/stat"),
              div(bem("/stat-name"), name),
              div(
                bem("/stat-value"),
                child.text <-- sig.map(_.toString)
              ),
              inContext: ctx =>
                sig.changes.distinct --> (_ =>
                  val elem = ctx.ref.asInstanceOf[org.scalajs.dom.html.Div]
                  elem.classList.remove("animate")
                  val _    = elem.offsetWidth
                  elem.classList.add("animate")
                )
            ),
          )
        ),
        div(
          bem("/islands"),
          Scroller(if !prompt then 50 else org.scalajs.dom.window.prompt("Speed").toDouble),
          (0 until 10000).toList.map(i =>
            if i == resetCount then
              val inView = InView(0.25)
              var firedAlready = false
              renderIsland(i).amend(
                inView.mount(),
                inView.inView.signal --> (value =>
                  if !firedAlready && value then
                    firedAlready = true
                    cache.resetStats()
                  ),
                )
            else
              renderIsland(i)
          )
        )
      )
    )

  private val cache = LamiCache(
    getForSeed,
    if !prompt then 10 else org.scalajs.dom.window.prompt("Cache size").toInt,
  )

  private val resetCount = if !prompt then 1000 else org.scalajs.dom.window.prompt("Reset count").toInt
  private val rand = scala.util.Random(0)

  Histo.histo.set(Map(-10 -> 1, 0 -> 2, 1 -> 3, 2 -> 2, 3 -> 10))

  private def gaussianRange(range: Int): Int =
    // do it once since newly created Random instances are not very random
    // ^^ the quality of randomness for the first value is poor.
    val gauss = rand.nextGaussian()
    (gauss * range).toInt

  private def renderIsland(seed: Int): HtmlElement =
    val inView = InView(0.1 + (seed % 5)/10.0)
    val gaussSeed = gaussianRange(50)
    div(
      bem("/island"),
      inView.mount(),
      child <-- inView.inView.signal.map:
        case false => div()
        case true  =>
          canvasTag(
            bem("/island-canvas"),
            inContext: ctx =>
              cache.signal(gaussSeed) --> (imageBitmap =>
                val ctx2d = ctx.ref.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
                imageBitmap match
                  case None              =>
                    ctx.ref.width = 500
                    ctx.ref.height = 500
                    ctx2d.fillStyle = "oklch(94.26% 0.1 145.82)"
                    ctx2d.fillRect(0, 0, 500, 500)
                  case Some(imageBitmap) =>
                    ctx.ref.width = imageBitmap.width.toInt
                    ctx.ref.height = imageBitmap.height.toInt
                    val _ = ctx2d.asInstanceOf[scalajs.js.Dynamic].drawImage(imageBitmap, 0, 0)
              )
          ),
      div(bem("/island-seed"), gaussSeed.toString),
    )

  private def getForSeed(seed: Int): Future[Option[ImageBitmap]] =
    val promise   = Promise[Option[ImageBitmap]]()
    val newWorker = worker()

    newWorker.onmessage = (event: org.scalajs.dom.MessageEvent) =>
      val bitMap = event.data.asInstanceOf[org.scalajs.dom.ImageBitmap]
      promise.success(Some(bitMap))
      newWorker.terminate()

    val _ = newWorker.postMessage(seed)

    promise.future
