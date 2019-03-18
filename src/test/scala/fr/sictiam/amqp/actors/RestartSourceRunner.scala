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
package fr.sictiam.amqp.actors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.dispatch.ExecutionContexts
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource, CommittableIncomingMessage}
import akka.stream.scaladsl.{Flow, RestartSource, Source}
import akka.util.ByteString
import fr.sictiam.amqp.api.{AmqpClientConfiguration, AmqpConfiguration, ExchangeTypes}

import scala.collection.immutable
import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-15
  */

object RestartSourceRunner extends App with AmqpConfiguration {

  implicit val system = ActorSystem("RestartSourceRunnerSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = ExecutionContexts.global()

  val exchangeName = "testExchange"
  val topic = "graph.create.node"

  val exchangeType: ExchangeTypes.ExchangeTypeVal = ExchangeTypes.Topic

  val connectionProvider = AmqpDetailsConnectionProvider("invalid", 5673)
    .withCredentials(AmqpCredentials(user, pwd))
    .withHostsAndPorts(immutable.Seq(host -> port))
    .withAutomaticRecoveryEnabled(automaticRecoveryEnabled)
    .withTopologyRecoveryEnabled(topologyRecoveryEnabled)


  lazy val exchangeDeclaration = ExchangeDeclaration(exchangeName, exchangeType.label).withDurable(AmqpClientConfiguration.durable)
  lazy val sourceSettings: AmqpSourceSettings = TemporaryQueueSourceSettings(connectionProvider, exchangeName).withDeclaration(exchangeDeclaration)

  val source: Source[CommittableIncomingMessage, NotUsed] = AmqpSource.committableSource(
    TemporaryQueueSourceSettings(connectionProvider, exchangeName)
      .withDeclaration(exchangeDeclaration)
      .withRoutingKey(topic),
    bufferSize = 10
  )

  val flow = Flow[CommittableIncomingMessage].map { cm =>
    cm.ack()
    println("processing message")
    OutgoingMessage(ByteString("OK"), true, true)
  }

  val sink = AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider))

  val restartSrc = RestartSource.withBackoff(100 milliseconds, 1000 milliseconds, 0.2) { () => source }
  restartSrc.via(flow).runWith(sink)
}
