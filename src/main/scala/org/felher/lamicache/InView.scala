package org.felher.lamicache

import com.raquo.laminar.api.L.*
import scala.scalajs.js

class InView(threshold: Double):
  val inView = Var(false)

  def mount(): Modifier[HtmlElement] =
    onMountCallback: ctx =>
      val ioOptions = js.Dynamic.literal(threshold = threshold)

      val ioCallback = (entries: js.Array[js.Dynamic]) => inView.set(entries(0).isIntersecting.asInstanceOf[Boolean])

      val io = js.Dynamic.newInstance(js.Dynamic.global.IntersectionObserver)(ioCallback, ioOptions)
      val _  = io.observe(ctx.thisNode.ref)
