package coup.server

class CountingMap<I, K, V>(
  private val key: (I) -> K,
  private val new: (I) -> V,
  private val _values: Map<K, Pair<V, Int>> = mapOf()
) {
  private fun update(values: () -> Map<K, Pair<V, Int>>) = CountingMap(key, new, values())
  private fun update(k: K, f: (Pair<V, Int>?) -> Pair<V, Int>?) = update {
    val res = f(_values[k])
    if (res == null) {
      _values - k
    } else {
      _values + (k to res)
    }
  }

  private val Pair<V, Int>.inc get() = first to (second + 1)
  private val Pair<V, Int>.dec get() = (first to (second - 1)).takeIf { (_, c) -> c > 0 }

  operator fun plus(x: I) = update(key(x)) { it?.inc ?: (new(x) to 1) }

  operator fun minus(x: I) = update(key(x)) { it?.dec }

  operator fun get(x: I) = _values[key(x)]?.first
  fun getValue(x: I) = _values.getValue(key(x)).first

  val values get() = _values.values.map { it.first }

  fun isEmpty() = _values.isEmpty()
}