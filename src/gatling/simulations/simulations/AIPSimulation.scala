package simulations

import io.gatling.core.Predef._
import io.gatling.core.pause.PauseType
import io.gatling.core.scenario.Simulation
import scenarios.{AIPRequestRespondentEvidence, AIP_Appeal, CreateUser}
import scenarios.utils.Environment


class AIPSimulation extends Simulation {

  val BaseURL = Environment.baseURL
  val aipuserdetails = csv("AIPUserDetails.csv").circular
  val holetterdetails = csv("HOLetterDetails.csv").circular
  val personaldetails = csv("PersonalDetails.csv").circular
  
  /* TEST TYPE DEFINITION */
  /* pipeline = nightly pipeline against the AAT environment (see the Jenkins_nightly file) */
  /* perftest (default) = performance test against the perftest environment */
  val testType = scala.util.Properties.envOrElse("TEST_TYPE", "perftest")
  
  //set the environment based on the test type
  val environment = testType match {
    case "perftest" => "perftest"
    case "pipeline" => "perftest" //updated pipeline to run against perftest - change to aat to run against AAT
    case _ => "**INVALID**"
  }
  /* ******************************** */
  
  /* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
  val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradle gatlingRun -Ddebug=on (default: off)
  val env = System.getProperty("env", environment) //manually override the environment aat|perftest e.g. ./gradle gatlingRun -Denv=aat
  /* ******************************** */
  
  //If running in debug mode, disable pauses between steps
  val pauseOption: PauseType = debugMode match {
    case "off" => constantPauses
    case _ => disabledPauses
  }
  /* ******************************** */
  
  /* PIPELINE CONFIGURATION */
  val numberOfPipelineUsers: Double = 5
  /* ******************************** */
  
  
  val httpProtocol = Environment.HttpProtocol
    .baseUrl(BaseURL)
    .doNotTrackHeader("1")
    //.inferHtmlResources()
    .silentResources
  
  before {
    println(s"Test Type: ${testType}")
    println(s"Test Environment: ${env}")
    println(s"Debug Mode: ${debugMode}")
  }
  

  // below scenario is for user data creation for an AIP Citizen User
  val UserCreationScenario = scenario("CMC User Creation")
    .exec(
      CreateUser.CreateCitizen("citizen")
        .pause(20)
    )

  val AIPAppeal = scenario("AIP Appeal Journey")
    .exitBlockOnFail{
      exec(_.set("env", s"${env}"))
      .exec(AIP_Appeal.home)
      .exec(AIP_Appeal.eligibility)
      .exec(AIP_Appeal.LoginHomePage)
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
  

  val AIPRequestRespondent = scenario("IAC Request Respondent Evidence")
    .exitBlockOnFail{
      exec(_.set("env", s"${env}"))
      .exec(AIPRequestRespondentEvidence.IAChome)
      .exec(AIPRequestRespondentEvidence.IACLogin)
      .exec(AIPRequestRespondentEvidence.IACSearchCase)
      .exec(AIPRequestRespondentEvidence.IACSelectCase)
      .exec(AIPRequestRespondentEvidence.IACRequestRespondentEvidence)
      .exec(AIPRequestRespondentEvidence.IACSubmitRespondentEvidence)
      .exec(AIPRequestRespondentEvidence.IACSendRespondentEvidence)
    }


  //Scenario to create users - users will be output to AIPUserDetails.csv

   /* setUp(
  UserCreationScenario.inject(nothingFor(1),rampUsers(5) during (5))
).protocols(httpProtocol)
*/

  //Scenario which runs through the AIP Appeal Journey.  The Appeal reference number is output into AIPAppealRef.csv
  /*setUp(
    AIPAppeal.inject(nothingFor(1),rampUsers(69) during (2400))
  ).protocols(httpProtocol)
*/

  // CaseWorker progressing the case to request respondent evidence
  setUp(
    AIPRequestRespondent.inject(nothingFor(1),rampUsers(1) during (1))
  ).protocols(httpProtocol)

}