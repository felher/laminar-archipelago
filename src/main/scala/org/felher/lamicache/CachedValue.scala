package org.felher.lamicache

import scala.scalajs.js.WeakRef

enum CachedValue[+Value] derives CanEqual:
  case Retrieving                    extends CachedValue[Nothing]
  case Cached(value: WeakRef[Value]) extends CachedValue[Value]

  def asCached: Option[WeakRef[Value]] = this match
    case Cached(value) => Some(value)
    case _             => None
