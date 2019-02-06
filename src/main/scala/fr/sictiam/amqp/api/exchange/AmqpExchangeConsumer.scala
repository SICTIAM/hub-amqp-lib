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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import akka.stream.scaladsl.Sink
import fr.sictiam.amqp.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

class AmqpExchangeConsumer(val exchangeName: String, val exchangeType: ExchangeTypes.ExchangeTypeVal, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericConsumer with Exchange {

  override def init: Unit = {}

  override def consume(nbMsgToTake: Long): Future[Seq[AmqpMessage]] = {

    val amqpSource = AmqpSource.atMostOnceSource(
      TemporaryQueueSourceSettings(connectionProvider, exchangeName)
        .withDeclaration(exchangeDeclaration),
      bufferSize = prefetchCount
    )
    val result = amqpSource.take(nbMsgToTake).runWith(Sink.seq)
    result.map(_.map(msg => AmqpMessage(msg.bytes.utf8String)))

  }
}
