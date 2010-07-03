package no.kvikshaug.gh.modules

// TODO: handle dates and time with Joda, and use this wrapper: http://github.com/jorgeortiz85/scala-time
case class TrackingItem(dbId: Int, trackingId: String, owner: String, packages: java.util.List[TrackingItemPackage]) {
  def totalStatus(): String = {
    if(packages.size == 1) {
      "Status: " + packages.get(0).description + ", " + packages.get(0).dateTime
    } else {
      var i = 0
      val it: java.util.Iterator[TrackingItemPackage] = packages.iterator
      val sb = new StringBuilder
      while(it.hasNext) {
        val p = it.next
        i += 1
        sb.append("Package " + i + ", " + p.packageId+ ": " + p.description + ", " + p.dateTime)
        if(it.hasNext) {
          sb.append("\n")
        }
      }
      sb.toString
    }
  }

  def oneLineStatus(): String = {
    if(packages.size == 0) {
      "No packages found for this item"
    } else if(packages.size == 1) {
      packages.get(0).description + ", " + packages.get(0).dateTime
    } else {
      "(" + packages.size + " packages): " + packages.get(0).description + ", " + packages.get(0).dateTime
    }
  }
}

case class TrackingItemPackage(dbId: Int, packageId: String, var status: String,
                               var description: String, var dateTime: String)

case class TrackingItemEvent(desc: String, status: String, signature: String, unitId: String, postalCode: String,
                             city: String, isoDateTime: String, consignmentEvent: String)

case class TrackingItemInfo(consignmentId: String, totalWeight: String, totalVolume: String,
                            packageInfo: java.util.List[TrackingItemPackageInfo])

case class TrackingItemPackageInfo(productName: String, productCode: String, brand: String, weight: String,
                                   length: String, width: String, height: String, volume: String)