package org.felher.lamicache

import scala.language.experimental.namedTuples
import scala.scalajs.js

object Worker:
  def sayIt(): Unit =
    org.scalajs.dom.DedicatedWorkerGlobalScope.self.onmessage = { (event: org.scalajs.dom.MessageEvent) =>
      val seed  = event.data.asInstanceOf[Double].toInt
      val image = MapGen.gen(seed)
      org.scalajs.dom.DedicatedWorkerGlobalScope.self.asInstanceOf[js.Dynamic].postMessage(image, js.Array(image))
    }
