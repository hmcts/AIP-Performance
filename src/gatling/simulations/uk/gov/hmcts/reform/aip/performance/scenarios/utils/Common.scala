package uk.gov.hmcts.reform.aip.performance.scenarios.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scala.util.Random

object Common {

  val rnd = new Random()
  val now = LocalDate.now()
  val patternDay = DateTimeFormatter.ofPattern("dd")
  val patternMonth = DateTimeFormatter.ofPattern("MM")
  val patternYear = DateTimeFormatter.ofPattern("yyyy")

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }
  
  def randomRepeatInteger() : Integer=
  {
    val list=List(300, 280, 260, 250, 240, 200,220,350)
    //val list=List(2,3)
    val start = 200
    val end   = 300
    val rnd = new scala.util.Random
    list(rnd.nextInt(list.length))
  }
  
  import scala.util.Random
  object MyClass {
    def main(args: Array[String]) {
      val list = List(12, 65, 89, 41, 99, 102)
      val random = new Random
      println("Random value of the list " + list(random.nextInt(list.length)))
    }
  }


 def getDay(): String = {
    (1 + rnd.nextInt(28)).toString
  }

  def getMonth(): String = {
    (1 + rnd.nextInt(12)).toString
  }
  /*
  def getDay(): String = {
    (10 + rnd.nextInt(28)).toString.format(patternDay).reverse.padTo(2, '0').reverse //pads single-digit dates with a leading zero
  }

  def getMonth(): String = {
    (11 + rnd.nextInt(12)).toString.format(patternMonth).reverse.padTo(2, '0').reverse //pads single-digit dates with a leading zero
  }
  */
  //Dod <= 21 years
  def getYear(): String = {
    now.minusYears(30 + rnd.nextInt(50)).format(patternYear)
  }

/*
  val reasonForAppealing = "Social Security and Child Support forms including notices of appeal to the Department of Work and Pensions and HM Revenue and Customs.
    You can appeal a decision about your entitlement to benefits, for example Personal Independence Payment (PIP), Employment and Support Allowance (ESA) and Universal Credit.
    Appeals are decided by the Social Security and Child Support Tribunal. The tribunal is impartial and independent of government. The tribunal will listen to both sides before making a decision.
    You can appeal a decision about your entitlement to benefits, for example Personal Independence Payment (PIP), Employment and Support Allowance (ESA) or Universal Credit.
    If you do not need a mandatory reconsideration your decision letter will say why. Include this explanation when you submit your appeal.
    You’ll need to choose whether you want to go to the tribunal hearing to explain your appeal in person. If you do not attend, your appeal will be decided on your appeal form and any supporting evidence you provide."

  val anythingElse = "Things that were not considered are. 
    You can appoint someone as a ‘representative’ to help you with your appeal. A representative can.help you submit your appeal or prepare your evidence act on your behalf give you advice. Anyone can be a representative, including friends and family.
    You might also be able to find a representative through a library or from an organisation in your area that gives advice on claiming benefits, such as Citizens Advice.
    Your representative will have permission to act on your behalf, for example to respond to letters. They’ll be sent all the information about your appeal, including any medical evidence."
*/
}