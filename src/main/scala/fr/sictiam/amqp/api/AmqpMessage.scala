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
package fr.sictiam.amqp.api

import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import com.rabbitmq.client.AMQP.BasicProperties
import fr.sictiam.exceptions.MessageParsingException
import play.api.libs.json.{Format, JsObject, JsValue, Json}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-31
  */

case class AmqpMessage(val headers: Map[String, JsValue], val body: JsValue) {

  override def toString: String = Json.stringify(toJson())

  def toJson(): JsObject = Json.obj(
    "headers" -> Json.toJson(headers),
    "body" -> body
  )

  def toIncomingMessage(): IncomingMessage = {
    IncomingMessage(ByteString(this.toString), null, null)
  }

  def toOutgoingMessage(immediate: Boolean, mandatory: Boolean, properties: BasicProperties): OutgoingMessage = {
    OutgoingMessage(ByteString(this.toString), immediate, mandatory).withProperties(properties)
  }

}

object AmqpMessage {

  implicit val format: Format[AmqpMessage] = Json.format[AmqpMessage]

  def apply(s: String): AmqpMessage = parse(s)

  def parse(s: String): AmqpMessage = Json.parse(s).as[AmqpMessage]

  def apply(im: IncomingMessage): AmqpMessage = {
    val parsing = Json.parse(im.bytes.utf8String).validate[AmqpMessage]
    if (parsing.isSuccess) parsing.get
    else throw new MessageParsingException("Unable to parse the message", null)
  }

}
