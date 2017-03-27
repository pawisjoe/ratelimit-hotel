package models

import javax.inject.Inject

import play.api.{Configuration, Environment}
import scala.io.Source._

case class Hotel(hotelId: Int, city: String, room: String, price: Int)

class Hotels @Inject()(environment: Environment, configuration: Configuration) {
  val hotels: List[Hotel] = generateHotelFromCSV()
  val orderingByPrice: Ordering[Hotel] = Ordering.by(h => h.price)

  /**
    * Read CSV file and generate Hotel model
    * @return
    */
  def generateHotelFromCSV(): List[Hotel] = {
    val hotels = scala.collection.mutable.ListBuffer.empty[Hotel]
    val lines = fromFile(environment.getFile("conf/hoteldb.csv"))
    for(line <- lines.getLines.drop(1)) {
      val cols = line.split(",").map(_.trim)
      hotels += Hotel(cols(1).toInt, cols(0), cols(2), cols(3).toInt)
    }
    return hotels.toList
  }

  import OrderBy._
  def getAllHotel(orderBy: OrderBy): List[Hotel] = {
    if(orderBy == DESC) return hotels.sorted(orderingByPrice.reverse)
    hotels.sorted(orderingByPrice)
  }

  def searchHotelByCity(city: String, orderBy: OrderBy): List[Hotel] = {
    if(orderBy == DESC) return hotels.filter(_.city == city).sorted(orderingByPrice.reverse)
    hotels.filter(_.city == city).sorted(orderingByPrice)
  }

}

object OrderBy extends Enumeration {
  type OrderBy = Value
  val ASC, DESC = Value

  def fromValue(s: String): Option[Value] = values.find(_.toString == s)
}
