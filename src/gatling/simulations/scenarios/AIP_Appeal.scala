
package scenarios

import java.io.{BufferedWriter, FileWriter}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment, Headers}

import scala.concurrent.duration._

/*AIP Appeal is a new Journey which allows an appellant to go through several eligibility questions and if successful
they can login to the new AIP portal and start an appeal. This script carries out this business journey all the way
until the appeal is submitted and then captures and stores the appeal reference which we can use in business journeys
which are still to be developed*/

object AIP_Appeal {

val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime
val aipuser = csv("AIPUser.csv").circular

  //AIP Start Appeal HomePage
  val Home = group ("AIP_010_Homepage") {
    //exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
    exec(http("AIP_010_Homepage")
      .get("/start-appeal")
      .check(CurrentPageCheck.save)
      //.check(CsrfCheck.save)
      .check(regex("Appeal an immigration or asylum decision"))
      .headers(Headers.commonHeader))
    
    .exitHereIfFailed
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Sign in to continue with your appeal
  val LoginHomePage = group("AIP_020_CreateAcct_GET") {
    exec(http("AIP_020_CreateAcct_GET")
      .get("/login")
      .headers(Headers.commonHeader)
      .check(regex("Sign in or create an account"))
      .check(regex("client_id=iac&state=([0-9a-z-]+?)&nonce").saveAs("state"))
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Login into Application with an IAC Citizen account
  val Login = group("AIP_030_Login_POST") {
    //feed(aipuser)
    exec(http("AIP_030_Login_POST")
      .post(IdAMURL + "/login?redirect_uri=https%3a%2f%2fimmigration-appeal.#{env}.platform.hmcts.net%2fredirectUrl&client_id=iac&state=#{state}&response_type=code")
      .headers(Headers.commonHeader)
      .formParam("username", "#{emailAddress}")
      .formParam("password", "#{password}")
      .formParam("selfRegistrationEnabled", "true")
      .formParam("_csrf", "#{csrf}")
      .check(regex("Your appeal details")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //User gets the About Appeal Page after they have logged in
  val AboutAppeal = group("AIP_040_AboutAppeal_GET") {
    exec(http("AIP_040_AboutAppeal_GET")
      .get("/about-appeal")
      .headers(Headers.commonHeader)
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Go to Type of Appeal Page
  val TypeofAppeal = group("AIP_050_InTheUK_GET") {
    exec(http("AIP_050_InTheUK_GET")
      .get("/in-the-uk")
      .headers(Headers.commonHeader)
      .check(regex("Are you currently living in the United Kingdom?"))
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .group("AIP_060_InTheUK_POST") {
    exec(http("AIP_060_InTheUK_POST")
      .post("/in-the-uk")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("questionId", "")
      .formParam("answer", "Yes")
      .formParam("continue", "")
      .check(regex("What is your appeal type")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Type of appeal - Protection
  .group("AIP_070_AppealType_POST") {
    exec(http("AIP_070_AppealType_POST")
      .post("/appeal-type")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("appealType", "protection")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //User clicks on Home Office & gets Home Office Reference Number Page
  val HomeOffice = group("AIP_080_HomeOffice_GET") {
    exec(http("AIP_080_HomeOffice_GET")
      .get("/home-office-reference-number")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("What is your Home Office reference number")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Home Office Letter Reference - this can be any 10 digit number
  .group("AIP_090_HomeOffice_POST") {
    exec(http("AIP_090_HomeOffice_POST")
      .post("/home-office-reference-number")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("homeOfficeRefNumber", "123456789")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What date was your decision letter sent?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Date Home Office Letter Sent this date has to be within the last 14 days
  .group("AIP_100_HomeOffice_DateLetterSent") {
    exec(http("AIP_100_HomeOffice_DateLetterSent")
      .post("/date-letter-sent")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("day", Environment.currentDate.getDayOfMonth.toString)
      .formParam("month", Environment.currentDate.getMonthValue.toString)
      .formParam("year", Environment.currentDate.getYear.toString)
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("Upload your Home Office decision letter")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User uploads the decision letter
  .group("AIP_110A_HomeOffice_UploadDecisionLetter") {
    exec(http("AIP_110A_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter/upload?_csrf=#{csrf}")
      .headers(Headers.commonHeader)
      .header("content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryBOgHbWr4LuU2ZuBi")
      .bodyPart(RawFileBodyPart("file-upload", "HORefusal.pdf")
        .fileName("HORefusal.pdf").transferEncoding("binary")).asMultipartForm
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Checks that the file has been uploaded successfully
  .group("AIP_110B_HomeOffice_UploadDecisionLetter") {
    exec(http("AIP_110B_HomeOffice_UploadDecisionLetter")
      .get("/home-office-upload-decision-letter")
      .headers(Headers.commonHeader)
      .check(regex("HORefusal.pdf"))
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Save the Decision letter file
  .group("AIP_110C_HomeOffice_UploadDecisionLetter") {
    exec(http("AIP_110C_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("file-upload", "")
      .formParam("saveAndContinue", "")
      .check(regex("Has a deportation order been made against you?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //NEW STEP 12/2024 - Has a deportation order been made against you?
  .group("AIP_120_HomeOffice_DeportationOrder") {
    exec(http("AIP_120_HomeOffice_DeportationOrder")
      .post("/deportation-order")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("answer", "No")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Select Enter Personal Details
  val PersonalDetails = group("AIP_130_Name_GET") {
    exec(http("AIP_130_Name_GET")
      .get("/name")
      .headers(Headers.commonHeader)
      .check(regex("What is your name?"))
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Name Details
  .group("AIP_140_Name_Post") {
    exec(http("AIP_140_Name_Post")
      .post("/name")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("givenNames", "Perf Test")
      .formParam("familyName", "IAC")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your date of birth?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Date of Birth Details
  .group("AIP_150_Date-Birth_Post") {
    exec(http("AIP_150_Date-Birth_Post")
      .post("/date-birth")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("day", "01")
      .formParam("month", "01")
      .formParam("year", "1980")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your nationality?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Nationality
  .group("AIP_160_Nationality_Post") {
    exec(http("AIP_160_Nationality_Post")
      .post("/nationality")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("nationality", "AF")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your address?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Go to page to Enter Address Manually as the post code lookup is failing
  .group("AIP_170_Manual_Address_Get") {
    exec(http("AIP_170_Manual_Address_Get")
      .get("/manual-address")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("What is your address?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Address Manually
  .group("AIP_180_Manual_Address_Post") {
    exec(http("AIP_180_Manual_Address_Get")
      .post("/manual-address")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("address-line-1", "Flat 21")
      .formParam("address-line-2", "214 Westferry Road")
      .formParam("address-town", "London")
      .formParam("address-county", "London")
      .formParam("address-postcode", "e14 3rr")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Go to contact preferences page
  val ContactDetails = group("AIP_190_ContactPreferences_GET") {
    exec(http("AIP_190_ContactPreferences_GET")
      .get("/contact-preferences")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("How do you want us to contact you?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter contact preferences
  .group("AIP_200_ContactPreferences_POST") {
    exec(http("AIP_200_ContactPreferences_POST")
      .post("/contact-preferences")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("selections", "email")
      .formParam("email-value", "perftestiac001@gmail.com")
      .formParam("text-message-value", "")
      .formParam("saveAndContinue", "")
      .check(regex("Do you have a sponsor?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Do you have a sponsor - step removed 12/2024, re-added 04/2025
  .group("AIP_210_Sponsor_POST") {
    exec(http("AIP_210_Sponsor_POST")
      .post("/has-sponsor")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("questionId", "")
      .formParam("answer", "No")
      .formParam("continue", "")
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Go to decision type page
  val DecisionType = group("AIP_220_DecisionType_GET") {
    exec(http("AIP_220_DecisionType_GET")
      .get("/decision-type")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("How do you want your appeal to be decided?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Appeal without a hearing
  .group("AIP_230_DecisionType_POST") {
    exec(http("AIP_230_DecisionType_POST")
      .post("/decision-type")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("answer", "decisionWithoutHearing")
      .formParam("saveAndContinue", "")
      .check(regex("Do you want to pay for the appeal now")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Pay the appeal later - this leads to equality and diversity questions which are not included in the journey
  group("AIP_230_PayLater_POST") {
    exec(http("AIP_230_PayLater_POST")
      .post("/pay-now")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("answer", "payLater")
      .formParam("saveAndContinue", "")
      .check(regex("Equality and diversity questions")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //NEW STEP Go to Equality
  //Go to Equality page
  val Equality = group("AIP_231_Equality_GET") {
    exec(http("AIP_231_Equality_GET")
      .get("https://pcq.perftest.platform.hmcts.net/start-page")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("Equality and diversity questions")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //NEW STEP Equality
  .group("AIP_232_Equality_Post") {
    exec(http("AIP_232_Equality_Post")
      .post("https://pcq.perftest.platform.hmcts.net/opt-out")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("opt-out-button", "")
      .check(regex("Tell us about your appeal")))
  }


  //Support with fees
  val FeeSupport = group("AIP_240_FeeSupport_GET") {
    exec(http("AIP_240_FeeSupport_GET")
      .get("/fee-support")
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("Do you have to pay the fee?")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Do not request support
  .group("AIP_250_FeeSupport_POST") {
    exec(http("AIP_250_FeeSupport_POST")
      .post("/fee-support")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("answer", "noneOfTheseStatements")
      .formParam("saveAndContinue", "")
      .check(regex("Help with paying the fee")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Pay for the appeal now
  .group("AIP_260_FeeSupport_HelpWithFees_POST") {
    exec(http("AIP_260_FeeSupport_HelpWithFees_POST")
      .post("/help-with-fees")
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("answer", "willPayForAppeal")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Verifies the information entered is valid
  val CheckAnswers = group("AIP_270_CheckAnswers_GET") {
    exec(http("AIP_270_CheckAnswers_GET")
      .get("/check-answers")
      .headers(Headers.commonHeader)
      .check(regex("Check your answers"))
      .check(regex("I believe the information I have given is true"))
      .check(CsrfCheck.save))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Accepts the details are valid and submits the appeal
  .group("AIP_280_CheckAnswers_POST") {
    exec(http("AIP_280_CheckAnswers_POST")
      .post("/check-answers")
      .headers(Headers.commonHeader)
      .header("content-Type", "application/x-www-form-urlencoded")
      .formParam("_csrf", "#{csrf}")
      .formParam("statement", "acceptance")
      .check(regex("Your appeal details have been sent")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //User goes to the Appeal Overview page and gets the reference number for the appeal. This is stored in
  //AIPAppealRef.csv by the script. It can be used for future business journeys once they are developed.
  val AppealOverview = group("AIP_290_AppealOverview_GET") {
    exec(http("AIP_290_AppealOverview_GET")
      .get("/appeal-overview")
      .headers(Headers.commonHeader)
      .check(regex("""Appeal reference: (.*)</p>""").saveAs("AppealRef")))

    .exec { session =>
      val fw = new BufferedWriter(new FileWriter("AIPAppealRef.csv", true))
      try {
        fw.write(session("AppealRef").as[String] + "\r\n")
      } finally fw.close()
      session
    }
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Log out
  val AIPLogout = group("AIP_300_Logout_GET") {
    exec(http("AIP_300_Logout_GET")
      .get("/logout")
      .headers(Headers.commonHeader)
      .check(regex("Appeal an immigration or asylum decision")))
  }
  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)




  //The following section contains requests which are no longer part of the journey

  /*
  //User first answers eligibility questions before he is allowed to login. First question - Are you currently in UK
  val eligibility =group("AIP_020_eligibility_GET") {
    exec(http("AIP_020_eligibility_GET")
      .get("/eligibility")
      .check(CurrentPageCheck.save)
      .check(CsrfCheck.save)
      .check(status.is(200))
      //.check(regex("Are you currently living in the United Kingdom"))
      .check(regex("Are you currently in detention"))
      //.headers(Headers.headers_19))
      .headers(Headers.commonHeader))
  }
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

     // Eligibility - Q1 Are you living in UK - Answers yes
      .group("AIP_030_eligibility_Question1_POST") {
        exec(http("AIP_030_eligibility_Question1_POST")
          .post("/eligibility")
          //.headers(Headers.headers_34)
          .headers(Headers.commonHeader)
          .formParam("_csrf", "#{csrf}")
          .formParam("questionId", "0")
          .formParam("answer", "no")
          .formParam("continue", "")
          .check(status.is(200))
          .check(CsrfCheck.save))
      }
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    // Eligibility - Q2 Are you in Detention - Answers no
    .group("AIP_040_eligibility_Question2_POST") {
      exec(http("AIP_040_eligibility_Question2_POST")
        .post("/eligibility")
        // .headers(Headers.headers_34)
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("questionId", "0")
        .formParam("answer", "no")
        .formParam("continue", "")
        .check(status.is(200))
        //.check(CsrfCheck.save)
      )
    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


    // Eligibility - Q3 Are you Appealing an asylum or Humanitarian Decision - Answers Yes
    .group("AIP_050_eligibility_Question3_POST") {
      exec(http("AIP_050_eligibility_Question3_POST")
        .post("/eligibility")
       // .headers(Headers.headers_34)
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("questionId", "2")
        .formParam("answer", "yes")
        .formParam("continue", "")
        .check(status.is(200)))
    }


      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User gets to Create Account Page after successfully answering the eligibility questions - replaced by new step below
  val LoginHomePage =group("AIP_060_CreateAcct_GET") {
    exec (http ("AIP_060_CreateAcct_GET")
      .get("/login?register=true")
     // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check (status.is (200))
    //  .check(regex("client_id=iac&state=([0-9a-z-]+?)&scope").saveAs("state"))
    //  .check(regex("response_type=code&state=([0-9a-z-]+?)").saveAs("state"))
      .check(css("input[name='state'], value").saveAs("state"))
      .check(CsrfCheck.save)
      .check(regex("Create an account or sign in")))
  }
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Go to Login Page, separate from account creation page
  .group("AIP_070_Login_GET") {
    exec (http("AIP_070_Login_GET")
      .get(IdAMURL + "/login?redirect_uri=https%3a%2f%2fimmigration-appeal.perftest.platform.hmcts.net%2fredirectUrl&client_id=iac&state=#{state}&scope=")
      .headers(Headers.commonHeader)
      .check(regex("Sign in or create an account")))
  }
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)
  */


}