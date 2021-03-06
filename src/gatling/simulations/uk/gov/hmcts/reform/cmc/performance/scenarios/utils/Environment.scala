package uk.gov.hmcts.reform.cmc.performance.scenarios.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Environment {
  
  val httpConfig = scala.util.Properties.envOrElse("httpConfig", "http")
  val baseURL = scala.util.Properties.envOrElse("baseURL", "https://immigration-appeal.perftest.platform.hmcts.net")
  val idamURL = "https://idam-web-public.perftest.platform.hmcts.net"
  val idamAPIURL = "https://idam-api.perftest.platform.hmcts.net"
  val idamCookieName="SESSION_ID"
  val HttpProtocol = http
  
  val minThinkTime = 20//80//100//140//5
  val maxThinkTime = 40//80//100//150//10

}