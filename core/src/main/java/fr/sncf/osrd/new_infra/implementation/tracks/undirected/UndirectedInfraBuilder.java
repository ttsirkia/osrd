package fr.sncf.osrd.new_infra.implementation.tracks.undirected;

import static fr.sncf.osrd.new_infra.api.tracks.undirected.TrackObject.TrackObjectType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.NetworkBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.new_infra.api.tracks.undirected.*;
import fr.sncf.osrd.new_infra.implementation.RJSObjectParsing;
import fr.sncf.osrd.railjson.schema.infra.RJSInfra;
import fr.sncf.osrd.railjson.schema.infra.RJSSwitch;
import fr.sncf.osrd.railjson.schema.infra.RJSSwitchType;
import fr.sncf.osrd.railjson.schema.infra.RJSTrackSection;
import fr.sncf.osrd.railjson.schema.infra.trackobjects.RJSRouteWaypoint;
import fr.sncf.osrd.utils.graph.EdgeEndpoint;
import java.util.*;

@SuppressFBWarnings({"NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
public class UndirectedInfraBuilder {

    private final HashMap<String, TrackNode> beginEndpoints = new HashMap<>();
    private final HashMap<String, TrackNode> endEndpoints = new HashMap<>();
    private final HashMap<TrackSectionImpl, ArrayList<TrackObject>> objectLists = new HashMap<>();
    private final ImmutableNetwork.Builder<TrackNode, TrackEdge> builder;

    /** Constructor */
    private UndirectedInfraBuilder() {
        builder = NetworkBuilder
                .directed()
                .immutable();
    }

    /** Creates a TrackInfra from a railjson infra */
    public static TrackInfra parseInfra(RJSInfra infra) {
        return new UndirectedInfraBuilder().parse(infra);
    }

    /** Parse the railjson to build an infra */
    private TrackInfra parse(RJSInfra infra) {
        // Creates switches
        var switchTypeMap = new HashMap<String, RJSSwitchType>();
        for (var rjsSwitchType : infra.switchTypes)
            switchTypeMap.put(rjsSwitchType.id, rjsSwitchType);
        var switches = new ImmutableMap.Builder<String, Switch>();
        for (var s : infra.switches) {
            switches.put(s.id, parseSwitch(s, switchTypeMap));
        }

        // Create remaining links
        for (var link : infra.trackSectionLinks) {
            var newNode = new TrackNodeImpl.Joint();
            addNode(link.src.track.id.id, link.src.endpoint, newNode);
            addNode(link.dst.track.id.id, link.dst.endpoint, newNode);
        }

        var trackSectionsByID = new HashMap<String, TrackSectionImpl>();
        for (var track : infra.trackSections) {
            var newTrack = makeTrackSection(track);
            trackSectionsByID.put(newTrack.getID(), newTrack);
            objectLists.put(newTrack, new ArrayList<>());
        }

        for (var detector : infra.detectors) {
            makeWaypoint(trackSectionsByID, detector, TrackObjectType.DETECTOR);
        }
        for (var bufferStop : infra.bufferStops) {
            makeWaypoint(trackSectionsByID, bufferStop, TrackObjectType.BUFFER_STOP);
        }

        for (var entry : objectLists.entrySet()) {
            var track = entry.getKey();
            var objects = entry.getValue();
            objects.sort(Comparator.comparingDouble(TrackObject::getOffset));
            track.trackObjects = ImmutableList.copyOf(objects);
        }

        return TrackInfraImpl.from(switches.build(), builder.build());
    }

    /** Creates a waypoint and add it to the corresponding track */
    private void makeWaypoint(HashMap<String, TrackSectionImpl> trackSectionsByID,
                            RJSRouteWaypoint waypoint, TrackObjectType trackObjectType) {
        var track = RJSObjectParsing.getTrackSection(waypoint.track, trackSectionsByID);
        var newWaypoint = new TrackObjectImpl(track, waypoint.position, trackObjectType, waypoint.id);
        objectLists.get(track).add(newWaypoint);
    }

    /** Creates a track section and registers it in the graph */
    private TrackSectionImpl makeTrackSection(RJSTrackSection track) {
        var begin = getNode(track.id, EdgeEndpoint.BEGIN);
        var end = getNode(track.id, EdgeEndpoint.END);
        var edge = new TrackSectionImpl(track.length, track.id);
        builder.addEdge(begin, end, edge);
        return edge;
    }

    /** Creates a node and registers it in the graph */
    private void addNode(String trackId, EdgeEndpoint endpoint, TrackNode node) {
        var map = beginEndpoints;
        if (endpoint == EdgeEndpoint.END)
            map = endEndpoints;
        if (map.containsKey(trackId)) {
            if (map.get(trackId) instanceof SwitchPort) // the link is already created in a switch, ignore
                return;
            throw new InvalidInfraException("Duplicated track link"); // TODO: add details
        }
        map.put(trackId, node);
        builder.addNode(node);
    }

    /** Returns the node at the given end of the track */
    private TrackNode getNode(String trackId, EdgeEndpoint endpoint) {
        var map = beginEndpoints;
        if (endpoint == EdgeEndpoint.END)
            map = endEndpoints;
        if (map.containsKey(trackId))
            return map.get(trackId);
        var res = new TrackNodeImpl.End();
        addNode(trackId, endpoint, res);
        return res;
    }

    /** Parse a given RJS switch */
    private Switch parseSwitch(
            RJSSwitch rjsSwitch,
            HashMap<String, RJSSwitchType> switchTypeMap
    ) throws InvalidInfraException {
        var networkBuilder = NetworkBuilder.directed()
                .<SwitchPort, SwitchBranch>immutable();
        var portMap = ImmutableMap.<String, SwitchPort>builder();
        var allPorts = new HashSet<SwitchPortImpl>();
        for (var entry : rjsSwitch.ports.entrySet()) {
            var portName = entry.getKey();
            var port = entry.getValue();
            var newNode = new SwitchPortImpl(portName);
            portMap.put(portName, newNode);
            networkBuilder.addNode(newNode);
            addNode(port.track.id.id, port.endpoint, newNode);
            allPorts.add(newNode);
        }
        var finalPortMap = portMap.build();
        var switchType = rjsSwitch.switchType.getSwitchType(switchTypeMap);
        var groups = ImmutableMultimap.<String, SwitchBranch>builder();
        var allBranches = new HashSet<SwitchBranchImpl>();
        for (var entry : switchType.groups.entrySet()) {
            for (var e : entry.getValue()) {
                var src = finalPortMap.get(e.src);
                var dst = finalPortMap.get(e.dst);
                var branch = new SwitchBranchImpl();
                groups.put(entry.getKey(), branch);
                assert src != null;
                assert dst != null;
                networkBuilder.addEdge(src, dst, branch);
                builder.addEdge(src, dst, branch);
                allBranches.add(branch);
            }
        }
        var switchTypePorts = new HashSet<>(switchType.ports);
        var switchPorts = rjsSwitch.ports.keySet();
        if (!switchTypePorts.equals(switchPorts))
            throw new InvalidInfraException(String.format(
                    "Switch %s doesn't have the right ports for type %s (expected %s, got %s)",
                    rjsSwitch.id, switchType.id, switchTypePorts, switchPorts
            ));

        var res = new SwitchImpl(rjsSwitch.id, networkBuilder.build(), groups.build(), finalPortMap);
        for (var branch : allBranches)
            branch.switchRef = res;
        for (var port : allPorts)
            port.switchRef = res;
        return res;
    }
}