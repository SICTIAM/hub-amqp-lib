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

import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, PoisonPill, Props}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import fr.sictiam.amqp.api.AmqpGenericRpcServer
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicServer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-01
  */

object Scheduler extends App {

  implicit val system = ActorSystem("Scheduler-" + System.currentTimeMillis())
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(10)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

  val ticker = system.actorOf(Props[Ticker], "ticker")

  val server: AmqpGenericRpcServer = new AmqpRpcTopicServer("testExchange", "rpcNode1Test") {

    override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
      println(s"Message received: ${msg.bytes.utf8String}")
      Future(OutgoingMessage(ByteString("OK"), true, true))(ec)
    }

    override def onReply(msg: ByteString): Unit = {
      println(s"Reply received: ${msg.utf8String}")
    }
  }

  var cancellable: Cancellable = system.scheduler.schedule(0 milliseconds, 500 milliseconds) {
    ticker ! Consume
  }

  case object Consume

  case object Quit

  class Ticker extends Actor with ActorLogging {
    override def receive: Receive = {
      case Consume ⇒
      //        server.consume(1)
      case Quit ⇒
        println("received unknown message")
        self ! PoisonPill
      case _ ⇒ println("received unknown message")
    }
  }

  //  This cancels further Ticks to be sent
  sys addShutdownHook (cancellable.cancel())
}
