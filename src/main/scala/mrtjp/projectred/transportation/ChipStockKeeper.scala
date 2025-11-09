package mrtjp.projectred.transportation

import mrtjp.core.inventory.InvWrapper
import mrtjp.core.item.{ItemEquality, ItemKey, ItemKeyStack}
import mrtjp.projectred.transportation.RequestMode._

import scala.collection.mutable.{ListBuffer, Set => MSet}

class ChipStockKeeper
    extends RoutingChip
    with TChipStock
    with TChipMatchMatrix {
  private var remainingDelay = operationDelay
  private def operationDelay = 100

  private var operationsWithoutRequest = 0
  private def throttleDelay = {
    var throttle = 10 * operationsWithoutRequest
    throttle = Math.min(throttle, 20 * 60)
    throttle
  }

  private val maxRequestSize = 128

  override def getMatchInventory = stock

  override def update() {
    super.update()

    remainingDelay -= 1
    if (remainingDelay > 0) return

    val real = invProvider.getInventory
    val side = invProvider.getInterfacedSide
    if (real == null || side < 0) return

    val inv = InvWrapper.wrap(real).setSlotsFromSide(side)
    val filt = InvWrapper.wrap(stock).setSlotsAll()

    val checked = MSet[ItemKey]()
    var requestAttempted = false
    var requestedSomething = false

    for (i <- 0 until stock.getSizeInventory) {
      val (wasAttempted, wasRequested) = processStockSlot(i, inv, filt, checked)
      if (wasAttempted) {
        requestAttempted = true
        if (wasRequested) requestedSomething = true
      }
    }

    if (requestAttempted)
      RouteFX2.spawnType1(
        RouteFX2.color_request,
        routeLayer.getWorldRouter.getContainer
      )
    if (requestAttempted && requestedSomething) operationsWithoutRequest = 0
    else operationsWithoutRequest += 1

    remainingDelay = operationDelay + throttleDelay
  }

  /** Process a single stock slot and attempt to request items if needed.
    *
    * @param slotIndex
    *   The slot index to process
    * @param inv
    *   The wrapped inventory to check and request from
    * @param filt
    *   The wrapped filter inventory containing target stock levels
    * @param checked
    *   Set of already checked items (to avoid duplicates)
    * @return
    *   A tuple (wasAttempted, wasRequested) where: wasAttempted is true if a
    *   request was attempted, wasRequested is true if the network successfully
    *   accepted the request (req.requested > 0)
    */
  private def processStockSlot(
      slotIndex: Int,
      inv: InvWrapper,
      filt: InvWrapper,
      checked: MSet[ItemKey]
  ): (Boolean, Boolean) = {
    val keyStack = ItemKeyStack.get(stock.getStackInSlot(slotIndex))

    // Early exit: skip if slot is empty or item already checked
    if (keyStack == null || checked.contains(keyStack.key)) {
      return (false, false)
    }

    checked += keyStack.key

    val eq = createEqualityFor(slotIndex)
    inv.eq = eq

    val stockToKeep = requestMode match {
      case INFINITE => Int.MaxValue
      case _        => filt.getItemCount(keyStack.key)
    }

    val spaceInInventory =
      routeLayer.getRequester.getActiveFreeSpace(keyStack.key)

    // Early exit: if no space in inventory, no need to check further
    if (spaceInInventory <= 0) {
      return (false, false)
    }

    val storedCount = inv.getItemCount(keyStack.key)

    // Early exit: if WHEN_EMPTY mode and we have items, skip
    if (requestMode == WHEN_EMPTY && storedCount > 0) {
      return (false, false)
    }

    val enrouteCount = getEnroute(eq, keyStack.key)

    // Early exit: if quota already satisfied, skip expensive getPendingOrders call
    if (storedCount + enrouteCount >= stockToKeep) {
      return (false, false)
    }

    val pendingCount = getPendingOrders(eq, keyStack.key)
    val inInventory = storedCount + enrouteCount + pendingCount

    var toRequest = math.min(stockToKeep - inInventory, spaceInInventory)
    toRequest = math.min(toRequest, maxRequestSize)

    // Early exit: if nothing to request after all calculations
    if (toRequest <= 0) {
      return (false, false)
    }

    // Make the request
    val req = new RequestConsole(RequestFlags.full)
      .setDestination(routeLayer.getRequester)
      .setEquality(eq)
    val request = ItemKeyStack.get(keyStack.key, toRequest)
    req.makeRequest(request)

    (true, req.requested > 0)
  }

  def getEnroute(eq: ItemEquality, item: ItemKey) =
    routeLayer.getWorldRouter.getContainer.transitQueue
      .count(eq.matches(item, _))

  def getPendingOrders(eq: ItemEquality, item: ItemKey): Int = {
    val requester = routeLayer.getRequester
    val routes = routeLayer.getRouter
      .getFilteredRoutesByCost(p =>
        p.flagRouteFrom && (p.allowBroadcast || p.allowCrafting)
      )

    routes.foldLeft(0) { (total, path) =>
      path.end.getParent match {
        case broadcaster: IWorldBroadcaster =>
          total + broadcaster.getPendingDeliveries(item, eq, requester)
        case _ =>
          total
      }
    }
  }

  override def weakTileChanges = true

  override def onNeighborTileChanged(side: Int, weak: Boolean) {
    operationsWithoutRequest = 0
    remainingDelay = Math.min(remainingDelay, operationDelay)
  }

  override def infoCollection(list: ListBuffer[String]) {
    super.infoCollection(list)
    addStockInfo(list)
  }

  def getChipType = RoutingChipDefs.ITEMSTOCKKEEPER
}
