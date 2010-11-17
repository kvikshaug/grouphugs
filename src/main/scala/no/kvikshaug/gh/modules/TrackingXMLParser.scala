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
  val dbName = "tracking"

  @throws(classOf[IOException])
  @throws(classOf[SQLException])
  def track(item: Package) = {
    // load the xml
    val root = XML.load(Web.prepareEncodedBufferedReader(
      new URL("http://sporing.posten.no/sporing.xml?q=" + item.id)))

    val e = ((root \\ "PackageSet" \ "Package")(0) \ "EventSet" \ "Event")(0)
    val status = (e \ "Description").text.replaceAll("<.*?>", "") + " " +
                 (e \ "PostalCode").text.replaceAll("<.*?>", "")  + " " +
                 (e \ "City").text.replaceAll("<.*?>", "") + " " +
                 (e \ "OccuredAtDisplayTime").text.replaceAll("<.*?>", "") + " " +
                 (e \ "OccuredAtDisplayDate").text.replaceAll("<.*?>", "")
    val statusCode = (e \ "Status").text.replaceAll("<.*?>", "")
/*      case "NO_PACKAGES"       => NoPackages
      case "IN_TRANSIT"        => InTransit
      case "READY_FOR_PICKUP"  => ReadyForPickup
      case "NOTIFICATION_SENT" => NotificationSent
      case "RETURNED"          => Returned
      case "DELIVERED"         => Delivered
      case x                   => Unknown(x) */
    val packageCount = (root \\ "PackageSet" \ "Package").size

    var changed = false
    if(status != item.status || statusCode != item.statusCode) {
      changed = true
    }
    item.status = status
    item.statusCode = statusCode
    sqlHandler.update("update " + dbName + " set status='" + status + "', statusCode='" + statusCode + "' where trackingId='" + item.id + "'")

    (changed, packageCount)
  }
}
