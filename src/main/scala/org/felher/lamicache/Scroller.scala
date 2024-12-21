package org.felher.lamicache

import com.raquo.laminar.api.L.*

object Scroller:
  def apply(speed: Double): Modifier.Base =
    var active           = false
    var prevTime: Double = 0

    List(
      onMountCallback: ctx =>
        active = true
        def smoothScroolDown(hrts: Double): Unit =
          if prevTime == 0 then prevTime = hrts
          else
            val delta = hrts - prevTime
            prevTime = hrts
            val _     = ctx.thisNode.ref
              .asInstanceOf[scalajs.js.Dynamic]
              .scrollBy(
                scalajs.js.Dynamic.literal(
                  top = 1 * speed,
                  behavior = "smooth"
                )
              )
          if active then
            val _ = org.scalajs.dom.window.requestAnimationFrame(smoothScroolDown)

        org.scalajs.dom.window.setTimeout(
          () =>
            val _ = org.scalajs.dom.window.requestAnimationFrame(smoothScroolDown)
          ,
          5000
        )
      ,
      onUnmountCallback: ctx =>
        active = false
    )
