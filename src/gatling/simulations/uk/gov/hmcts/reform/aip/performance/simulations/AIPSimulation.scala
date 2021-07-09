package uk.gov.hmcts.reform.aip.performance.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import uk.gov.hmcts.reform.aip.performance.scenarios._
import uk.gov.hmcts.reform.aip.performance.scenarios.utils.Environment

class AIPSimulation extends Simulation {

  val BaseURL = Environment.baseURL

  val httpProtocol = Environment.HttpProtocol
    .baseUrl(BaseURL)
    //.inferHtmlResources()
    //.silentResources

  val holetterdetails = csv("HOLetterDetails.csv").circular
  val personaldetails = csv("PersonalDetails.csv").circular

  val AIPAppeal = scenario("AIP Appeal Journey")
    .exitBlockOnFail{
      feed(holetterdetails)
      .feed(personaldetails)
      .exec(CreateUser.CreateCitizen)
      .exec(AIP_Appeal.home)
      .exec(AIP_Appeal.eligibility)
      .exec(AIP_Appeal.Login)
      .exec(AIP_Appeal.AboutAppeal)
      .exec(AIP_Appeal.HomeOffice)
      .exec(AIP_Appeal.PersonalDetails)
      .exec(AIP_Appeal.ContactDetails)
      .exec(AIP_Appeal.TypeofAppeal)
      .exec(AIP_Appeal.CheckAnswers)
      .exec(AIP_Appeal.AppealOverview)
      .exec(AIP_Appeal.AIPLogout)
    }
    .exec(DeleteUser.DeleteCitizen)

  //Scenario which runs through the AIP Appeal Journey.  The Appeal reference number is output into AIPAppealRef.csv
  setUp(
    AIPAppeal.inject(rampUsers(2) during 60)
  ).protocols(httpProtocol)
}