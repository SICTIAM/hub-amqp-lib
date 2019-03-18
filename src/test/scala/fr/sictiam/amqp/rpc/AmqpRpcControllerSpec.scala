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
package fr.sictiam.amqp.rpc

import akka.Done
import akka.actor.Terminated
import akka.util.ByteString
import fr.sictiam.amqp.AmqpSpec
import fr.sictiam.amqp.api.AmqpMessage
import fr.sictiam.amqp.api.rpc.{AmqpRpcController, AmqpRpcTopicProducer}
import fr.sictiam.amqp.rpc.tasks.{FakeCreateTask, FakeDeleteTask, FakeReadTask, FakeUpdateTask}
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-27
  */

class AmqpRpcControllerSpec extends AmqpSpec {

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "AmqpRpcController" should {

    val exchange = "rpcTaskExchangeTest"

    val topic1 = "graph.create.triples"
    val topic2 = "graph.read.triples"
    val topic3 = "graph.update.triples"
    val topic4 = "graph.delete.triples"
    val replyBuffer = mutable.Buffer[String]()

    val controller = new AmqpRpcController("serviceTest")
    controller.registerTask(topic1, new FakeCreateTask(topic1, exchange))
    controller.registerTask(topic2, new FakeReadTask(topic2, exchange))
    controller.registerTask(topic3, new FakeUpdateTask(topic3, exchange))
    controller.registerTask(topic4, new FakeDeleteTask(topic4, exchange))

    val producer = new AmqpRpcTopicProducer(exchange, "Fake RPC Topic Producer service") {
      override def onReply(msg: ByteString): Unit = {
        replyBuffer += msg.utf8String
      }
    }

    val headers = Map.empty[String, JsValue]

    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three")),
      AmqpMessage(headers, JsString("Four")),
      AmqpMessage(headers, JsString("Five")),
      AmqpMessage(headers, JsString("Six")),
      AmqpMessage(headers, JsString("Seven")),
      AmqpMessage(headers, JsString("Eight")),
      AmqpMessage(headers, JsString("Nine")),
      AmqpMessage(headers, JsString("Ten"))
    )

    "start without error" in {
      controller.start
    }
    "receive messages and process them without error" in {
      producer.publish(topic1, messages).futureValue shouldBe Done
      producer.publish(topic2, messages).futureValue shouldBe Done
      producer.publish(topic3, messages).futureValue shouldBe Done
      producer.publish(topic4, messages).futureValue shouldBe Done
      replyBuffer.size shouldEqual messages.size * 4
      replyBuffer.filter(_.contains("graph.create.triples")).size shouldEqual messages.size
      replyBuffer.filter(_.contains("graph.read.triples")).size shouldEqual messages.size
      replyBuffer.filter(_.contains("graph.update.triples")).size shouldEqual messages.size
      replyBuffer.filter(_.contains("graph.delete.triples")).size shouldEqual messages.size
    }

    "stop without error" in {
      controller.shutdown.futureValue.isInstanceOf[Terminated] shouldBe true
    }
  }
}
