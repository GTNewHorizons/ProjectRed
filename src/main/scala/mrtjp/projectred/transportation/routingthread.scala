package mrtjp.projectred.transportation

object TableUpdateThread {
  private val updateCalls = new CoalescingUpdateQueue[Router](
    _.getLinkStateID,
    (router, version) => new RouteLayerUpdater(router, version),
    (left, leftVersion, right, rightVersion) => {
      var c = 0
      if (rightVersion <= 0)
        c = java.lang.Integer.compare(leftVersion, rightVersion)
      if (c == 0)
        c = java.lang.Integer.compare(left.getIPAddress, right.getIPAddress)
      if (c == 0) c = java.lang.Integer.compare(rightVersion, leftVersion)
      c
    }
  )
  private var average = 0L

  val avgSync = new AnyRef

  def add(r: Router) {
    updateCalls.add(r)
  }

  def remove(run: Runnable) = updateCalls.remove(run)

  def size = updateCalls.size

  def getAverage = avgSync synchronized average
}

class TableUpdateThread(i: Int) extends Thread("PR RoutingThread #" + i) {
  setDaemon(true)
  setPriority(Thread.NORM_PRIORITY)
  start()

  override def run() {
    import TableUpdateThread._

    var job: Runnable = null
    try {
      while ({ job = updateCalls.take; job } != null) {
        val starttime = System.nanoTime
        job.run()
        val took = System.nanoTime - starttime

        TableUpdateThread.avgSync synchronized {
          if (TableUpdateThread.average == 0) TableUpdateThread.average = took
          else
            TableUpdateThread.average =
              (TableUpdateThread.average * 999L + took) / 1000L
        }
      }
    } catch { case e: InterruptedException => }
  }
}

private[this] class RouteLayerUpdater(val router: Router, newVersion: Int)
    extends Runnable {
  private var complete = false

  def run() {
    if (complete) return
    try {
      if (router.getParent == null) return
      {
        var i = 0
        while (i < 10 && !router.isLoaded) {
          Thread.sleep(10)
          i += 1
        }
      }
      if (!router.isLoaded) return
      router.refreshRoutingTable(newVersion)
    } catch { case e: Exception => e.printStackTrace() }
    complete = true
  }

}
