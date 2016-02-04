import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ExecutionContextExecutor}
import spray.json.DefaultJsonProtocol

case class ReturnFacebookEvent(data: List[FacebookEvent])


trait Protocols extends DefaultJsonProtocol with FacebookProtocols {
  implicit val returnFacebookEvent = jsonFormat1(ReturnFacebookEvent.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  lazy val facebookApi = new FacebookApi(config, logger)

  val routes = {
    logRequestResult("akka-http-microservice") {
      path("profile") {
        (get & parameter("fb_token")) { fbToken =>
          complete {
            facebookApi.fetchProfile(fbToken).map[ToResponseMarshallable] {
              case Right(profile) => profile
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      path("events") {
        (get & parameters("fb_token", "fb_profile", "meetup_profile", "city", "date_from", "date_to")) {
            (fbToken, fbProfile, meetupProfile, city, dateFrom, dateTo) =>
          complete {
            facebookApi.fetchEvents(fbToken, city).map[ToResponseMarshallable] {
              case Right(info) => ReturnFacebookEvent(info)
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      (get & path(Segments(0, Int.MaxValue))) { _ =>
        complete {
          BadRequest -> "Bad request, check documentation: https://github.com/jakozaur/facebook-microservice"
        }
      }
    }
  }
}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
