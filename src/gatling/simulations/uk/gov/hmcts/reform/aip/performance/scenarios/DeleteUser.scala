package uk.gov.hmcts.reform.aip.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment

object DeleteUser {

  val IdamAPIURL = Environment.idamAPIURL

  val DeleteCitizen =
    group("AIP_000_DeleteCitizen") {
      exec(http("DeleteCitizen")
        .delete(IdamAPIURL + "/testing-support/accounts/${emailAddress}")
        .check(status.is(204)))
    }
}
