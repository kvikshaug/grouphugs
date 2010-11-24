package no.kvikshaug.gh.modules

case class Package(id: String, var status: String, var statusCode: String, owner: String, channel: String) {
  override def equals(other: Any) = other match {
    case that: Package => id == that.id
    case thatId: String => id == thatId
    case _ => false
  }
}

