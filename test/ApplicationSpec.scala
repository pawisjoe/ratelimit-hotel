import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "No api key" in  {
      val noApikeyRoute = route(app, FakeRequest(GET, "/test")).map(status(_)) mustBe Some(BAD_REQUEST)
    }

  }

  "HotelController" should {

    "render hotel page" in {
      val hotels = route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      status(hotels) mustBe OK
      contentType(hotels) mustBe Some("application/json")
    }

    "render not found hotel page" in {
      val hotels = route(app, FakeRequest(GET, "/hotel/Test?apikey=\"test\"")).get
      status(hotels) mustBe BAD_REQUEST
      contentType(hotels) mustBe Some("text/plain")
      contentAsString(hotels) mustBe "No hotels found"
    }

    "render api exceeded limit" in {
      route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      var hotels = route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      status(hotels) mustBe TOO_MANY_REQUESTS
      contentType(hotels) mustBe Some("text/plain")
      contentAsString(hotels) mustBe "API key rate limit exceeded"
    }

    "render api has been suspended" in {
      route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      var hotels = route(app, FakeRequest(GET, "/hotel/Bangkok?apikey=\"test\"")).get
      status(hotels) mustBe TOO_MANY_REQUESTS
      contentType(hotels) mustBe Some("text/plain")
      contentAsString(hotels) mustBe "API key has been suspended. Please wait and request again"
    }

  }

}
