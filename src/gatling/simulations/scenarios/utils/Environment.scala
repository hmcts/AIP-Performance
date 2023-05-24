package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Environment {
  
  val httpConfig = scala.util.Properties.envOrElse("httpConfig", "http")
  val baseURL = scala.util.Properties.envOrElse("baseURL", "https://manage-case.#{env}.platform.hmcts.net")
  val idamURL = "https://idam-web-public.#{env}.platform.hmcts.net"
  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"
  val idamCookieName="SESSION_ID"
  val HttpProtocol = http
  
  val minThinkTime = 20//80//100//140//5
  val maxThinkTime = 40//80//100//150//10

}