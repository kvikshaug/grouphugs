package no.kvikshaug.gh.util

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import org.junit.Assert._
import java.io.ByteArrayInputStream

class IOTest extends AssertionsForJUnit {

  val str = "æøå¡∑∂∂∑ç˜˜¨√ˆ¨√¡ø£¡¡™¡∑çµ¬˚¥˙∂øˆªˆ¬ÆØÅ"
  val strBytes = str.getBytes("UTF-8")

  @Test
  def testToByteArray() {
    val bytes = IO.toByteArray(new ByteArrayInputStream(strBytes))
    val newStr = new String(bytes, "UTF-8")
    assertArrayEquals("Bytes from toByteArray does not match actual", strBytes, bytes)
  }
}