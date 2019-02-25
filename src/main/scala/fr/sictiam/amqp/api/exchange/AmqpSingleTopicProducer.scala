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
package fr.sictiam.amqp.api.exchange

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import fr.sictiam.amqp.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

class AmqpSingleTopicProducer(val exchangeName: String, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericAgent with Exchange {

  override val exchangeType = ExchangeTypes.Topic

  override def init: Unit = {}

  /**
    * Publishes a message to the broker with the name of the queue to
    *
    * @param messages
    * @return
    */
  def publish(topic: String, messages: Vector[AmqpMessage]): Future[Done] = {
    val amqpSink = AmqpSink.simple(
      AmqpSinkSettings(connectionProvider)
        .withExchange(exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic)
    )
    Source(messages).map(s => ByteString(s.toString)).runWith(amqpSink)
  }

}
