package org.felher.lamicache

import com.raquo.laminar.api.L.*
import scala.scalajs.js.WeakRef
import scala.scalajs.js.FinalizationRegistry
import scala.collection.mutable.HashMap
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LamiCache[Key, Value](
    reader: Key => Future[Option[Value]],
    cacheSize: Int
)(using Intable[Key]):
  val stats     = Var(CacheStats.zero)
  var cache     = HashMap.empty[Key, CachedValue[Value]]
  var listeners = HashMap.empty[Key, List[Try[Option[Value]] => Unit]]
  val cbCache   = CountingBloomCache[Key, Value](cacheSize)

  def resetStats(): Unit =
    stats.set(CacheStats.zero)

  def sampleSize(): Unit =
    stats.update(_.pushSize(cache.values.count(_.asCached.fold(false)(_.deref().isDefined))))

  val finalizationRegistry = FinalizationRegistry[Value, Key, Nothing]: key =>
    cache.get(key) match
      case None                          => ()
      case Some(CachedValue.Retrieving)  => ()
      case Some(CachedValue.Cached(ref)) =>
        if ref.deref().isEmpty then
          stats.update(_.incEvicted)
          val _ = cache.remove(key)

  def signal(key: Key): Signal[Option[Value]] =
    var listener: Try[Option[Value]] => Unit = null

    Signal.fromCustomSource(
      initial = scala.util.Success:
        cache
          .get(key)
          .flatMap(_.asCached)
          .flatMap(_.deref().toOption)
      ,
      start = (set, get, _, _) =>
        listener = set
        val _ = listeners.updateWith(key)(_.map(_ :+ listener).orElse(Some(List(listener))))
        reReadIfNotCached(key).foreach(v => set(scala.util.Success(Some(v))))
      ,
      stop = (_) =>
        val _ = listeners.updateWith(key)(_.map(_.filterNot(_ eq listener)))
    )

  private def reReadIfNotCached(key: Key): Option[Value] =
    cbCache.note(key)
    cache.get(key) match
      case None                          =>
        stats.update(_.incMisses)
        forceReRead(key)
        None
      case Some(CachedValue.Retrieving)  =>
        stats.update(_.incHits)
        None
      case Some(CachedValue.Cached(ref)) =>
        val value = ref.deref()
        if value.isEmpty then
          stats.update(_.incPreEvicted)
          stats.update(_.incMisses)
          forceReRead(key)
          None
        else
          stats.update(_.incHits)
          Some(value.get)

  private def forceReRead(key: Key): Unit =
    cache.update(key, CachedValue.Retrieving)

    reader(key).onComplete: value =>
      val _ = value match
        case scala.util.Success(Some(value)) =>
          finalizationRegistry.register(value, key)
          cbCache.offer(key, value)
          cache.update(key, CachedValue.Cached(WeakRef(value)))
        case scala.util.Success(None)        => cache.remove(key)
        case scala.util.Failure(f)           =>
          org.scalajs.dom.window.console.error(f)
          cache.remove(key)

      listeners.getOrElse(key, Nil).foreach(_(value))
