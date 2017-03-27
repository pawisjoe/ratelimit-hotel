package filters

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import javax.inject._

import org.joda.time.DateTime
import play.api.{Configuration, Environment}
import play.api.mvc._
import scala.collection._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RateLimitFilter @Inject()(
    implicit override val mat: Materializer,
    exec: ExecutionContext, environment: Environment, configuration: Configuration) extends Filter {

  private val maxRequest = getMaxRequestFromConfiguration()
  private val maxRequestTime = getMaxRequestTimeFromConfiguration()
  private val suspendTime = getSuspendTimeFromConfiguration()

  private var rateLimitMap = mutable.Map[String, scala.collection.mutable.Map[DateTime, AtomicInteger]]()
  private var suspendMap = mutable.Map[String, DateTime]()

  override def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val now = DateTime.now()
    val apiKey: Option[String] = requestHeader.getQueryString("apikey")
    //require apikey
    if(apiKey == None) return Future.successful(Results.BadRequest("Missing apikey parameter"))

    //first time request with this apikey
    if(!rateLimitMap.contains(apiKey.get) && !suspendMap.contains(apiKey.get)) {
      rateLimitMap += (apiKey.get -> mutable.Map(now -> new AtomicInteger(1)))
      nextFilter(requestHeader)
    }
    else if(rateLimitMap.contains(apiKey.get)) {
      checkApiLimit(apiKey.get, now, nextFilter, requestHeader)
    }
    else {
      checkSuspendApi(apiKey.get, now, nextFilter, requestHeader)
    }
  }

  /**
    * check status of apikey, in case this api was requested to service before
    * @param apiKey
    * @param now
    * @param nextFilter
    * @param request
    * @return
    */
  def checkApiLimit(apiKey: String, now: DateTime,
                    nextFilter: RequestHeader => Future[Result],
                    request: RequestHeader): Future[Result] = {
    if(now.getMillis - rateLimitMap(apiKey).keySet.head.getMillis() < maxRequestTime) {
      if(rateLimitMap(apiKey).values.head.get() < maxRequest) {
        rateLimitMap(apiKey) = mutable.Map[DateTime, AtomicInteger](rateLimitMap(apiKey).keySet.head -> new AtomicInteger(rateLimitMap(apiKey).values.head.incrementAndGet()))
        nextFilter(request)
      }
      else {
        rateLimitMap -= apiKey
        suspendMap += (apiKey -> now)
        Future.successful(Results.TooManyRequests("API key rate limit exceeded"))
      }
    }
    else {
      rateLimitMap -= apiKey
      nextFilter(request)
    }
  }

  /**
    * check suspended api key
    * @param apiKey
    * @param now
    * @param nextFilter
    * @param request
    * @return
    */
  def checkSuspendApi(apiKey: String, now: DateTime,
                      nextFilter: RequestHeader => Future[Result],
                      request: RequestHeader): Future[Result] = {
    if(now.getMillis - suspendMap(apiKey).getMillis < suspendTime) {
      println(now.getMillis - suspendMap(apiKey).getMillis)
      Future.successful(Results.TooManyRequests("API key has been suspended. Please wait and request again"))
    }
    else {
      suspendMap -= apiKey
      nextFilter(request)
    }
  }

  def getMaxRequestFromConfiguration(): Int = {
    try {
      configuration.getString("maxRequest").getOrElse("1").toInt
    } catch {
      case _: Throwable => return 1;
    }
  }

  def getMaxRequestTimeFromConfiguration(): Int = {
    try {
      configuration.getString("maxRequestTime").getOrElse("10000").toInt
    } catch {
      case _: Throwable => return 10000;
    }
  }

  def getSuspendTimeFromConfiguration(): Int = {
    try {
      configuration.getString("suspendTime").getOrElse("300000").toInt
    } catch {
      case _: Throwable => return 300000;
    }
  }
}
