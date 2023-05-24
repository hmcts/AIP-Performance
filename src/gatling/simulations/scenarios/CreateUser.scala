package scenarios

import java.io.{BufferedWriter, FileWriter}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.{Common, Environment}

object CreateUser {

  val IdamAPIURL = Environment.idamAPIURL

  val newUserFeeder = Iterator.continually(Map(
    "emailAddress" -> ("AIP_Citizen_" + Common.getDay() + Common.randomString(5) + "@mailinator.com"),
    "password" -> "Pa55word11",
    "role" -> "citizen"
  ))

  //takes an userType e.g. petitioner/respondent, to create unique users for each user
  def CreateCitizen(userType: String) = {
    feed(newUserFeeder)
      .group("CMC User Create") {
        exec(http("CreateCitizen")
          .post(IdamAPIURL + "/testing-support/accounts")
          .body(ElFileBody("CreateUserTemplate.json")).asJson
          .check(status.is(201)))
      }
  
      .exec { session =>
        val fw = new BufferedWriter(new FileWriter("AIPUserDetails.csv", true))
        try {
          fw.write(session("emailAddress").as[String] + "," + session("password").as[String]  + "\r\n")
        } finally fw.close()
        session
      }
    
    
  }

}
