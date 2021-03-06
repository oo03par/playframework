/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{Json, JsError}
import play.api.test._

object AnyContentBodyParserSpec extends PlaySpecification {

  "The anyContent body parser" should {

    def parse(method: String, contentType: Option[String], body: Array[Byte]) = {
      val request = FakeRequest(method, "/x").withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq:_*)
      await(Enumerator(body) |>>> BodyParsers.parse.anyContent(request))
    }

    "ignore empty bodies for GET requests" in new WithApplication() {
      parse("GET", None, Array[Byte]()) must beRight(AnyContentAsEmpty)
    }

    "ignore text bodies for GET requests" in new WithApplication() {
      parse("GET", Some("text/plain"), "bar".getBytes("utf-8")) must beRight(AnyContentAsEmpty)
    }

    "ignore empty bodies for HEAD requests" in new WithApplication() {
      parse("HEAD", None, Array[Byte]()) must beRight(AnyContentAsEmpty)
    }

    "ignore text bodies for HEAD requests" in new WithApplication() {
      parse("HEAD", None, "bar".getBytes("utf-8")) must beRight(AnyContentAsEmpty)
    }

    "parse text bodies for POST requests" in new WithApplication() {
      parse("POST", Some("text/plain"), "bar".getBytes("utf-8")) must beRight(AnyContentAsText("bar"))
    }

    "parse JSON bodies for PUT requests" in new WithApplication() {
      parse("PUT", Some("application/json"), """{"foo":"bar"}""".getBytes("utf-8")) must beRight.like {
        case AnyContentAsJson(json) => (json \ "foo").as[String] must_== "bar"
      }
    }

    "parse unknown bodies as raw for PUT requests" in new WithApplication() {
      val inBytes = Array[Byte](0)
      parse("PUT", None, inBytes) must beRight.like {
        case AnyContentAsRaw(rawBuffer) => rawBuffer.asBytes() must beSome.like {
          case outBytes => outBytes.to[Vector] must_== inBytes.to[Vector]
        }
      }
    }

  }
}
