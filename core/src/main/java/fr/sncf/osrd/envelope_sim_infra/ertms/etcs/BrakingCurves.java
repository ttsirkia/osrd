package fr.sncf.osrd.envelope_sim_infra.ertms.etcs;

import static fr.sncf.osrd.envelope.part.constraints.EnvelopePartConstraintType.CEILING;
import static fr.sncf.osrd.envelope.part.constraints.EnvelopePartConstraintType.FLOOR;
import static fr.sncf.osrd.envelope_sim.EnvelopeSimContext.UseCase.*;
import static fr.sncf.osrd.envelope_sim_infra.ertms.etcs.BrakingCurves.BrakingCurveType.*;
import static fr.sncf.osrd.envelope_sim_infra.ertms.etcs.FixedValueData.*;
import static fr.sncf.osrd.envelope_sim_infra.ertms.etcs.NationalDefaultData.qNvinhsmicperm;

import fr.sncf.osrd.envelope.Envelope;
import fr.sncf.osrd.envelope.EnvelopeAttr;
import fr.sncf.osrd.envelope.EnvelopeCursor;
import fr.sncf.osrd.envelope.part.ConstrainedEnvelopePartBuilder;
import fr.sncf.osrd.envelope.part.EnvelopePart;
import fr.sncf.osrd.envelope.part.EnvelopePartBuilder;
import fr.sncf.osrd.envelope.part.constraints.EnvelopeConstraint;
import fr.sncf.osrd.envelope.part.constraints.SpeedConstraint;
import fr.sncf.osrd.envelope_sim.EnvelopePath;
import fr.sncf.osrd.envelope_sim.EnvelopeProfile;
import fr.sncf.osrd.envelope_sim.EnvelopeSimContext;
import fr.sncf.osrd.envelope_sim.TrainPhysicsIntegrator;
import fr.sncf.osrd.envelope_sim.overlays.EnvelopeDeceleration;
import fr.sncf.osrd.envelope_sim.pipelines.MaxSpeedEnvelope;
import fr.sncf.osrd.envelope_sim_infra.EnvelopeTrainPath;
import fr.sncf.osrd.infra_state.api.TrainPath;
import fr.sncf.osrd.train.RollingStock;
import kotlin.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrakingCurves {

    public enum BrakingCurveType {
        EBD,
        EBI,
        SBD,
        SBI,
        SBI1,
        SBI2,
        GUI,
        WARNING,
        PS,
        IND
    }

    /** Computes the ETCS braking curves from a path, a schedule, a time step, and a MRSP envelope */
    public static List<EnvelopePart> from(
            TrainPath trainPath,
            RollingStock rollingStock,
            double timeStep,
            Envelope mrsp
    ) {
        var totalEBDCurves = computeETCSBrakingCurves(EBD, trainPath, rollingStock, timeStep, mrsp);
        var totalSBDCurves = computeETCSBrakingCurves(SBD, trainPath, rollingStock, timeStep, mrsp);
        var totalGUICurves = computeETCSBrakingCurves(GUI, trainPath, rollingStock, timeStep, mrsp);
        var totalEBICurves =
                computeETCSBrakingCurvesFromRef(EBI, trainPath, rollingStock, mrsp, totalEBDCurves);
        var totalCurves = new ArrayList<EnvelopePart>();
        totalCurves.addAll(totalEBDCurves);
        totalCurves.addAll(totalSBDCurves);
        totalCurves.addAll(totalGUICurves);
        totalCurves.addAll(totalEBICurves);
        return totalCurves;
    }

    private static Envelope computeCeilingEnvelope(BrakingCurveType type, Envelope mrsp) {
        var parts = new EnvelopePart[mrsp.size()];
        for (int i = 0; i < mrsp.size(); i++) {
            var part = mrsp.get(i);
            var speed = part.getMinSpeed();
            var newSpeed = speed + computedV(type, speed);
            parts[i] = EnvelopePart.generateTimes(
                    Collections.singleton(EnvelopeProfile.CONSTANT_SPEED),
                    new double[]{part.getBeginPos(), part.getEndPos()},
                    new double[]{newSpeed, newSpeed}
            );
        }
        return Envelope.make(parts);
    }

    private static List<EnvelopePart> computeETCSBrakingCurves(
            BrakingCurveType type,
            TrainPath trainPath,
            RollingStock rollingStock,
            double timeStep,
            Envelope mrsp
    ) {
        var ceilingEnvelope = computeCeilingEnvelope(type, mrsp);

        var envelopePath = EnvelopeTrainPath.from(trainPath);
        EnvelopeSimContext context;

        switch (type) {
            case EBD -> context = new EnvelopeSimContext(rollingStock, envelopePath, timeStep, ETCS_EBD);
            case SBD -> context = new EnvelopeSimContext(rollingStock, envelopePath, timeStep, ETCS_SBD);
            case GUI -> context = new EnvelopeSimContext(rollingStock, envelopePath, timeStep, ETCS_GUI);
            default -> context = new EnvelopeSimContext(rollingStock, envelopePath, timeStep, RUNNING_TIME);
        }

        var markerBoardsCurves =
                computeBrakingCurvesAtMarkerBoards(type, trainPath, context, ceilingEnvelope);
        var slowdownsCurves = computeBrakingCurvesAtSlowdowns(type, context, ceilingEnvelope);
        var totalCurves = new ArrayList<EnvelopePart>();
        totalCurves.addAll(markerBoardsCurves);
        totalCurves.addAll(slowdownsCurves);
        return totalCurves;
    }

    /**
     * Compute braking curves at every slowdown, with a given reference curve
     * This is needed to comput EBI, SBI, and Warning
     */
    private static List<EnvelopePart> computeETCSBrakingCurvesFromRef(
            BrakingCurveType type,
            TrainPath path,
            RollingStock rollingStock,
            Envelope ceiling,
            List<EnvelopePart> references
    ) {
        assert type != EBD && type != SBD && type != GUI;

        var envelopePath = EnvelopeTrainPath.from(path);
        var slowdownBrakingCurves = new ArrayList<EnvelopePart>();

        for (var part : references) {
            var pos = part.clonePositions();
            var speeds = part.cloneSpeeds();
            var newPos = new ArrayList<Double>();
            var newSpeeds = new ArrayList<Double>();
            for (int i = 0; i < part.pointCount(); i++) {
                var bec =
                        computedBecvBec(rollingStock, envelopePath, part, speeds[i], part.getEndSpeed());
                var dBec = bec.component1();
                var delta = bec.component2();
                //make sure positions and speeds stay positives
                // TODO : add at least one point in 0 if the curves need to be cut
                if (pos[i] - dBec >= 0 && speeds[i] - delta >= 0) {
                    newPos.add(pos[i] - dBec);
                    newSpeeds.add(speeds[i] - delta);
                }
            }
            assert newPos.size() == newSpeeds.size();
            if (newPos.size() <= 1)
                continue;
            double[] newPosArray = new double[newPos.size()];
            double[] newSpeedsArray = new double[newSpeeds.size()];

            for (int i = 0; i < newPosArray.length; i++) {
                newPosArray[i] = newPos.get(i);
                newSpeedsArray[i] = newSpeeds.get(i);
            }
            var newPart = EnvelopePart.generateTimes(newPosArray, newSpeedsArray);
            slowdownBrakingCurves.add(newPart);
        }

        return slowdownBrakingCurves;
    }


    /**
     * Compute braking curves at every slowdown.
     * This method should only be called for EBD, SBD, and GUI, as they don't need a reference EnvelopePart.
     * Otherwise, call the method below with a reference EnvelopePart.
     */
    private static List<EnvelopePart> computeBrakingCurvesAtSlowdowns(
            BrakingCurveType type,
            EnvelopeSimContext context,
            Envelope ceiling
    ) {
        assert type == EBD || type == SBD || type == GUI;
        var cursor = EnvelopeCursor.backward(ceiling);
        var slowdownBrakingCurves = new ArrayList<EnvelopePart>();

        while (cursor.findPartTransition(MaxSpeedEnvelope::increase)) {
            var brakingCurve =
                    computeBrakingCurve(context, ceiling, cursor.getPosition(), cursor.getSpeed());
            slowdownBrakingCurves.add(brakingCurve);
            cursor.nextPart();
        }
        return slowdownBrakingCurves;
    }

    /**
     * Compute braking curves at every marker board
     */
    private static List<EnvelopePart> computeBrakingCurvesAtMarkerBoards(
            BrakingCurveType type,
            TrainPath trainPath,
            EnvelopeSimContext context,
            Envelope mrsp
    ) {
        var ranges = TrainPath.removeLocation(trainPath.trackRangePath());

        var detectorsEBDCurves = new ArrayList<EnvelopePart>();

        for (var range : ranges) {
            if (range.getLength() == 0)
                continue;
            var markerBoards = range.getDetectors();
            for (var detector : markerBoards) {
                var detectorPosition = range.begin + detector.offset();
                if (detectorPosition > 0 && detectorPosition <= trainPath.length()) {
                    var brakingCurve =
                            computeBrakingCurve(context, mrsp, detectorPosition, 0);
                    detectorsEBDCurves.add(brakingCurve);
                }
            }
        }
        return detectorsEBDCurves;
    }

    /**
     * EBD = Emergency Brake Deceleration
     */
    private static EnvelopePart computeBrakingCurve(EnvelopeSimContext context,
                                                    Envelope mrsp,
                                                    double targetPosition,
                                                    double targetSpeed) {
        // if the stopPosition is below zero, or above path length, the input is invalid
        if (targetPosition <= 0.0 || targetPosition > context.path.getLength())
            throw new RuntimeException(String.format(
                    "Trying to compute ETCS braking curve from out of bounds ERTMS marker board (position = %f,"
                    + "path length = %f)",
                    targetPosition, context.path.getLength()
            ));
        var partBuilder = new EnvelopePartBuilder();
        partBuilder.setAttr(EnvelopeProfile.BRAKING);
        var overlayBuilder = new ConstrainedEnvelopePartBuilder(
                partBuilder,
                new SpeedConstraint(0, FLOOR),
                new EnvelopeConstraint(mrsp, CEILING)
        );
        EnvelopeDeceleration.decelerate(context, targetPosition, targetSpeed, overlayBuilder, -1);

        return partBuilder.build();
    }

    /** Compute the constant speed offset for ceiling envelopes. */
    private static double computedV(BrakingCurveType type, double speed) {
        double vMin = 0;
        double vMax = 0;
        double dvMin = 0;
        double dvMax = 0;
        var vDelta0 = !qNvinhsmicperm ? vUra(speed) : 0.;
        switch (type) {
            case EBD, SBD, GUI -> {
                return vDelta0;
            }
            case EBI -> {
                vMin = vEbiMin;
                vMax = vEbiMax;
                dvMin = dvEbiMin;
                dvMax = dvEbiMax;
            }
            case SBI -> {
                vMin = vSbiMin;
                vMax = vSbiMax;
                dvMin = dvSbiMin;
                dvMax = dvSbiMax;
            }
            case WARNING -> {
                vMin = vWarningMin;
                vMax = vWarningMax;
                dvMin = dvWarningMin;
                dvMax = dvWarningMax;
            }
        }
        if (speed <= vMin)
            return dvMin;
        else if (speed < vMax)
            return (dvMax - dvMin) / (vMax - vMin) * (speed - vMin) + dvMin;
        else return dvMax;
    }

    /** Compute the speed offset between EBD and EBI curves, for a given speed. */
    private static Pair<Double, Double> computedBecvBec(RollingStock rollingStock,
                                                        EnvelopePath path,
                                                        EnvelopePart ebd,
                                                        double speed,
                                                        double targetSpeed) {

        var position = ebd.interpolatePosition(speed);
        var grade = path.getLowestGrade(position - rollingStock.length, position);
        var weightForce = TrainPhysicsIntegrator.getWeightForce(rollingStock, grade);

        // the time during which the traction effort is still present
        var tTraction = Math.max(rollingStock.tTractionCutOff - (tWarning + rollingStock.tBs2), 0);
        // the remaining time during which the traction effort is not present
        var tBerem = Math.max(rollingStock.tBe - tTraction, 0);
        var vDelta0 = !qNvinhsmicperm ? vUra(speed) : 0.;
        // estimated acceleration during tTraction, worst case scenario (the train accelerates as much as possible)
        var aEst1 = TrainPhysicsIntegrator.computeAcceleration(
                rollingStock,
                rollingStock.getRollingResistance(speed),
                weightForce,
                speed,
                rollingStock.getMaxEffort(speed),
                0,
                1
        );
        // estimated acceleration during tBerem, worst case scenario (aEst2 is between 0 and 0.4), expressed in m/s²
        var aEst2 = 0.4;
        // speed correction due to the traction staying active during tTraction
        var vDelta1 = aEst1 * tTraction;
        // speed correction due to the braking system not being active yet
        var vDelta2 = aEst2 * tBerem;

        var maxV = Math.max(speed + vDelta0 + vDelta1, targetSpeed);
        var dBec = Math.max(speed + vDelta0 + vDelta1 / 2, targetSpeed) * tTraction + (maxV + vDelta1 / 2) * tBerem;
        var vBec = maxV + vDelta2;

        var delta = vBec - speed;

        return new Pair<>(dBec, delta);
    }
}
