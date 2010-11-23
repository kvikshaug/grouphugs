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
  // returns a tuple of ("Has the package changed?", "The number of packages in this item")
  def track(item: Package): Tuple2[Boolean, Int] = {
    var newStatus = ""
    var newStatusCode = ""
    var packageCount = 0

    try {
      // load the xml
      val root = XML.load(Web.prepareEncodedBufferedReader(
        new URL("http://sporing.posten.no/sporing.xml?q=" + item.id)))

      val e = ((root \\ "PackageSet" \ "Package")(0) \ "EventSet" \ "Event")(0)
      newStatus = (e \ "Description").text.replaceAll("<.*?>", "") + " " +
                  (e \ "PostalCode").text.replaceAll("<.*?>", "")  + " " +
                  (e \ "City").text.replaceAll("<.*?>", "") + " " +
                  (e \ "OccuredAtDisplayTime").text.replaceAll("<.*?>", "") + " " +
                  (e \ "OccuredAtDisplayDate").text.replaceAll("<.*?>", "")
      newStatusCode = (e \ "Status").text.replaceAll("<.*?>", "")
      packageCount = (root \\ "PackageSet" \ "Package").size
    } catch {
      // thrown by the API when the item ID isn't found at posten (404)
      case e: FileNotFoundException =>
        newStatus = "Not found (but it might appear soon if it was recently added)"
        newStatusCode = "NO_PACKAGES"
    }

    var changed = false
    if(newStatus != item.status || newStatusCode != item.statusCode) {
      changed = true
    }
    item.status = newStatus
    item.statusCode = newStatusCode
    sqlHandler.update("update " + dbName + " set status='" + newStatus + "', statusCode='" + newStatusCode + "' where trackingId='" + item.id + "'")

    (changed, packageCount)
  }
}
