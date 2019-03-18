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

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

import akka.actor.ActorSystem
//import akka.dispatch.ExecutionContexts
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}


abstract class AmqpSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val system = ActorSystem(this.getClass.getSimpleName)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  //  implicit val executionContext = ExecutionContexts.global()
  //  implicit val executionContext: ExecutionContext = new ExecutionContext {
  //    val threadPool = Executors.newFixedThreadPool(10)
  //
  //    def execute(runnable: Runnable) {
  //      threadPool.submit(runnable)
  //    }
  //
  //    def reportFailure(t: Throwable) {}
  //  }


  override protected def afterAll(): Unit =
    system.terminate()
}