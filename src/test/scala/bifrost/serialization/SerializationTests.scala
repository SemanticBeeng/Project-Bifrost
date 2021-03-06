package bifrost.serialization

import bifrost.blocks.{BifrostBlock, BifrostBlockCompanion}
import bifrost.contract.Agreement
import bifrost.history.{BifrostSyncInfo, BifrostSyncInfoSerializer}
import bifrost.transaction._
import bifrost.transaction.box.proposition.{MofNProposition, MofNPropositionSerializer}
import bifrost.transaction.box._
import bifrost.{BifrostGenerators, ValidGenerators}
import io.circe.{Decoder, HCursor}
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519
import scorex.crypto.encode.Base58
import io.circe.parser.decode

/**
  * Created by cykoz on 4/12/17.
  */
class SerializationTests extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with BifrostGenerators
  with ValidGenerators {

  property("oneOfNProposition Serialization") {
    forAll(oneOfNPropositionGen) {
      case (_, mn: MofNProposition) =>
        val parsed = MofNPropositionSerializer.parseBytes(MofNPropositionSerializer.toBytes(mn)).get
        parsed.m shouldBe mn.m
        parsed.setOfPubKeyBytes should contain theSameElementsAs mn.setOfPubKeyBytes
    }
  }

  property("ContractBox Serialization") {
    forAll(contractBoxGen) {
      b: ContractBox =>
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        parsed.json.noSpaces shouldBe b.json.noSpaces
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("PolyBox Serialization") {
    forAll(polyBoxGen) {
      b: PolyBox =>
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("ArbitBox Serialization") {
    forAll(arbitBoxGen) {
      b: ArbitBox =>
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("AssetBox Serialization") {
    forAll(assetBoxGen) {
      b: AssetBox =>
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("Reputation Serialization") {
    forAll(reputationBoxGen) {
      b: ReputationBox =>
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("ProfileBox Serialization") {
    forAll(profileBoxGen) {
      b: ProfileBox =>
        val json = b.json
        val parsed = BifrostBoxSerializer.parseBytes(BifrostBoxSerializer.toBytes(b)).get
        val serialized = BifrostBoxSerializer.toBytes(parsed)
        json.as[ProfileBox].right.get.bytes sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
        serialized sameElements BifrostBoxSerializer.toBytes(b) shouldBe true
    }
  }

  property("Agreement Serialization") {
    forAll(validAgreementGen) {
      a: Agreement =>
        val parsed = AgreementCompanion.parseBytes(AgreementCompanion.toBytes(a)).get
        AgreementCompanion.toBytes(parsed) sameElements AgreementCompanion.toBytes(a) shouldBe true
    }
  }

  property("PolyTransfer Serialization") {
    forAll(polyTransferGen) {
      sc: PolyTransfer =>
        val parsed = TransferTransactionCompanion.parseBytes(
          TransferTransactionCompanion.toBytes(sc)
        ).get
        TransferTransactionCompanion.toBytes(parsed) sameElements TransferTransactionCompanion.toBytes(sc) shouldBe true
    }
  }

  property("ArbitTransfer Serialization") {
    forAll(arbitTransferGen) {
      ac: ArbitTransfer =>
        val parsed = TransferTransactionCompanion.parseBytes(
          TransferTransactionCompanion.toBytes(ac)
        ).get
        TransferTransactionCompanion.toBytes(parsed) sameElements TransferTransactionCompanion.toBytes(ac) shouldBe true
    }
  }

  property("AssetTransfer Serialization") {
    forAll(assetTransferGen) {
      at: AssetTransfer =>
        val parsed = TransferTransactionCompanion.parseBytes(
          TransferTransactionCompanion.toBytes(at)
        ).get
        TransferTransactionCompanion.toBytes(parsed) sameElements TransferTransactionCompanion.toBytes(at) shouldBe true
    }
  }

  property("ContractCreation Serialization") {
    forAll(contractCreationGen) {
      c: ContractCreation =>
        val parsed = ContractTransactionCompanion.parseBytes(
          ContractTransactionCompanion.toBytes(c)
        ).get

        val parsedBytes = ContractTransactionCompanion.toBytes(parsed)
        val directParsedBytes = ContractTransactionCompanion.toBytes(c)

        parsedBytes sameElements directParsedBytes shouldBe true
    }
  }

  property("ContractMethodExecution Serialization") {
    forAll(contractMethodExecutionGen) {
      c: ContractMethodExecution =>
        val parsed = ContractTransactionCompanion.parseBytes(
          ContractTransactionCompanion.toBytes(c)
        ).get
        ContractTransactionCompanion.toBytes(parsed) sameElements ContractTransactionCompanion.toBytes(c) shouldBe true
    }
  }

  property("ContractCompletion Serialization") {
    forAll(contractCompletionGen) {
      c: ContractCompletion =>
        val parsed = ContractTransactionCompanion.parseBytes(
          ContractTransactionCompanion.toBytes(c)
        ).get
        ContractTransactionCompanion.toBytes(parsed) sameElements ContractTransactionCompanion.toBytes(c) shouldBe true
    }
  }

  property("ProfileTransaction Serialization") {
    forAll(profileTxGen) {
      p: ProfileTransaction =>
        val parsed = ProfileTransactionCompanion.parseBytes(
            ProfileTransactionCompanion.toBytes(p)
        ).get
        ProfileTransactionCompanion.toBytes(parsed) sameElements ProfileTransactionCompanion.toBytes(p) shouldBe true
    }
  }

  property("AssetRedemption Serialization") {
    forAll(assetRedemptionGen) {
      ar: AssetRedemption =>
        val parsed = AssetRedemptionCompanion.parseBytes(
          AssetRedemptionCompanion.toBytes(ar)
        ).get
        AssetRedemptionCompanion.toBytes(parsed) sameElements AssetRedemptionCompanion.toBytes(ar) shouldBe true
    }
  }

  property("BifrostBlock Serialization") {
    forAll(bifrostBlockGen) {
      bb: BifrostBlock =>
        val parsed = BifrostBlockCompanion.parseBytes(BifrostBlockCompanion.toBytes(bb)).get
        BifrostBlockCompanion.toBytes(parsed) sameElements BifrostBlockCompanion.toBytes(bb) shouldBe true
    }
  }

  property("BifrostSyncInfo Serialization") {
    forAll(bifrostSyncInfoGen) {
      si: BifrostSyncInfo =>
        val parsed = BifrostSyncInfoSerializer.parseBytes(BifrostSyncInfoSerializer.toBytes(si)).get
        BifrostSyncInfoSerializer.toBytes(parsed) sameElements BifrostSyncInfoSerializer.toBytes(si) shouldBe true
    }
  }

}
