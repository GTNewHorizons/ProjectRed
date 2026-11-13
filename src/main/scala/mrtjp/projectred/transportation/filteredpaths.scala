package mrtjp.projectred.transportation

import mrtjp.core.item.ItemKey

class StartEndPath(
    val start: Router,
    val end: Router,
    val hopDir: Int,
    val distance: Int,
    filters: Set[PathFilter] = Set.empty,
    val netFlags: Int = 0x7
) extends Path(filters)
    with Ordered[StartEndPath] {
  def this(
      start: Router,
      end: Router,
      dirToFirstHop: Int,
      distance: Int,
      filter: PathFilter
  ) =
    this(start, end, dirToFirstHop, distance, Set(filter))

  val allowRouting = (netFlags & 0x1) != 0
  val allowBroadcast = (netFlags & 0x2) != 0
  val allowCrafting = (netFlags & 0x4) != 0

  override def equals(other: Any) = other match {
    case that: StartEndPath =>
      hopDir == that.hopDir &&
      distance == that.distance &&
      netFlags == that.netFlags &&
      super.equals(that)
    case _ => false
  }

  def -->(to: StartEndPath) = new StartEndPath(
    start,
    to.end,
    hopDir,
    distance + to.distance,
    filters ++ to.filters,
    netFlags & to.netFlags
  )

  override def compare(that: StartEndPath) = {
    var c = distance - that.distance
    if (c == 0) c = end.getIPAddress - that.end.getIPAddress
    c
  }

  override def toString =
    s"[${start.getIPAddress} -> ($distance) -> ${end.getIPAddress}] f:$pathFlags"
}

class Path(val filters: Set[PathFilter]) {
  override def equals(other: Any) = other match {
    case that: Path =>
      pathFlags == that.pathFlags &&
      filters == that.filters
    case _ => false
  }

  val emptyFilter = filters.forall(_ == PathFilter.default)

  def allowItem(item: ItemKey): Boolean = filters.forall(_.allowItem(item))
  def allowColor(c: Int) = filters.forall(_.allowColor(c))

  val pathFlags = filters.foldLeft(0x3) { (flags, filter) =>
    filter.freeze()
    flags & filter.pathFlags
  }
  val flagRouteTo = (pathFlags & 0x1) != 0
  val flagRouteFrom = (pathFlags & 0x2) != 0
}

object PathFilter {
  val default = new PathFilter
}

class PathFilter {
  private var frozen = false
  private var frozenHash = 0
  private var flagsValue = 0x3
  private var filterExcludeValue = true
  private var itemFilterValue: Set[ItemKey] = Set.empty
  private var colorExcludeValue = true
  private var colorsValue = 0

  // Keep the existing builder getter/setter ABI. A path publishes an immutable
  // filter: changing a key after insertion would corrupt its hash set.
  private def requireMutable() {
    if (frozen)
      throw new IllegalStateException("Cannot modify a published path filter")
  }
  def pathFlags: Int = flagsValue
  def pathFlags_=(value: Int) { requireMutable(); flagsValue = value }
  def filterExclude: Boolean = filterExcludeValue
  def filterExclude_=(value: Boolean) {
    requireMutable(); filterExcludeValue = value
  }
  def itemFilter: Set[ItemKey] = itemFilterValue
  def itemFilter_=(value: Set[ItemKey]) {
    requireMutable(); itemFilterValue = value
  }
  def colorExclude: Boolean = colorExcludeValue
  def colorExclude_=(value: Boolean) {
    requireMutable(); colorExcludeValue = value
  }
  def colors: Int = colorsValue
  def colors_=(value: Int) { requireMutable(); colorsValue = value }

  def allowItem(item: ItemKey) = itemFilter.contains(item) != filterExclude
  def filterContainsColor(c: Int) = (colors & 1 << c) != 0
  def allowColor(c: Int) = if (0 until 16 contains c)
    filterContainsColor(c) != colorExclude
  else true

  private def valueHash: Int = {
    var h = 17
    h = h * 31 + pathFlags
    h = h * 31 + (if (filterExclude) 1231 else 1237)
    h = h * 31 + itemFilter.hashCode
    h = h * 31 + (if (colorExclude) 1231 else 1237)
    h * 31 + colors
  }

  private[transportation] def freeze() {
    if (!frozen) { frozenHash = valueHash; frozen = true }
  }

  override def hashCode(): Int = if (frozen) frozenHash else valueHash

  override def equals(other: Any) = other match {
    case that: PathFilter =>
      pathFlags == that.pathFlags &&
      filterExclude == that.filterExclude &&
      itemFilter == that.itemFilter &&
      colorExclude == that.colorExclude &&
      colors == that.colors
    case _ => false
  }
}
