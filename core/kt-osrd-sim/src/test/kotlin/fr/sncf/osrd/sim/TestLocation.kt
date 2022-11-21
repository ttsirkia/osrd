package fr.sncf.osrd.sim


import fr.sncf.osrd.sim.api.Train
import fr.sncf.osrd.sim.api.ZoneOccupation
import fr.sncf.osrd.sim.impl.locationSim
import fr.sncf.osrd.sim_infra.api.*
import fr.sncf.osrd.sim_infra.impl.simInfra
import fr.sncf.osrd.utils.indexing.MutableArena
import fr.sncf.osrd.utils.indexing.dynIdxArraySetOf
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class TestLocation {
    @Test
    fun lockOccupyLeave() = runBlocking {
        // setup test data
        val infra = simInfra {
            // create a test switch
            val switchA = movableElement(delay = 42L.milliseconds) {
                defaultConfig = config("a")
                config("b")
            }

            val zoneA = zone(listOf(switchA))

            val detectorA = detector()
            setNextZone(detectorA.normal, zoneA)
            val detectorB = detector()
            setNextZone(detectorB.reverse, zoneA)
        }

        val sim = locationSim(infra)
        val trainArena = MutableArena<Train>(2)
        val trainA = trainArena.allocate()
        val trainB = trainArena.allocate()
        val zone = infra.zones[0]

        val occupationHistory: MutableList<ZoneOccupation> = mutableListOf()

        val watchJob = launch(Dispatchers.Unconfined) {
            sim.watchZoneOccupation(zone).collect {
                occupationHistory.add(it)
            }
        }

        sim.enterZone(zone, trainB)
        sim.enterZone(zone, trainA)
        sim.leaveZone(zone, trainB)
        sim.leaveZone(zone, trainA)

        val reference: List<ZoneOccupation> = listOf(
            dynIdxArraySetOf(),
            dynIdxArraySetOf(trainB),
            dynIdxArraySetOf(trainB, trainA),
            dynIdxArraySetOf(trainA),
            dynIdxArraySetOf()
        )

        assertEquals(reference, occupationHistory)
        watchJob.cancelAndJoin()
    }
}