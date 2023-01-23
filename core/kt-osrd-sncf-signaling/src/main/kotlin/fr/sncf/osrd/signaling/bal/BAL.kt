package fr.sncf.osrd.signaling.bal

import fr.sncf.osrd.signaling.DiagnosisReporter
import fr.sncf.osrd.signaling.SigBlock
import fr.sncf.osrd.signaling.SignalingSystemDriver
import fr.sncf.osrd.sim_infra.api.SigSettingsSchema
import fr.sncf.osrd.sim_infra.api.SigStateSchema

object BAL : SignalingSystemDriver {
    override val id = "BAL"
    override val stateSchema = SigStateSchema {
        enum("aspect", listOf("VL", "A", "S", "C"))
    }
    override val settingsSchema = SigSettingsSchema {
        flag("Nf")
    }
    override val isBlockDelimiterExpr = "true"

    override fun checkBlock(reporter: DiagnosisReporter, block: SigBlock) {
        // Check that we have the correct number of signals
        if (block.startsAtBufferStop || block.stopsAtBufferStop) {
           assert(block.signalTypes.size == 1)
        }
        else {
           assert(block.signalTypes.size == 2)
        }
    }
}
