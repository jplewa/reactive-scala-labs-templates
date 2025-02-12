package EShop.lab5

import EShop.lab3.Payment.{DoPayment, PaymentConfirmed}
import EShop.lab5.Payment.PaymentRestarted
import PaymentServiceServer.PaymentServiceServer
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentTest
  extends TestKit(ActorSystem("PaymentTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "properly confirm payment after 2 retries using payu payment method" in {
    val manager  = TestProbe()
    val checkout = TestProbe()
    val payment  = TestActorRef(Payment.props("payu", manager.ref, checkout.ref))

    val server = new PaymentServiceServer()
    Future { server.run() }

    payment ! DoPayment

    manager.expectMsg(PaymentRestarted)
    manager.expectMsg(PaymentRestarted)
    manager.expectMsg(PaymentConfirmed)
    server.system.terminate()
  }
}
