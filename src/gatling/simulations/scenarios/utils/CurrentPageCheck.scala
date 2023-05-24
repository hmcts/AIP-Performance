package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.http.Predef._
import io.gatling.http.check.url.CurrentLocationCheckType

object CurrentPageCheck {
  def save: CheckBuilder[CurrentLocationCheckType,String] = currentLocation.saveAs("currentPage")
  def currentPageTemplate: String = "${currentPage}"
}
