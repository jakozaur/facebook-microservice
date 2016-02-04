import java.io.IOException

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{RawHeader, Accept}
import akka.http.scaladsl.model.{MediaRange, HttpHeader, HttpResponse, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source, Flow}
import com.typesafe.config.Config
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._


import scala.concurrent.{ExecutionContextExecutor, Future}

case class FacebookEvents(data: List[FacebookEvent], paging: FacebookPaging)
case class FacebookEvent(description: String, name: String, place: Option[FacebookPlace], start_time: String, id: String)
case class FacebookPlace(name: String, location: FacebookLocation)
case class FacebookLocation(city: String, country: String, latitude: Double, longitude: Double, street: Option[String], zip: String)
case class FacebookPaging(next: String)

case class FacebookProfile(id: String, name: String, gender: String, birthday: String, location: FacebookProfileLocation)
case class FacebookProfileLocation(id: String, name: String)

trait FacebookProtocols extends DefaultJsonProtocol {
  implicit val facebookPaging = jsonFormat1(FacebookPaging.apply)
  implicit val facebookLocation = jsonFormat6(FacebookLocation.apply)
  implicit val facebookPlace = jsonFormat2(FacebookPlace.apply)
  implicit val facebookEvent = jsonFormat5(FacebookEvent.apply)
  implicit val facebookEvents = jsonFormat2(FacebookEvents.apply)
  implicit val facebookProfileLocation = jsonFormat2(FacebookProfileLocation.apply)
  implicit val facebookProfile = jsonFormat5(FacebookProfile.apply)

}

/**
 * @author Jacek Migdal (jacek@sumologic.com)
 */
class FacebookApi(config: Config, logger: LoggingAdapter)
                 (implicit val executor: ExecutionContextExecutor,
                  implicit val system: ActorSystem,
                  implicit val materializer: Materializer) extends FacebookProtocols {
  private lazy val facebookConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnectionTls(config.getString("facebook.host"), config.getInt("facebook.port"))

  private def facebookRequest(request: HttpRequest): Future[HttpResponse] = {
    Source.single(request.addHeader(RawHeader("Accept", "application/json"))).via(facebookConnectionFlow).runWith(Sink.head)
  }

  def fetchProfile(fbToken: String): Future[Either[String, FacebookProfile]] = {
    facebookRequest(RequestBuilding.Get(
      s"/v2.5/me?access_token=$fbToken&fields=name%2Clocation%2Cgender%2Cbirthday&format=json&method=get")).flatMap { response =>
      response.status match {
        //case _ => Unmarshal(response.entity).to[String].map(Left(_))
        case OK => Unmarshal(response.entity).to[FacebookProfile].map(Right(_))
        case BadRequest => Unmarshal(response.entity).to[String].map(msg => Left(s"BadRequest: $msg"))
        case _ => Future.successful(Left("Unexpected error: " + Unmarshal(response.entity)))
      }
    }
  }

  def fetchEvents(fbToken: String, city: String): Future[Either[String, List[FacebookEvent]]] = {

    facebookRequest(RequestBuilding.Get(
        s"/v2.5/search?access_token=$fbToken&format=json&method=get&pretty=0&q=$city&type=event")).flatMap { response =>
      response.status match {
        //case _ => Unmarshal(response.entity).to[String].map(Left(_))
        case OK => Unmarshal(response.entity).to[FacebookEvents].map(_.data).map(Right(_))
        case BadRequest => Unmarshal(response.entity).to[String].map(msg => Left(s"BadRequest: $msg"))
        case _ => Future.successful(Left("Unexpected error: " + Unmarshal(response.entity)))
      }
    }
  }
}