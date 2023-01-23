package fr.sncf.osrd.sim_infra.api

import fr.sncf.osrd.utils.indexing.StaticIdx
import fr.sncf.osrd.utils.indexing.StaticIdxList
import fr.sncf.osrd.utils.indexing.StaticIdxSpace
import fr.sncf.osrd.utils.indexing.mutableStaticIdxArrayListOf


/** A type of signaling system, which is used both for blocks and signals */
sealed interface SignalingSystem
typealias SignalingSystemId = StaticIdx<SignalingSystem>

sealed interface SignalDriver
typealias SignalDriverId = StaticIdx<SignalDriver>


sealed interface SignalSettingsMarker
typealias SigSettings = SigData<SignalSettingsMarker>
typealias SigSettingsSchema = SigSchema<SignalSettingsMarker>

sealed interface SignalStateMarker
typealias SigState = SigData<SignalStateMarker>
typealias SigStateSchema = SigSchema<SignalStateMarker>


/** The signaling system manager is a repository for drivers and signaling systems */
interface InfraSigSystemManager {
    val signalingSystems: StaticIdxSpace<SignalingSystem>
    fun findSignalingSystem(sigSystem: String): SignalingSystemId
    fun getStateSchema(sigSystem: SignalingSystemId): SigStateSchema
    fun getSettingsSchema(sigSystem: SignalingSystemId): SigSettingsSchema

    val drivers: StaticIdxSpace<SignalDriver>
    fun findDriver(outputSig: SignalingSystemId, inputSig: SignalingSystemId): SignalDriverId
    fun getInputSignalingSystem(driver: SignalDriverId): SignalingSystemId
    fun getOutputSignalingSystem(driver: SignalDriverId): SignalingSystemId
    fun isBlockDelimiter(sigSystem: SignalingSystemId, settings: SigSettings): Boolean
}


interface LoadedSignalInfra {
    val physicalSignals: StaticIdxSpace<PhysicalSignal>
    val logicalSignals: StaticIdxSpace<LogicalSignal>

    fun getLogicalSignals(signal: PhysicalSignalId): StaticIdxList<LogicalSignal>
    fun getPhysicalSignal(signal: LogicalSignalId): PhysicalSignalId

    fun getSignalingSystem(signal: LogicalSignalId): SignalingSystemId
    fun getSettings(signal: LogicalSignalId): SigSettings
    fun getDrivers(signal: LogicalSignalId): StaticIdxList<SignalDriver>
    fun isBlockDelimiter(signal: LogicalSignalId): Boolean
}

interface BlockInfra {
    val blocks: StaticIdxSpace<Block>
    fun getBlockPath(block: BlockId): StaticIdxList<ZonePath>
    fun getBlockSignals(block: BlockId): StaticIdxList<LogicalSignal>
    fun blockStartAtBufferStop(block: BlockId): Boolean
    fun blockStopAtBufferStop(block: BlockId): Boolean

    fun getBlockSignalingSystem(block: BlockId): SignalingSystemId
    fun getBlocksAt(detector: DirDetectorId): StaticIdxList<Block>
    fun getSignalsPositions(block: BlockId): DistanceList
}

data class BlockPathElements(
    val prev: BlockPathElements?,
    val block: BlockId
)

fun BlockPathElements.toList() : StaticIdxList<Block> {
    val res = mutableStaticIdxArrayListOf(this.block)
    var cur = this.prev
    while (cur != null) {
        res.add(cur.block)
        cur = cur.prev
    }
    return res.reversed()
}

fun getRouteBlocks(
    signalingInfra: RawSignalingInfra,
    blockInfra: BlockInfra,
    route: StaticIdxList<Route>,
    allowedSignalingSystems: StaticIdxList<SignalingSystem>
): List<StaticIdxList<Block>> {
    val path = route.flatMap { signalingInfra.getRoutePath(it) }
    val initialBlocks = getBlocksAtPoint(signalingInfra, blockInfra, path.first(), allowedSignalingSystems)
    var currentOpenPaths = initialBlocks.map { BlockPathElements(null, it) }
    for (i in 1 until path.size) {
        val currentZonePath = path[i]
        val newOpenPath = mutableListOf<BlockPathElements>();
        for (openPath in currentOpenPaths) {
            val block = openPath.block
            if (blockInfra.getBlockPath(block).contains(currentZonePath)) {
                newOpenPath.add(openPath)
                continue;
            }
            val signal = blockInfra.getBlockSignals(block).last()
            val blocksAtPoint = getBlocksAtPoint(signalingInfra, blockInfra, currentZonePath, allowedSignalingSystems)
            val nextElements = blocksAtPoint
                .filter { blockInfra.getBlockSignals(it).first() == signal }
                .map { BlockPathElements(openPath, it) }
            newOpenPath.addAll(nextElements)
        }
        currentOpenPaths = newOpenPath;
    }
    return currentOpenPaths.map { it.toList() }
}

private fun getBlocksAtPoint(
    signalingInfra: RawSignalingInfra,
    blockInfra: BlockInfra,
    currentZonePath: StaticIdx<ZonePath>,
    allowedSignalingSystems: StaticIdxList<SignalingSystem>
): List<StaticIdx<Block>> {
    val currentDet = signalingInfra.getZonePathEntry(currentZonePath)
    return blockInfra.getBlocksAt(currentDet)
        .filter { blockInfra.getBlockPath(it).first() == currentZonePath }
        .filter { allowedSignalingSystems.contains(blockInfra.getBlockSignalingSystem(it)) }
}
