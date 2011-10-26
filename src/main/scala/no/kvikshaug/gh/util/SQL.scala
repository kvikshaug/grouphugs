package no.kvikshaug.gh.util

import no.kvikshaug.gh.Config
import no.kvikshaug.gh.exceptions.{SQLUnavailableException, PreferenceNotSetException}

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.UnsupportedDatabaseException;

import java.io.File

object SQL {
  val dbType = "SQLite"
  val dbDriver = "org.sqlite.JDBC"
  val connectionUrl = "jdbc:sqlite"

  var isAvailable = false

  @throws(classOf[SQLUnavailableException])
  def initiate() {
    try {
      val db = new File(Config.dbFile)
      if(!db.exists()) {
        throw new SQLUnavailableException(
          "Unable to find the database file (expected in: " + db.getAbsolutePath() + ")")
      }
      if(!SQLSchema.compare(db.getName(), Config.dbSchema, connectionUrl + ":" + db.getName)) {
        throw new SQLUnavailableException("The database schema in " + db.getName() +
          " doesn't correspond to the one in " + Config.dbSchema + "! Please update your database.")
      }
      Worm.connect(dbType, dbDriver, connectionUrl + ":" + db.getName())
      isAvailable = true
    } catch {
      case e: UnsupportedDatabaseException =>
        throw new SQLUnavailableException("Worm threw an UnsupportedDatabaseException: " + e.getMessage());
      case e: ClassNotFoundException =>
        throw new SQLUnavailableException("Unable to load the SQL JDBC driver.");
      case e: PreferenceNotSetException =>
        throw new SQLUnavailableException("Database file not specified in config: " + e.getMessage());
    }
  }

  /** Prepends a backslash to any '-char that doesn't
      already have a backslash prepended */
  def sanitize(input: String) = {
    val sb = new StringBuilder
    var i = 0
    input foreach { c =>
      if(c == '\'' && (i == 0 || input(i-1) != '\'')) {
        sb.append('\'')
      }
      sb.append(c)
      i += 1
    }
    sb.toString
  }
}
