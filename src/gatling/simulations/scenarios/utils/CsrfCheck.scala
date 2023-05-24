package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.css.CssCheckType
import jodd.lagarto.dom.NodeSelector

object CsrfCheck {
  def save: CheckBuilder[CssCheckType, NodeSelector] = css("input[name='_csrf']", "value").saveAs("csrf")

  def csrfParameter: String = "_csrf"
  def csrfTemplate: String = "${csrf}"
}

