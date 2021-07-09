
package uk.gov.hmcts.reform.aip.performance.scenarios

import java.io.{BufferedWriter, FileWriter}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.{CsrfCheck, Environment,Headers}

/*AIP Appeal is a new Journey which allows an appellant to go through several eligibility questions and if successful
they can login to the new AIP portal and start an appeal. This script carries out this business journey all the way
until the appeal is submitted and then captures and stores the appeal reference which we can use in business journeys
which are still to be developed*/

object AIP_Appeal {

val BaseURL = Environment.baseURL
val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime

  //AIP Start Appeal HomePage
  val home = group ("AIP_010_Homepage") {
    exec(http("AIP_010_Homepage")
      .get("/start-appeal")
      .headers(Headers.headers_2)
      .check(regex("Appeal an immigration or asylum decision")))
  }
  .pause(MinThinkTime, MaxThinkTime)

  //User first answers eligibility questions before he is allowed to login.  First question - Are you currently in UK
  val eligibility =group("AIP_020_eligibility_GET") {
    exec(http("AIP_020_eligibility_GET")
      .get("/eligibility")
      .headers(Headers.headers_19)
      .check(CsrfCheck.save)
      .check(regex("Are you currently living in the United Kingdom")))

  }
    .pause(MinThinkTime, MaxThinkTime)

    // Eligibity - Q1 Are you living in UK - Answers yes
    .group("AIP_030_eligibility_Question1_POST") {
      exec(http("AIP_030_eligibility_Question1_POST")
        .post("/eligibility")
        .headers(Headers.headers_34)
        .formParam("_csrf", "${csrf}")
        .formParam("questionId", "0")
        .formParam("answer", "yes")
        .formParam("continue", "")
        .check(CsrfCheck.save)
        .check(regex("Are you currently in detention")))
    }
    .pause(MinThinkTime, MaxThinkTime)

    // Eligibity - Q2 Are you in Detention - Answers no
    .group("AIP_040_eligibility_Question2_POST") {
      exec(http("AIP_040_eligibility_Question2_POST")
        .post("/eligibility")
        .headers(Headers.headers_34)
        .formParam("_csrf", "${csrf}")
        .formParam("questionId", "1")
        .formParam("answer", "no")
        .formParam("continue", "")
        .check(CsrfCheck.save)
        .check(regex("Are you appealing an Asylum")))
    }

    .pause(MinThinkTime, MaxThinkTime)

    // Eligibity - Q3 Are you Appealing an asylum or Humanitarian Decision - Answers Yes
    .group("AIP_050_eligibility_Question3_POST") {
      exec(http("AIP_050_eligibility_Question3_POST")
        .post("/eligibility")
        .headers(Headers.headers_34)
        .formParam("_csrf", "${csrf}")
        .formParam("questionId", "2")
        .formParam("answer", "yes")
        .formParam("continue", "")
        .check(regex("You can use the new service")))
    }

    .pause(MinThinkTime, MaxThinkTime)

  // Users gets to Login Page after successfully answering the eligibility questions
  val Login =group("AIP_060_Login_GET") {
    exec (http ("AIP_060_Login_GET")
      .get("/login?register=true")
      .headers(Headers.headers_19)
      .check(regex("client_id=iac&state=([0-9a-z-]+?)&scope").saveAs("state"))
      .check(CsrfCheck.save))
  }

    .pause(MinThinkTime, MaxThinkTime)

    .group("AIP_075_Login_GET") {
      exec(http("AIP_075_Login_POST")
        .get(IdAMURL + "/login?redirect_uri=https%3a%2f%2fimmigration-appeal.perftest.platform.hmcts.net%2fredirectUrl&client_id=iac&state=${state}&scope=")
        .headers(Headers.headers_113)
        .check(regex("Sign in or create an account")))
    }

  //Login into Application with an IAC Citizen account
    .group("AIP_080_Login_POST") {
       exec(http("AIP_080_Login_POST")
         .post(IdAMURL + "/login?redirect_uri=https%3a%2f%2fimmigration-appeal.perftest.platform.hmcts.net%2fredirectUrl&client_id=iac&state=${state}&scope=")
         .headers(Headers.headers_113)
         .formParam("username", "${emailAddress}")
         .formParam("password", "${password}")
         .formParam("save", "Sign in")
         .formParam("selfRegistrationEnabled", "true")
         .formParam("_csrf", "${csrf}")
         .check(regex("Your appeal details")))
    }

    .pause(MinThinkTime, MaxThinkTime)

  //User gets the About Appeal Page after the have logged in
  val AboutAppeal =group("AIP_090_AboutAppeal_GET") {
      exec(http("AIP_090_AboutAppeal_GET")
      .get("/about-appeal")
      .headers(Headers.headers_19)
      .check(regex("Tell us about your appeal")))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //User clicks on Home Office & gets Home Office Reference Number Page
  val HomeOffice =group("AIP_100_HomeOffice_GET") {
    exec(http("AIP_100_HomeOffice_GET")
    .get("/home-office-reference-number")
    .headers(Headers.headers_19)
    .check(CsrfCheck.save)
    .check(regex("What is your Home Office reference number")))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //Enter Home Office Letter Reference - this can be any 10 digit number
  .group("AIP_110_HomeOffice_POST") {
    exec(http("AIP_110_HomeOffice_POST")
    .post("/home-office-reference-number")
    .headers(Headers.headers_34)
    .formParam("_csrf", "${csrf}")
    .formParam("homeOfficeRefNumber", "${HORef}")
    .formParam("saveAndContinue", "")
    .check(CsrfCheck.save)
    .check(regex("What date was your decision letter sent")))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //Date Home Office Letter Sent this date has to be within the last 30 days
  .group("AIP_120_HomeOffice_DateLetterSent") {
    exec(http("AIP_120_HomeOffice_DateLetterSent")
    .post("/date-letter-sent")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("day", "${HOday}")
      .formParam("month", "${HOmonth}")
      .formParam("year", "${HOyear}")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("Upload your Home Office decision letter")))
  }

    .pause(MinThinkTime, MaxThinkTime)

    //User uploads the decision letter
    .group("AIP_130A_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130A_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter/upload?_csrf=${csrf}")
      .headers(Headers.headers_0)
      .header("content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryBOgHbWr4LuU2ZuBi")
        .bodyPart(RawFileBodyPart("file-upload", "HORefusal.pdf")
          .fileName("HORefusal.pdf")
          .transferEncoding("binary"))
        .asMultipartForm
        .formParam("classification", "PUBLIC")
    .check(CsrfCheck.save))
    }

    .pause(MinThinkTime, MaxThinkTime)

    //Checks that the file has been uploaded successfully
    .group("AIP_130C_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130C_HomeOffice_UploadDecisionLetter")
        .get("/home-office-upload-decision-letter")
        .headers(Headers.headers_19)
        .check(regex("HORefusal.pdf"))
        .check(CsrfCheck.save))
    }

    .pause(MinThinkTime, MaxThinkTime)

    //Save the Decision letter file
    .group("AIP_130B_HomeOffice_UploadDecisionLetter") {
      exec(http("AIP_130B_HomeOffice_UploadDecisionLetter")
      .post("/home-office-upload-decision-letter")
        .headers(Headers.headers_17)
        .formParam("_csrf", "${csrf}")
        .formParam("file-upload", "")
        .formParam("saveAndContinue", "")
        .check(regex("Tell us about your appeal")))
    }

    .pause(MinThinkTime, MaxThinkTime)

  //Select Enter Personal Details
  val PersonalDetails =

    group("AIP_140_Name_GET") {
      exec(http("AIP_140_Name_GET")
      .get("/name")
      .headers(Headers.headers_19)
      .check(regex("What is your name?"))
      .check(CsrfCheck.save))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //Enter Name Details
  .group("AIP_150_Name_Post") {
    exec(http("AIP_150_Name_Post")
    .post("/name")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("givenNames", "${givenname}")
      .formParam("familyName", "${familyname}")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your date of birth?")))
  }

   .pause(MinThinkTime, MaxThinkTime)

  //Enter Date of Birth Details
  .group("AIP_160_Date-Birth_Post") {
    exec(http("AIP_160_Date-Birth_Post")
    .post("/date-birth")
    .headers(Headers.headers_34)
    .formParam("_csrf", "${csrf}")
    .formParam("day", "${day}")
    .formParam("month", "${month}")
    .formParam("year", "${year}")
    .formParam("saveAndContinue", "")
    .check(CsrfCheck.save)
    .check(regex("What is your nationality?")))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //Enter Nationality
  .group("AIP_170_Nationality_Post") {
    exec(http("AIP_170_Nationality_Post")
    .post("/nationality")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("nationality", "${nationality}")
      .formParam("saveAndContinue", "")
      .check(CsrfCheck.save)
      .check(regex("What is your address?")))
  }

    .pause(MinThinkTime, MaxThinkTime)

  //Go to page toEnter Address Manually as the post code lookup is failing
  .group("AIP_180_Manual_Address_Get") {
    exec(http("AIP_180_Manual_Address_Get")
      .get("/manual-address")
      .headers(Headers.headers_19)
      .check(CsrfCheck.save)
      .check(regex("What is your address?")))
  }
    .pause(MinThinkTime, MaxThinkTime)

  //Enter Address Manually
  .group("AIP_190_Manual_Address_Post"){
    exec(http("AIP_190_Manual_Address_Get")
      .post("/manual-address")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("address-line-1", "Flat 21")
      .formParam("address-line-2", "214 Westferry Road")
      .formParam("address-town", "London")
      .formParam("address-county", "London")
      .formParam("address-postcode", "e14 3rr")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
    }

    .pause(MinThinkTime, MaxThinkTime)

//Go to contact preferences page
  val ContactDetails =group("AIP_200_ContactPreferences_GET") {
    exec(http("AIP_200_ContactPreferences_GET")
      .get("/contact-preferences")
      .headers(Headers.headers_19)
      .check(CsrfCheck.save)
      .check(regex("How do you want us to contact you?")))
  }

  .pause(MinThinkTime, MaxThinkTime)

  //Enter contact preferences
 .group("AIP_210_ContactPreferences_POST") {
    exec(http("AIP_210_ContactPreferences_POST")
      .post("/contact-preferences")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("selections", "email")
      .formParam("email-value", "perftestiac001@gmail.com")
      .formParam("text-message-value", "")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }

  .pause(MinThinkTime, MaxThinkTime)

  //Go to Type of Appeal Page
  val TypeofAppeal =group("AIP_220_AppealType_GET") {
    exec(http("AIP_220_AppealType_GET")
      .get("/appeal-type")
      .headers(Headers.headers_19)
      .check(regex("What is your appeal type?"))
      .check(CsrfCheck.save))
  }

  .pause(MinThinkTime, MaxThinkTime)

  //Enter Type of appeal - Protection
  .group("AIP_230_AppealType_POST") {
    exec(http("AIP_230_AppealType_POST")
      .post("/appeal-type")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("appealType", "protection")
      .formParam("saveAndContinue", "")
      .check(regex("Tell us about your appeal")))
  }

  .pause(MinThinkTime, MaxThinkTime)

  //Verifies the information entered is valid
  val CheckAnswers =group("AIP_240_CheckAnswers_GET") {
    exec(http("AIP_240_CheckAnswers_GET")
      .get("/check-answers")
      .headers(Headers.headers_19)
      .check(regex("Check your answer"))
      .check(regex("I believe the information I have given is true"))
      .check(CsrfCheck.save))
  }

  .pause(MinThinkTime, MaxThinkTime)

    //Accepts the details are valid and submits the appeal
   .group("AIP_250_CheckAnswers_POST") {
     exec(http("AIP_250_CheckAnswers_POST")
      .post("/check-answers")
      .headers(Headers.headers_34)
      .formParam("_csrf", "${csrf}")
      .formParam("statement", "acceptance")
      .check(regex("Your appeal details have been sent")))
   }

  .pause(MinThinkTime, MaxThinkTime)

  //User goes tot the Appeal Overview page and gets the reference  number for the appeal.  This is stored in
  // AIPAppeakRef.csv by the script.  this can be used for future business journeys once they are developed
  val AppealOverview =group("AIP_260_AppealOverview_GET") {
    exec(http("AIP_260_AppealOverview_GET")
      .get("/appeal-overview")
      .headers(Headers.headers_19)
      .check(regex("""Appeal reference:(.*)</p>""").saveAs("AppealRef")))

    .exec { session =>
      val fw = new BufferedWriter(new FileWriter("AIPAppealRef.csv", true))
      try {
        fw.write(session("AppealRef").as[String] + "\r\n")
      } finally fw.close()
      session
    }
  }

  .pause(MinThinkTime, MaxThinkTime)

  val AIPLogout =group("AIP_270_Logout_GET") {
    exec(http("AIP_270_Logout_GET")
      .get("/logout")
      .headers(Headers.headers_19))
  }
}