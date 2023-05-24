
package scenarios

import java.io.{BufferedWriter, FileWriter}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment, Headers}
import scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment, Headers}
import CsrfCheck.{csrfParameter, csrfTemplate}
import CurrentPageCheck.currentPageTemplate

import scala.concurrent.duration._

/*AIP Appeal is a new Journey which allows an appellant to go through several eligibility questions and if successful
they can login to the new AIP portal and start an appeal. This script carries out this business journey all the way
until the appeal is submitted and then captures and stores the appeal reference which we can use in business journeys
which are still to be developed*/

object AIP_Appeal {

val BaseURL = Environment.baseURL
val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime
val aipuser = csv("AIPUser.csv").circular
val aipuserdetails = csv("AIPUserDetails.csv").circular
val holetterdetails = csv("HOLetterDetails.csv").circular
val personaldetails = csv("PersonalDetails.csv").circular

  //AIP Start Appeal HomePage
  val home = group ("AIP_010_Homepage") {
    //exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
      exec(http("AIP_010_Homepage")
        .get("/start-appeal")
        .check(CurrentPageCheck.save)
        //.check(CsrfCheck.save)
        .check(status.is(200))
        .check(regex("Appeal an immigration or asylum decision"))
       // .headers(Headers.headers_2))
        .headers(Headers.commonHeader))
    
      .exitHereIfFailed
    }
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

      //User first answers eligibility questions before he is allowed to login.  First question - Are you currently in UK
      val eligibility =group("AIP_020_eligibility_GET") {
        exec(http("AIP_020_eligibility_GET")
          .get("/eligibility")
          .check(CurrentPageCheck.save)
          .check(CsrfCheck.save)
          .check(status.is(200))
          .check(regex("Are you currently living in the United Kingdom"))
          //.headers(Headers.headers_19))
          .headers(Headers.commonHeader))
      }
        .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

        // Eligibity - Q1 Are you living in UK - Answers yes
        .group("AIP_030_eligibility_Question1_POST") {
          exec(http("AIP_030_eligibility_Question1_POST")
            .post("/eligibility")
            //.headers(Headers.headers_34)
            .headers(Headers.commonHeader)
            .formParam("_csrf", "#{csrf}")
            .formParam("questionId", "0")
            .formParam("answer", "yes")
            .formParam("continue", "")
            .check(status.is(200))
            .check(CsrfCheck.save))
        }
        .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

        // Eligibity - Q2 Are you in Detention - Answers no
        .group("AIP_040_eligibility_Question2_POST") {
          exec(http("AIP_040_eligibility_Question2_POST")
            .post("/eligibility")
           // .headers(Headers.headers_34)
            .headers(Headers.commonHeader)
            .formParam("_csrf", "#{csrf}")
            .formParam("questionId", "1")
            .formParam("answer", "no")
            .formParam("continue", "")
            .check(status.is(200))
            .check(CsrfCheck.save))
        }
  
        .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

        // Eligibity - Q3 Are you Appealing an asylum or Humanitarian Decision - Answers Yes
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

  // Users gets to Login Page after successfully answering the eligibility questions
  val LoginHomePage =group("AIP_060_Login_GET") {
    exec (http ("AIP_060_Login_GET")
      .get("/login?register=true")
     // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check (status.is (200))
      .check(regex("client_id=iac&state=([0-9a-z-]+?)&scope").saveAs("state"))
      .check(CsrfCheck.save))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Login into Application with an IAC Citizen account
  val Login  =group("AIP_080_Login_POST") {
      feed(aipuser)
       .exec(http("AIP_080_Login_POST")
         .post(IdAMURL + "/login?redirect_uri=https%3a%2f%2fimmigration-appeal.perftest.platform.hmcts.net%2fredirectUrl&client_id=iac&state=#{state}&scope=")
         //.headers(Headers.headers_113)
         .headers(Headers.commonHeader)
         //.headers(Headers.headers_142)
         .formParam("username", "#{aipuser}")
         .formParam("password", "#{password}")
         .formParam("save", "Sign in")
         .formParam("selfRegistrationEnabled", "true")
         .formParam("_csrf", "#{csrf}")
         .check (status.is (200))
         .check(regex("Your appeal details")))
    }

    .exec { session =>
      println(session)
      session
    }
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User gets the About Appeal Page after the have logged in
  val AboutAppeal =group("AIP_090_AboutAppeal_GET") {
      exec(http("AIP_090_AboutAppeal_GET")
      .get("/about-appeal")
      //.headers(Headers.headers_19)
        .headers(Headers.commonHeader)
      .check(regex("Tell us about your appeal"))
      .check (status.is (200)))

  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User clicks on Home Office & gets Home Office Reference Number Page
  val HomeOffice =group("AIP_100_HomeOffice_GET") {
    exec(http("AIP_100_HomeOffice_GET")
    .get("/home-office-reference-number")
   // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
    .check (status.is (200))
    .check(CsrfCheck.save)
    .check(regex("What is your Home Office reference number")))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Home Office Letter Reference - this can be any 10 digit number
  .group("AIP_110_HomeOffice_POST") {
    feed(holetterdetails)
    .exec(http("AIP_110_HomeOffice_POST")
    .post("/home-office-reference-number")
    //.headers(Headers.headers_34)
      .headers(Headers.commonHeader)
    .formParam("_csrf", "#{csrf}")
    .formParam("homeOfficeRefNumber", "123456789")
    .formParam("saveAndContinue", "")
   //.resources(http("request_162")
    .check(CsrfCheck.save)
    .check(regex("What date was your decision letter sent"))
    .check (status.is (200)))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Date Home Office Letter Sent this date has to be within the last 30 days
  .group("AIP_120_HomeOffice_DateLetterSent") {
    feed(holetterdetails)
    .exec(http("AIP_120_HomeOffice_DateLetterSent")
    .post("/date-letter-sent")
     // .headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("day", "08")
      .formParam("month", "07")
      .formParam("year", "2021")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("Upload your Home Office decision letter"))
      .check (status.is (200)))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //User uploads the decision letter
    .group("AIP_130A_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130A_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter/upload?_csrf=#{csrf}")
      //.headers(Headers.headers_0)
        .headers(Headers.commonHeader)
      .header("content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryBOgHbWr4LuU2ZuBi")
        .bodyPart(RawFileBodyPart("file-upload", "HORefusal.pdf")
          .fileName("HORefusal.pdf")
          .transferEncoding("binary"))
        .asMultipartForm
        .formParam("classification", "PUBLIC")
    .check(CsrfCheck.save)
    .check(status in(200, 304)))
    }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Checks that the file has been uploaded successfully
    .group("AIP_130C_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130C_HomeOffice_UploadDecisionLetter")
        .get("/home-office-upload-decision-letter")
        //.headers(Headers.headers_19)
        .headers(Headers.commonHeader)
        .check (status.is (200))
        .check(regex("HORefusal.pdf"))
        .check(CsrfCheck.save))
    }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Save the Decision letter file
    .group("AIP_130B_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130B_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter")
        //.headers(Headers.headers_17)
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("file-upload", "")
        .formParam("saveAndContinue", "")
        .check(regex("Tell us about your appeal"))
        .check (status.is (200)))
    }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Select Enter Personal Details
  val PersonalDetails =

    group("AIP_140_Name_GET") {
    feed(personaldetails)
    .exec(http("AIP_140_Name_GET")
    .get("/name")
   // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
    .check (status.is (200))
    .check(regex("What is your name?"))
    .check(CsrfCheck.save))
  }
  
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Name Details
  .group("AIP_150_Name_Post") {
    feed(personaldetails)
    .exec(http("AIP_150_Name_Post")
    .post("/name")
     // .headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("givenNames", "Perf Test")
      .formParam("familyName", "IAC")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your date of birth?"))
      .check (status.is (200)))
  }
  
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Date of Birth Details
  .group("AIP_160_Date-Birth_Post") {
    feed(personaldetails)
    .exec(http("AIP_160_Date-Birth_Post")
    .post("/date-birth")
    //.headers(Headers.headers_34)
      .headers(Headers.commonHeader)
    .formParam("_csrf", "#{csrf}")
    .formParam("day", "01")
    .formParam("month", "01")
    .formParam("year", "1980")
    .formParam("saveAndContinue", "")
    //.resources(http("request_264")
    .check(CsrfCheck.save)
    .check(regex("What is your nationality?"))
    .check (status.is (200)))
  }
  
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Nationality
  .group("AIP_170_Nationality_Post") {
    feed(personaldetails)
    .exec(http("AIP_170_Nationality_Post")
    .post("/nationality")
     // .headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("nationality", "AF")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your address?"))
      .check (status.is (200)))
  }
  
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Go to page toEnter Address Manually as the post code lookup is failing
  .group("AIP_180_Manual_Address_Get") {
    exec(http("AIP_180_Manual_Address_Get")
      .get("/manual-address")
     // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("What is your address?"))
      .check (status.is (200)))
  }
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Address Manually
  .group("AIP_190_Manual_Address_Post"){
    exec(http("AIP_190_Manual_Address_Get")
      .post("/manual-address")
      //.headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("address-line-1", "Flat 21")
      .formParam("address-line-2", "214 Westferry Road")
      .formParam("address-town", "London")
      .formParam("address-county", "London")
      .formParam("address-postcode", "e14 3rr")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal"))
      .check (status.is (200)))
    }
  
      .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

//Go to contact preferences page
  val ContactDetails =group("AIP_200_ContactPreferences_GET") {
    exec(http("AIP_200_ContactPreferences_GET")
      .get("/contact-preferences")
      //.headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check(CsrfCheck.save)
      .check(regex("How do you want us to contact you?"))
      .check (status.is (200)))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter contact preferences
 .group("AIP_210_ContactPreferences_POST") {
    exec(http("AIP_210_ContactPreferences_POST")
      .post("/contact-preferences")
      //.headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("selections", "email")
      .formParam("email-value", "perftestiac001@gmail.com")
      .formParam("text-message-value", "")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal"))
      .check (status.is (200)))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Go to Type of Appeal Page
  val TypeofAppeal =group("AIP_220_AppealType_GET") {
    exec(http("AIP_220_AppealType_GET")
      .get("/appeal-type")
      //.headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check (status.is (200))
      .check(regex("What is your appeal type?"))
      .check(CsrfCheck.save))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Enter Type of appeal - Protection
  .group("AIP_230_AppealType_POST") {
    exec(http("AIP_230_AppealType_POST")
      .post("/appeal-type")
     // .headers(Headers.headers_34)
      .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("appealType", "protection")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal"))
      .check (status.is (200)))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Verifies the information entered is valid
  val CheckAnswers =group("AIP_240_CheckAnswers_GET") {
    exec(http("AIP_240_CheckAnswers_GET")
      .get("/check-answers")
      //.headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check (status.is (200))
      .check(regex("Check your answer"))
      .check(regex("I believe the information I have given is true"))
      .check(CsrfCheck.save))
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Accepts the details are valid and submits the appeal
   .group("AIP_250_CheckAnswers_POST") {
     exec(http("AIP_250_CheckAnswers_POST")
      .post("/check-answers")
     // .headers(Headers.headers_34)
       .headers(Headers.commonHeader)
      .formParam("_csrf", "#{csrf}")
      .formParam("statement", "acceptance")
      .check(regex("Your appeal details have been sent"))
      .check (status.is (200)))
   }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User goes tot the Appeal Overview page and gets the reference  number for the appeal.  This is stored in
  // AIPAppeakRef.csv by the script.  this can be used for future business journeys once they are developed
  val AppealOverview =group("AIP_260_AppealOverview_GET") {
    exec(http("AIP_260_AppealOverview_GET")
      .get("/appeal-overview")
     // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check(regex("""Appeal reference:(.*)</p>""").saveAs("AppealRef"))
      .check (status.is (200)))

    .exec { session =>
      val fw = new BufferedWriter(new FileWriter("AIPAppealRef.csv", true))
      try {
        fw.write(session("AppealRef").as[String] + "\r\n")
      } finally fw.close()
      session
    }
  }
  
    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  val AIPLogout =group("AIP_270_Logout_GET") {
    exec(http("AIP_270_Logout_GET")
      .get("/logout")
     // .headers(Headers.headers_19)
      .headers(Headers.commonHeader)
      .check (status.is (200)))
  //  .check(CsrfCheck.save))
  }
}