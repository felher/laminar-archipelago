package org.felher.lamicache

import scala.collection.mutable.HashMap

class CountingBloomCache[K, V](maxSize: Int)(using I: Intable[K]):
  private val perfect  = HashMap.empty[K, Int]
  private val counters = Array.fill(2000)(0)
  private val values   = HashMap.empty[K, V]
  private var i        = 0

  def note(key: K): Unit =
    updateCounters(key)

  def offer(key: K, value: V): Unit =
    if maxSize == 0 then ()
    else if values.contains(key) then values.update(key, value)
    else if values.size < maxSize then values.update(key, value)
    else
      val (worstKey, worstScore) = getWorst
      if score(key) >= worstScore then
        values.update(key, value)
        val _ = values.remove(worstKey)

    i += 1
    if (i % 50 == 1) then
      println(s"keys: " + values.keys.toList.sortBy(I.toInt).map(k => s"$k (${score(k)})").mkString(", "))
      println(s"key: $key")
      println(s"score: ${score(key)}")
      println(perfect.toList.sortBy(-_._2).map((k, v) => s"$k: $v").mkString("{", ", ", "}"))
      Histo.histo.set(perfect.toMap.asInstanceOf)

  private def getWorst: (K, Int) =
    values.keySet.map(k => (k, score(k))).minBy(_._2)

  private def updateCounters(key: K): Unit =
    perfect.update(key, perfect.getOrElse(key, 0) + 1)
    CountingBloomCache.hashes.foreach: hash =>
      val bucket = hash(I.toInt(key)) % counters.size
      counters(bucket) = counters(bucket) + 1

  private def score(key: K): Int =
    val counts = CountingBloomCache.hashes.map(hash => counters(hash(I.toInt(key)) % counters.size))
    counts.min

object CountingBloomCache:
  val hashes = """
    [17 ed5ad4bb 11 ac4c1b51 15 31848bab 14] = 0.020888578919738908
    [16 aeccedab 14 ac613e37 16 19c89935 17] = 0.021246568167078764
    [16 236f7153 12 33cd8663 15 3e06b66b 16] = 0.021280991798512679
    [18 4260bb47 13 27e8e1ed 15 9d48a33b 15] = 0.021576730651802156
    [17 3f6cde45 12 51d608ef 16 6e93639d 17] = 0.021772288363808408
    [15 5dfa224b 14 4bee7e4b 17 930ee371 15] = 0.02184521628884813
    [17 3964f363 14 9ac3751d 16 4e8772cb 17] = 0.021883292578109576
    [16 66046c65 14 d3f0865b 16 f9999193 16] = 0.0219446068365007
    [16 b1a89b33 14 09136aaf 16 5f2a44a7 15] = 0.021998624107282542
    [16 24767aad 12 daa18229 16 e9e53beb 16] = 0.022043911220395354
    [15 42f91d8d 14 61355a85 15 dcf2a949 14] = 0.022052539152635078
    [15 4df8395b 15 466b428b 16 b4b2868b 16] = 0.022140187420461286
    [16 2bbed51b 14 cd09896b 16 38d4c587 15] = 0.022159936298777144
    [16 0ab694cd 14 4c139e47 16 11a42c3b 16] = 0.02220928191220355
    [17 7f1e072b 12 8750a507 16 ecbb5b5f 16] = 0.022283743052847804
    [16 f1be7bad 14 73a54099 15 3b85b963 15] = 0.022316544125749647
    [16 66e756d5 14 b5f5a9cd 16 84e56b11 16] = 0.022372957847491555
    [15 233354bb 15 ce1247bd 16 855089bb 17] = 0.022406591070966285
    [16 eb6805ab 15 d2c7b7a7 16 7645a32b 16] = 0.022427060650927547
    [16 8288ab57 14 0d1bfe57 16 131631e5 16] = 0.022431656871313443
    [16 45109e55 14 3b94759d 16 adf31ea5 17] = 0.022436433678417977
    [15 26cd1933 14 e3da1d59 16 5a17445d 16] = 0.022460520416491526
    [16 7001e6eb 14 bb8e7313 16 3aa8c523 15] = 0.022491767264054854
    [16 49ed0a13 14 83588f29 15 658f258d 15] = 0.022500668856510898
  """.trim
    .split("\n")
    .toList
    .zipWithIndex
    .map: (line, i) =>
      line.split("\\[|]")(1).split(" ") match
        case Array(n1, n2, n3, n4, n5, n6, n7) =>
          hashGen2(
            i,
            n1.toInt,
            java.lang.Long.parseLong(n2, 16).toInt,
            n3.toInt,
            java.lang.Long.parseLong(n4, 16).toInt,
            n5.toInt,
            java.lang.Long.parseLong(n6, 16).toInt,
            n7.toInt
          )

  // val hashes = List[Int => Int](
  //  hashGen1(0, 16, 0x7feb352d, 15, 0x846ca68b, 16),
  //  hashGen1(7, 16, 0x7feb352d, 15, 0x846ca68b, 16),
  //  hashGen1(31, 16, 0x7feb352d, 15, 0x846ca68b, 16),
  //  hashGen1(0, 15, 0xd168aaad, 15, 0xaf723597, 15),
  //  hashGen1(7, 15, 0xd168aaad, 15, 0xaf723597, 15),
  //  hashGen1(31, 15, 0xd168aaad, 15, 0xaf723597, 15),
  //  hashGen1(0, 17, 0x9e485565, 16, 0xef1d6b47, 16),
  //  hashGen1(7, 17, 0x9e485565, 16, 0xef1d6b47, 16),
  //  hashGen1(31, 17, 0x9e485565, 16, 0xef1d6b47, 16),
  //  hashGen1(0, 16, 0x604baa5d, 15, 0x43d6ce97, 15),
  //  hashGen1(7, 16, 0x604baa5d, 15, 0x43d6ce97, 15),
  //  hashGen1(31, 16, 0x604baa5d, 15, 0x43d6ce97, 15),
  //  hashGen1(0, 16, 0xa812d533, 15, 0xb278e4ad, 17),
  //  hashGen1(7, 16, 0xa812d533, 15, 0xb278e4ad, 17),
  //  hashGen1(31, 16, 0xa812d533, 15, 0xb278e4ad, 17),
  //  hashGen1(0, 16, 0x9c8f2d35, 15, 0x5d1346b5, 17),
  //  hashGen1(7, 16, 0x9c8f2d35, 15, 0x5d1346b5, 17),
  //  hashGen1(31, 16, 0x9c8f2d35, 15, 0x5d1346b5, 17)
  // )

  def hashGen1(offset: Int, n1: Int, n2: Int, n3: Int, n4: Int, n5: Int)(v: Int): Int =
    var x = v + offset
    x ^= x >>> n1;
    x *= n2;
    x ^= x >>> n3;
    x *= n4;
    x ^= x >>> n5;
    x

  def hashGen2(offset: Int, n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, n6: Int, n7: Int)(v: Int): Int =
    var x = v + offset
    x ^= x >>> n1;
    x *= n2;
    x ^= x >>> n3;
    x *= n4;
    x ^= x >>> n5;
    x *= n6;
    x ^= x >> n7;
    x;
