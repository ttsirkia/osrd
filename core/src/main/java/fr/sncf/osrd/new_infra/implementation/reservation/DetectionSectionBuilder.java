package fr.sncf.osrd.new_infra.implementation.reservation;

import static fr.sncf.osrd.new_infra.api.Direction.BACKWARD;
import static fr.sncf.osrd.new_infra.api.Direction.FORWARD;
import static fr.sncf.osrd.new_infra.implementation.GraphHelpers.*;

import com.google.common.collect.ImmutableSet;
import fr.sncf.osrd.new_infra.api.Direction;
import fr.sncf.osrd.new_infra.api.reservation.DetectionSection;
import fr.sncf.osrd.new_infra.api.reservation.DiDetector;
import fr.sncf.osrd.new_infra.api.tracks.directed.DiTrackInfra;
import fr.sncf.osrd.new_infra.api.tracks.undirected.TrackEdge;
import fr.sncf.osrd.utils.UnionFind;
import fr.sncf.osrd.utils.graph.EdgeEndpoint;
import java.util.*;

public class DetectionSectionBuilder {

    /** Represents a DetectionSection that isn't complete yet */
    private static class SectionBuilder {
        public final ImmutableSet.Builder<DiDetector> detectors = new ImmutableSet.Builder<>();

        /** Builds the DetectionSection */
        DetectionSection build() {
            return new DetectionSectionImpl(detectors.build());
        }
    }

    private final ArrayList<DetectionSection> detectionSections = new ArrayList<>();
    private final Map<String, DetectorImpl> detectors;
    private final Map<Direction, Map<String, DiDetector>> diDetectors;
    private final Map<DiDetector, DetectionSection> diDetectorToNextSection = new HashMap<>();
    private final DiTrackInfra infra;

    /** Constructor */
    public DetectionSectionBuilder(
            DiTrackInfra infra,
            Map<String, DetectorImpl> detectors,
            Map<Direction, Map<String, DiDetector>> diDetectors
    ) {
        this.infra = infra;
        this.detectors = detectors;
        this.diDetectors = diDetectors;
    }

    /**
     * Build all detection sections and link them to waypoints
     */
    public static ArrayList<DetectionSection> build(DiTrackInfra infra) {
        var detectorMaps = DetectorMaps.from(infra);
        return new DetectionSectionBuilder(infra, detectorMaps.detectorMap, detectorMaps.diDetectorMap).build();
    }

    /**
     * Build all detection sections and link them to waypoints, with detectors already instantiated
     */
    public static ArrayList<DetectionSection> build(
            DiTrackInfra infra,
            Map<String, DetectorImpl> detectors,
            Map<Direction, Map<String, DiDetector>> diDetectors
    ) {
        return new DetectionSectionBuilder(infra, detectors, diDetectors).build();
    }


    /** Builds all the results */
    private ArrayList<DetectionSection> build() {
        createSectionsInsideTracks();
        createSectionsOverSeveralTracks();
        return buildResult();
    }

    /** Creates all the sections that are inside a single track */
    private void createSectionsInsideTracks() {
        // Create detection section inside a track section
        for (var track : infra.getTrackGraph().edges()) {
            var waypoints = track.getTrackObjects();
            for (int i = 1; i < waypoints.size(); i++) {
                var prev = waypoints.get(i - 1);
                var cur = waypoints.get(i);
                var detectionSection = new DetectionSectionImpl(ImmutableSet.of(
                        diDetectors.get(FORWARD).get(prev.getID()),
                        diDetectors.get(BACKWARD).get(cur.getID())
                ));
                detectionSections.add(detectionSection);
                setNextSection(prev.getID(), FORWARD, detectionSection);
                setNextSection(cur.getID(), BACKWARD, detectionSection);
            }
        }
    }

    /** Create detection sections which cross track section boundaries */
    private void createSectionsOverSeveralTracks() {
        // Keep track of what detection section each endpoint belongs to
        var uf = new UnionFind(infra.getTrackGraph().edges().size() * 2);

        for (var track : infra.getTrackGraph().edges()) {
            var beginIndex = getEndpointIndex(track, EdgeEndpoint.BEGIN);
            var endIndex = getEndpointIndex(track, EdgeEndpoint.END);
            if (track.getTrackObjects().size() == 0)
                uf.union(beginIndex, endIndex);

            for (var neighbor : infra.getTrackGraph().adjacentEdges(track)) {
                assert neighbor != track;
                var commonNode = getCommonNode(infra.getTrackGraph(), neighbor, track);
                var neighborDir = getDirectionFromEndpoint(infra.getTrackGraph(), neighbor, commonNode);
                var linkIndex = beginIndex;
                if (getDirectionFromEndpoint(infra.getTrackGraph(), track, commonNode) == BACKWARD)
                    linkIndex = endIndex;
                uf.union(linkIndex, getEndpointIndex(neighbor, EdgeEndpoint.startEndpoint(neighborDir)));
            }
        }

        var detectionSectionsMap = new HashMap<Integer, SectionBuilder>();
        for (var track : infra.getTrackGraph().edges()) {
            var waypoints = track.getTrackObjects();
            if (waypoints.size() == 0)
                continue;

            var beginGroupIndex = uf.findRoot(getEndpointIndex(track, EdgeEndpoint.BEGIN));
            var startDetectionSection = detectionSectionsMap.computeIfAbsent(beginGroupIndex,
                    (x) -> new SectionBuilder());

            var firstWaypoint = waypoints.get(0);

            var endGroupIndex = uf.findRoot(getEndpointIndex(track, EdgeEndpoint.END));
            var endDetectionSection = detectionSectionsMap.computeIfAbsent(endGroupIndex,
                    (x) -> new SectionBuilder());
            var lastWaypoint = waypoints.get(waypoints.size() - 1);

            startDetectionSection.detectors.add(diDetectors.get(BACKWARD).get(firstWaypoint.getID()));
            endDetectionSection.detectors.add(diDetectors.get(FORWARD).get(lastWaypoint.getID()));
        }

        for (var builder : detectionSectionsMap.values()) {
            var section = builder.build();
            detectionSections.add(section);
            for (var diDetector : section.getDetectors()) {
                setNextSection(diDetector.getDetector().getID(), diDetector.getDirection(), section);
            }
        }
    }

    /** Drop the empty sections, build the result, and link everything together */
    private ArrayList<DetectionSection> buildResult() {
        var res = new ArrayList<DetectionSection>();
        for (var section : detectionSections) {
            assert section.getDetectors().size() != 0;
            if (section.getDetectors().size() == 1) {
                // filter out detection sections with a single waypoint
                // (those are the tiny space behind buffer stops)
                deleteSection(section);
            } else {
                // make a new array for kept sections, and allocate indexes
                res.add(section);
            }
        }
        setNextSections();
        return res;
    }

    /** Registers all the detection sections in the detectors */
    private void setNextSections() {
        for (var entry : diDetectorToNextSection.entrySet()) {
            if (entry.getValue() == null)
                continue;
            var detector = detectors.get(entry.getKey().getDetector().getID());
            detector.setDetectionSection(entry.getKey().getDirection(), entry.getValue());
        }
    }

    /** Sets the next section for the given directed detector */
    private void setNextSection(
            String detectorID,
            Direction dir,
            DetectionSection section
    ) {
        var diDetector = diDetectors.get(dir).get(detectorID);
        diDetectorToNextSection.put(diDetector, section);
    }

    /** Removes a detection section */
    private void deleteSection(DetectionSection section) {
        for (var detector : section.getDetectors()) {
            for (var dir : Direction.values()) {
                if (detector.getDetector().getNextDetectionSection(dir) == section) {
                    setNextSection(detector.getDetector().getID(), dir, null);
                }
            }
        }
    }

    /** Returns the index of a track + endpoint as a unique integer, used in the union find */
    private static int getEndpointIndex(TrackEdge track, EdgeEndpoint endpoint) {
        return track.getIndex() * 2 + endpoint.id;
    }
}