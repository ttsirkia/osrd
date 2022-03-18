package fr.sncf.osrd.new_infra_state.implementation;

import com.google.common.collect.ImmutableList;
import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.new_infra.api.reservation.DetectionSection;
import fr.sncf.osrd.new_infra.api.reservation.DiDetector;
import fr.sncf.osrd.new_infra.api.signaling.SignalingInfra;
import fr.sncf.osrd.new_infra.api.signaling.SignalingRoute;
import fr.sncf.osrd.new_infra.api.tracks.undirected.TrackLocation;
import fr.sncf.osrd.new_infra.implementation.tracks.directed.TrackRangeView;
import fr.sncf.osrd.new_infra_state.api.NewTrainPath;
import fr.sncf.osrd.railjson.parser.exceptions.InvalidSchedule;
import fr.sncf.osrd.railjson.schema.schedule.RJSTrainPath;
import java.util.ArrayList;
import java.util.List;

public class TrainPathBuilder {

    /** Build Train Path from routes, a starting and ending location */
    public static NewTrainPath from(
            List<SignalingRoute> routePath,
            TrackLocation startLocation,
            TrackLocation endLocation
    ) throws InvalidSchedule {
        ImmutableList<NewTrainPath.LocatedElement<TrackRangeView>> trackSectionPath;
        try {
            trackSectionPath = createTrackRangePath(routePath, startLocation, endLocation);
        } catch (RuntimeException e) {
            throw new InvalidSchedule(e.getMessage());
        }
        var detectors = createDetectorPath(trackSectionPath);
        var length = trackSectionPath.stream()
                .mapToDouble(x -> x.element().getLength())
                .sum();
        var locatedRoutePath = makeLocatedRoutePath(routePath, startLocation);
        var trainPath = new NewTrainPath(
                locatedRoutePath,
                trackSectionPath,
                detectors,
                makeDetectionSections(locatedRoutePath, length),
                length
        );
        validate(trainPath);
        return trainPath;
    }

    /** Build Train Path from an RailJSON train path */
    public static NewTrainPath from(SignalingInfra infra, RJSTrainPath rjsTrainPath) throws InvalidSchedule {
        try {
            var routePath = new ArrayList<SignalingRoute>();
            for (var rjsRoutePath : rjsTrainPath.routePath) {
                var infraRoute = infra.getReservationRouteMap().get(rjsRoutePath.route.id.id);
                if (infraRoute == null)
                    throw new InvalidSchedule(String.format("Can't find route %s", rjsRoutePath.route.id.id));
                var signalingRoutes = infra.getRouteMap().get(infraRoute);
                // TODO: add an enum to determine the signalization type
                routePath.add(signalingRoutes.stream().findFirst().orElseThrow());
            }

            var rjsStartTrackRange = rjsTrainPath.routePath.get(0).trackSections.get(0);
            var startLocation = new TrackLocation(
                    infra.getTrackSection(rjsStartTrackRange.track.id.id),
                    rjsStartTrackRange.begin
            );

            var rjsEndRoutePath = rjsTrainPath.routePath.get(rjsTrainPath.routePath.size() - 1);
            var rjsEndTrackRange = rjsEndRoutePath.trackSections.get(rjsEndRoutePath.trackSections.size() - 1);
            var endLocation = new TrackLocation(
                    infra.getTrackSection(rjsEndTrackRange.track.id.id),
                    rjsEndTrackRange.end
            );

            return from(routePath, startLocation, endLocation);
        } catch (InvalidInfraException e) {
            throw new InvalidSchedule(e.getMessage());
        }
    }

    /** Creates the list of detection sections on the path */
    private static ImmutableList<NewTrainPath.LocatedElement<DetectionSection>> makeDetectionSections(
            ImmutableList<NewTrainPath.LocatedElement<SignalingRoute>> routePath,
            double pathLength
    ) {
        var res = new ArrayList<NewTrainPath.LocatedElement<DetectionSection>>();
        var offset = routePath.get(0).pathOffset();
        for (var locatedRoute : routePath) {
            var route = locatedRoute.element();
            for (var range : route.getInfraRoute().getTrackRanges()) {
                for (var object : range.getDetectors()) {
                    if (object.element() == null)
                        continue;
                    var diDetector = object.element().getDiDetector(range.track.getDirection());
                    var detectionSection = diDetector.detector().getNextDetectionSection(diDetector.direction());
                    addIfDifferent(res, new NewTrainPath.LocatedElement<>(offset + object.offset(), detectionSection));
                }
                offset += range.getLength();
            }
        }

        // Remove the first sections until only one start with a negative offset (the one we start on)
        while (res.size() > 1 && res.get(1).pathOffset() < 0)
            res.remove(0);
        // Remove the sections that start after the end of the path
        res.removeIf(section -> section.pathOffset() >= pathLength);

        return ImmutableList.copyOf(res);
    }

    /** check that everything make sense */
    private static void validate(NewTrainPath path) {
        assert !path.routePath().isEmpty() : "empty route path";
        assert !path.detectionSections().isEmpty() : "no detection section on path";
        assert !path.trackRangePath().isEmpty() : "empty track range path";
        assert Math.abs(path.detectionSections().size() - path.detectors().size()) <= 1
                : "Detection section size is inconsistent";
        assert path.length() > 0 : "length must be strictly positive";

        // TODO checks that the track ranges are properly connected
        // But this would require an actual infra, which technically isn't required otherwise
    }

    /** Creates the list of located routes */
    private static ImmutableList<NewTrainPath.LocatedElement<SignalingRoute>> makeLocatedRoutePath(
            List<SignalingRoute> routePath,
            TrackLocation startLocation
    ) {
        var res = ImmutableList.<NewTrainPath.LocatedElement<SignalingRoute>>builder();
        var offsetOnFirstRoute = offsetFromStartOfPath(
                routePath.get(0).getInfraRoute().getTrackRanges(),
                startLocation
        );
        var offset = -offsetOnFirstRoute;
        if (Math.abs(offset) == 0)
            offset = 0; // avoids the annoying -0
        for (var route : routePath) {
            res.add(new NewTrainPath.LocatedElement<>(offset, route));
            offset += route.getInfraRoute().getLength();
        }
        return res.build();
    }

    /** Returns the distance between the beginning of the list of ranges and the given location */
    private static double offsetFromStartOfPath(ImmutableList<TrackRangeView> path, TrackLocation location) {
        var offset = 0;
        for (var range : path) {
            if (range.contains(location))
                return offset + range.offsetOf(location);
            offset += range.getLength();
        }
        throw new RuntimeException("Location isn't in the given path");
    }

    /** Creates a list of located directed detectors on the path */
    private static ImmutableList<NewTrainPath.LocatedElement<DiDetector>> createDetectorPath(
            ImmutableList<NewTrainPath.LocatedElement<TrackRangeView>> trackSectionPath
    ) {
        var res = new ArrayList<NewTrainPath.LocatedElement<DiDetector>>();
        double offset = 0;
        for (var range : trackSectionPath) {
            for (var object : range.element().getDetectors()) {
                if (object.element() != null)
                    addIfDifferent(res, new NewTrainPath.LocatedElement<>(offset + object.offset(),
                            object.element().getDiDetector(range.element().track.getDirection())));
            }
            offset += range.element().getLength();
        }
        return ImmutableList.copyOf(res);
    }

    /** Creates the lists of track ranges */
    private static ImmutableList<NewTrainPath.LocatedElement<TrackRangeView>> createTrackRangePath(
            List<SignalingRoute> routePath,
            TrackLocation startLocation,
            TrackLocation endLocation
    ) {
        var res = new ArrayList<NewTrainPath.LocatedElement<TrackRangeView>>();
        double offset = 0;
        var reachedStart = false;
        for (int i = 0; i < routePath.size(); i++) {
            var signalingRoute = routePath.get(i);
            var route = signalingRoute.getInfraRoute();
            for (var range : route.getTrackRanges()) {
                if (!reachedStart) {
                    if (!range.contains(startLocation))
                        continue;
                    reachedStart = true;
                    range = range.truncateBegin(startLocation.offset());
                }
                // We have to check if we're on the last route to avoid problems with looping paths
                if (i == routePath.size() - 1 && range.contains(endLocation))
                    range = range.truncateEnd(endLocation.offset());
                res.add(new NewTrainPath.LocatedElement<>(offset, range));
                offset += range.getLength();
            }
        }
        return ImmutableList.copyOf(res);
    }

    /** Adds a located element if it's not already the last on the list */
    private static <T> void addIfDifferent(List<NewTrainPath.LocatedElement<T>> list,
                                           NewTrainPath.LocatedElement<T> element) {
        if (list.isEmpty() || list.get(list.size() - 1).element() != element.element())
            list.add(element);
    }
}