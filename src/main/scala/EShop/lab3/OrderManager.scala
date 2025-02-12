package EShop.lab3

import EShop.lab2._
import EShop.lab2.CartActor.CheckoutStarted
import EShop.lab3.Payment.DoPayment
import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

object OrderManager {

  sealed trait State
  case object Uninitialized extends State
  case object Open          extends State
  case object InCheckout    extends State
  case object InPayment     extends State
  case object Finished      extends State

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK

  sealed trait Data
  case object Empty                                                            extends Data
  case class CartData(cartRef: ActorRef)                                       extends Data
  case class CartDataWithSender(cartRef: ActorRef, sender: ActorRef)           extends Data
  case class InCheckoutData(checkoutRef: ActorRef)                             extends Data
  case class InCheckoutDataWithSender(checkoutRef: ActorRef, sender: ActorRef) extends Data
  case class InPaymentData(paymentRef: ActorRef)                               extends Data
  case class InPaymentDataWithSender(paymentRef: ActorRef, sender: ActorRef)   extends Data

  def props = Props(new OrderManager)
}

class OrderManager extends Actor {

  import OrderManager._

  override def receive: Receive = LoggingReceive {
    uninitialized
  }

  def uninitialized: Receive = LoggingReceive {
    case AddItem(item) =>
      val cart: ActorRef = context.actorOf(CartActor.props(), "cart")
      cart ! CartActor.AddItem(item)
      sender ! Done
      context become open(cart)
  }

  def open(cartActor: ActorRef): Receive = LoggingReceive {
    case AddItem(item) =>
      cartActor ! CartActor.AddItem(item)
      sender ! Done
      context become open(cartActor)
    case RemoveItem(item) =>
      cartActor ! CartActor.RemoveItem(item)
      sender ! Done
      context become open(cartActor)
    case Buy =>
      cartActor ! CartActor.StartCheckout
      context become inCheckout(cartActor, sender)
  }

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive {
    case CheckoutStarted(checkoutRef, _) =>
      senderRef ! Done
      context become inCheckout(checkoutRef)
  }

  def inCheckout(checkoutActorRef: ActorRef): Receive = LoggingReceive {
    case SelectDeliveryAndPaymentMethod(delivery, payment) =>
      checkoutActorRef ! Checkout.SelectDeliveryMethod(delivery)
      checkoutActorRef ! Checkout.SelectPayment(payment)
      context become inPayment(sender)
  }

  def inPayment(senderRef: ActorRef): Receive = LoggingReceive {
    case Checkout.PaymentStarted(paymentRef) =>
      senderRef ! Done
      context become inPayment(paymentRef, senderRef)
  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive {
    case Pay =>
      senderRef ! Done
      paymentActorRef ! DoPayment
      context become inPayment(paymentActorRef, sender)
    case Payment.PaymentConfirmed =>
      senderRef ! Done
      context become finished
  }

  def finished: Receive = LoggingReceive {
    case _ => sender ! "order manager finished job"
  }
}
