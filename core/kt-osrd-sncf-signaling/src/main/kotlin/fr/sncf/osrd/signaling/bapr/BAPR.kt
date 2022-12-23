package fr.sncf.osrd.signaling.bapr
import fr.sncf.osrd.signaling.DiagnosisReporter
import fr.sncf.osrd.signaling.SigBlock
import fr.sncf.osrd.signaling.SignalingSystemDriver
import fr.sncf.osrd.sim_infra.api.SigSettingsSchema
import fr.sncf.osrd.sim_infra.api.SigStateSchema

object BAPR : SignalingSystemDriver {
    override val id = "BAPR"
    override val stateSchema = SigStateSchema {
        enum("aspect", listOf("VL", "A", "S", "C"))
    }
    override val settingsSchema = SigSettingsSchema {
        flag("Nf")
        flag("distant")
    }
    override val isBlockDelimiterExpr = "!distant"

    override fun checkBlock(reporter: DiagnosisReporter, block: SigBlock) {
        // Check that we have the correct number of signals
        if (block.startsAtBufferStop || block.stopsAtBufferStop) {
            assert(block.signalTypes.size == 2)
        }
        else {
            assert(block.signalTypes.size == 3)
        }

        // Check the signal types and attributes
        if (block.startsAtBufferStop) {
            assert(block.signalSettings[0].getFlag("distant"))
        }
        else {
            assert(!block.signalSettings[0].getFlag("distant"))

            assert(block.signalTypes[1] == "BAPR")
            assert(block.signalSettings[1].getFlag("distant"))
        }
    }
}
