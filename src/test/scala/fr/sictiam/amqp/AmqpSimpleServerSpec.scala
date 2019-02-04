/*
 *  Copyright (C) 2019 SICTIAM
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package fr.sictiam.amqp

import akka.Done
import fr.sictiam.amqp.api.{AmqpMessage, AmqpSimpleServer}
import play.api.libs.json.{JsString, JsValue}

import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-01
  */

class AmqpSimpleServerSpec extends AmqpSpec {
  val headers = Map.empty[String, JsValue]

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AmqpSimpleServer" should {
    val server = new AmqpSimpleServer("serverTest")
    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three"))
    )
    "publish a message without error" in {

      server.publish(messages).futureValue shouldBe Done
    }

    "receive a message without error" in {
      val results = server.consume(3).futureValue
      results.size shouldEqual 3
      results.map { msg =>
        msg.body.as[String]
      } shouldBe Seq("One", "Two", "Three")
    }
  }
}
