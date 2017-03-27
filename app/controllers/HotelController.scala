package controllers

import javax.inject._

import models.{Hotel, Hotels, OrderBy}
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

@Singleton
class HotelController @Inject()(hotels: Hotels) extends Controller {

  /**
    * Get all Hotels
    * @param orderby
    * @return hotel list
    */
  def listHotel(orderby: String) = Action {
    val json = Json.toJson(hotels.getAllHotel(OrderBy.fromValue(orderby).getOrElse(OrderBy.ASC)))
    Ok(json)
  }

  /**
    * Search all that in specific city
    * @param city
    * @param orderby
    * @return filtered hotel list
    */
  def hotelByCity(city: String, orderby: String) = Action {
    val list = hotels.searchHotelByCity(city, OrderBy.fromValue(orderby).getOrElse(OrderBy.ASC))
    if(list.size > 0) {
      val json = Json.toJson(list)
      Ok(json)
    }
    else {
      BadRequest("No hotels found")
    }
  }

  /**
    * parse object to JSON
    */
  implicit val hotelWrites: Writes[Hotel] = (
    (JsPath \ "hotelId").write[Int] and
    (JsPath \ "city").write[String] and
    (JsPath \ "room").write[String] and
    (JsPath \ "price").write[Int]
   )(unlift(Hotel.unapply))
}
