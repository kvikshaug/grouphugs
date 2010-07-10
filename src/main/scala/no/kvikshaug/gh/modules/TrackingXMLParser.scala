package no.kvikshaug.gh.modules

import scala.xml._
import java.net.URL
import no.kvikshaug.gh.util.{SQLHandler, Web}
import java.sql.SQLException
import java.io.{FileNotFoundException, IOException}

/**
 * Utility class used by the Tracking module.
 * It fetches and parses the XML provided by posten's API
 */
object TrackingXMLParser {

  val sqlHandler = SQLHandler.getSQLHandler

  @throws(classOf[IOException])
  @throws(classOf[SQLException])
  def track(item: TrackingItem): Int = {
    // load the xml
    val root = XML.load(Web.prepareEncodedBufferedReader(
      new URL("http://sporing.posten.no/sporing.xml?q=" + item.trackingId)))

    // true if there is any difference in the package list or their details
    var changes = false

    // iterate all packages in the trackingitem set
    for(packageXml <- (root \\ "PackageSet" \ "Package")) {
      // extract the data we want from the latest event
      val packageId = (packageXml \ "@packageId").text
      val status = ((packageXml \\ "Event")(0) \ "Status").text
      val description = ((packageXml \\ "Event")(0) \ "Description").text + " " +
              ((packageXml \\ "Event")(0) \ "PostalCode").text + " " +
              ((packageXml \\ "Event")(0) \ "City").text
      val dateTime = ((packageXml \\ "Event")(0) \ "OccuredAtDisplayTime").text + " " +
                     ((packageXml \\ "Event")(0) \ "OccuredAtDisplayDate").text

      // true if this package already exists in the trackingitem's package list
      var found = false

      // check if this package is already there
      val it = item.packages.iterator
      while(it.hasNext) {
        val p = it.next
        if(p.packageId == packageId) {
          found = true
          // package is already tracked, see if its status has changed
          if(p.status != status || p.description != description) {
            changes = true
            p.status = status
            p.description = description
            sqlHandler.update("update " + Tracking.DB_PACKAGES_NAME + " set status='" + status + "'," +
                    "description='" + description + "' where id='" + packageId + "'")
          }
        }
      }
      if(!found) {
        // this package doesn't exist in the trackingitem's package list; add it
        changes = true
        val dbId = sqlHandler.insert("insert into " + Tracking.DB_PACKAGES_NAME + " " +
                "(packageId, status, description, dateTime) VALUES ('" +
                packageId + "', '" + status + "', '" + description + "', '" + dateTime + "');")
        sqlHandler.insert("insert into " + Tracking.DB_TRACKINGPACKAGES_NAME + " (trackingId, packageId) VALUES " +
                "('" + item.dbId + "', '" + dbId + "');")
        item.packages.add(TrackingItemPackage(dbId, packageId, status, description, dateTime))
      }
    }
    if(changes) {
      Tracking.CHANGED
    } else {
      Tracking.NOT_CHANGED
    }
  }

  def historyFor(item: TrackingItem, packageId: String): java.util.List[TrackingItemEvent] = {
    try {
      val events = new java.util.ArrayList[TrackingItemEvent]
      // load the xml
      val root = XML.load(Web.prepareEncodedBufferedReader(
        new URL("http://sporing.posten.no/sporing.xml?q=" + item.trackingId)))

      for(somePackage <- (root \\ "PackageSet" \ "Package")) {
        if((somePackage \ "@packageId").text.equals(packageId)) {
          for(event <- (somePackage \ "EventSet" \ "Event")) {
            events.add(TrackingItemEvent(
              (event \ "Description").text,
              (event \ "Status").text,
              (event \ "RecipientSignature" \ "Name").text,
              (event \ "UnitId").text,
              (event \ "PostalCode").text,
              (event \ "City").text,
              (event \ "OccuredAtIsoDateTime").text,
              (event \ "ConsignmentEvent").text
              ))
          }
        }
      }
      events
    } catch {
      case ex: FileNotFoundException =>
        null
    }
  }

  def infoFor(item: TrackingItem): TrackingItemInfo = {
    try {
      val packageInfo = new java.util.ArrayList[TrackingItemPackageInfo]
      // load the xml
      val root = XML.load(Web.prepareEncodedBufferedReader(
        new URL("http://sporing.posten.no/sporing.xml?q=" + item.trackingId)))

      for(somePackage <- (root \\ "PackageSet" \ "Package")) {
        packageInfo.add(TrackingItemPackageInfo(
          (somePackage \ "ProductName").text,
          (somePackage \ "ProductCode").text,
          (somePackage \ "Brand").text,
          (somePackage \ "Weight").text + (somePackage \ "Weight" \ "@unitCode").text,
          (somePackage \ "Length").text/* + (somePackage \ "Length" \ "@unitCode").text*/,
          (somePackage \ "Width").text/* + (somePackage \ "Width" \ "@unitCode").text*/,
          (somePackage \ "Height").text/* + (somePackage \ "Height" \ "@unitCode").text*/,
          (somePackage \ "Volume").text + (somePackage \ "Volume" \ "@unitCode").text
          ))
      }

      TrackingItemInfo(
        (root \\ "Consignment" \ "@consignmentId").text,
        (root \\ "TotalWeight").text + (root \\ "TotalWeight" \ "@unitCode").text,
        (root \\ "TotalVolume").text + (root \\ "TotalVolume" \ "@unitCode").text,
        packageInfo)
    } catch {
      case ex: FileNotFoundException =>
        null
    }
  }
}