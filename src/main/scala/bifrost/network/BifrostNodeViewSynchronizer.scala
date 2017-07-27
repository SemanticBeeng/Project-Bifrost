package bifrost.network

import akka.actor.{Actor, ActorRef}
import bifrost.BifrostNodeViewHolder
import bifrost.exchange.ExchangeManager
import bifrost.history.{BifrostSyncInfo, BifrostSyncInfoMessageSpec}
import bifrost.scorexMod.GenericNodeViewHolder._
import bifrost.scorexMod.GenericNodeViewSynchronizer.GetLocalSyncInfo
import bifrost.scorexMod.{GenericNodeViewHolder, GenericNodeViewSynchronizer}
import bifrost.transaction.BifrostTransaction
import scorex.core.network.NetworkController.{DataFromPeer, SendToNetwork}
import scorex.core.network._
import scorex.core.network.message.{InvSpec, RequestModifierSpec, _}
import scorex.core.transaction.box.proposition.{ProofOfKnowledgeProposition, Proposition, PublicKey25519Proposition}
import scorex.core.transaction.state.PrivateKey25519
import serializer.{PeerMessage, ProducerProposal}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * A middle layer between a node view holder(NodeViewHolder) and a network
  *
  * @param networkControllerRef
  * @param viewHolderRef
  * @param localInterfaceRef
  * @param syncInfoSpec
  */
class BifrostNodeViewSynchronizer(networkControllerRef: ActorRef,
                                  viewHolderRef: ActorRef,
                                  localInterfaceRef: ActorRef,
                                  syncInfoSpec: BifrostSyncInfoMessageSpec.type,
                                  exchangeRef: ActorRef)
  extends GenericNodeViewSynchronizer[ProofOfKnowledgeProposition[PrivateKey25519],
    BifrostTransaction,
    BifrostSyncInfo,
    BifrostSyncInfoMessageSpec.type
  ](networkControllerRef, viewHolderRef, localInterfaceRef, syncInfoSpec) {

  override def preStart(): Unit = {
    //register as a handler for some types of messages
    val messageSpecs = Seq(InvSpec, RequestModifierSpec, ModifiersSpec, syncInfoSpec, PeerMessageSpec)
    networkControllerRef ! NetworkController.RegisterMessagesHandler(messageSpecs, self)

    //subscribe for failed transaction,
    val events = Seq(
      GenericNodeViewHolder.EventType.FailedTransaction,
      GenericNodeViewHolder.EventType.FailedPersistentModifier,
      GenericNodeViewHolder.EventType.SuccessfulTransaction,
      GenericNodeViewHolder.EventType.SuccessfulPersistentModifier
    )
    viewHolderRef ! Subscribe(events)

    context.system.scheduler.schedule(2.seconds, 15.seconds)(self ! GetLocalSyncInfo)
  }

  private def processPeerMessage: Receive = {
    case DataFromPeer(spec, data: PeerMessage, remote)
      if spec.messageCode == PeerMessageSpec.messageCode =>
      data.messageType match {
        case PeerMessage.Type.ProducerProposal =>
          viewHolderRef ! BifrostNodeViewHolder.PeerMessageReceived(data)
          networkControllerRef ! NetworkController.SendToNetwork(
            Message(PeerMessageSpec, Right(data), None), BroadcastExceptOf(Seq(remote))
          )
        case PeerMessage.Type.BuySellOrder =>
          exchangeRef ! ExchangeManager.PeerMessageReceived(data)
          networkControllerRef ! NetworkController.SendToNetwork(
            Message(PeerMessageSpec, Right(data), None), BroadcastExceptOf(Seq(remote))
          )
      }
  }


  override def receive: Receive = processPeerMessage orElse super.receive
}

