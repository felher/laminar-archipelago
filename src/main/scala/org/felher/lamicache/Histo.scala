package org.felher.lamicache

import com.raquo.laminar.api.L.*
import org.felher.beminar.Bem

object Histo:
  val histo = Var(Map.empty[Int, Int])
  val bem   = Bem("/histo")

  val anchor = div(
    bem,
    inContext: ctx =>
      histo.signal.changes --> (_ =>
        val elem = ctx.ref.asInstanceOf[org.scalajs.dom.html.Div]
        elem.classList.remove("histo--animate")
        val _    = elem.offsetWidth
        elem.classList.add("histo--animate")
      ),
    children <-- histo.signal.map: histo =>
      if histo.isEmpty then List(div())
      else
        val min    = histo.keys.min
        val max    = histo.keys.max
        val num    = max - min + 1
        val valMax = histo.values.max
        (min to max).toList.map: i =>
          val count = histo.getOrElse(i, 0)
          val pct   = count.toDouble / valMax
          div(
            bem("/bar"),
            styleAttr(s"""
                height           : ${String.format("%.2f", pct * 80)}%;
                width            : ${String.format("%.2f", 100 / num.toDouble)}%;
              """),
            ""
          ),
  )
