package bifrost.history

import java.io.File

import bifrost.BifrostGenerators
import bifrost.blocks.BifrostBlock
import bifrost.transaction.BifrostTransaction
import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}
import scorex.core.NodeViewModifier._
import scorex.crypto.encode.Base58

import scala.util.Random

class IODBSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with BifrostGenerators {


  val iFile = new File(s"/tmp/scorex/scorextest-${Random.nextInt(10000000)}")
  iFile.mkdirs()
  val blocksStorage = new LSMStore(iFile)

  property("Rollback should not touch keys before") {

    /**
      * Apply a transaction by storing its new boxes (ignore old boxes)
      * @param tx   the transaction to write boxes to storage
      */
    def writeTx(tx: BifrostTransaction): Unit = {
      val boxIdsToRemove: Iterable[ByteArrayWrapper] = Seq()
      val boxesToAdd: Iterable[(ByteArrayWrapper, ByteArrayWrapper)] = tx.newBoxes.map(b => (ByteArrayWrapper(b.id), ByteArrayWrapper(b.bytes))).toList
      blocksStorage.update(ByteArrayWrapper(tx.id), boxIdsToRemove, boxesToAdd)
    }

    /**
      * Check that the boxes for the transaction are all stored
      * @param tx   the transaction to check has boxes in storage
      */
    def checkTx(tx: BifrostTransaction): Unit = {
      tx.newBoxes.foreach(b => require(blocksStorage.get(ByteArrayWrapper(b.id)).isDefined))
    }

    forAll(bifrostTransactionSeqGen) { txs =>
      whenever(txs.length >= 2) {

        /* Make sure transactions get written to storage */
        txs.foreach(tx => { writeTx(tx); checkTx(tx) })

        val head = txs.head

        /* Rollback to head shouldn't affect the head tx */
        blocksStorage.rollback(ByteArrayWrapper(head.id))
        checkTx(head)
      }
    }
  }

  property("Writing a block should result in storage of block") {

    /**
      * Apply a block by storing all of its transactions' new boxes (ignore old boxes)
      * @param b    the block to write tx boxes to storage
      */
    def writeBlock(b: BifrostBlock): Unit = {
      blocksStorage.update(
        ByteArrayWrapper(b.id),
        Seq(),
        Seq(ByteArrayWrapper(b.id) -> ByteArrayWrapper(BifrostBlock.ModifierTypeId +: b.bytes))
      )
    }

    var ids: Seq[ModifierId] = Seq()

    forAll(bifrostBlockGen) { block =>
      ids = block.id +: ids
      writeBlock(block)
      blocksStorage.get(ByteArrayWrapper(block.id)).isDefined shouldBe true
    }

    ids.foreach { id => {
        require(blocksStorage.get(ByteArrayWrapper(id)) match {
          case None => println(s"${Console.RED} Id ${Base58.encode(id)} not found"); false
          case Some(_) => true
        })
      }
    }
  }

}
