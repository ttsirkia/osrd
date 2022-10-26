import { Dispatch } from 'redux';
import { TFunction } from 'react-i18next';

import {
  formatRouteAspects,
  formatSignalAspects,
  formatStepsWithTime,
  formatStepsWithTimeMulti,
  mergeDatasArea,
  MergedDataPoint,
} from 'applications/osrd/components/Helpers/ChartHelpers';

import { setFailure } from 'reducers/main';
import { Train, Position, RouteAspect, SignalAspect, PositionSpeed } from 'reducers/osrdsimulation';

export interface SimulationTrain {
  id: number;
  isStdcm: boolean;
  name: string;
  trainNumber: number;
  headPosition: Position<Date | null>[][];
  tailPosition: Position<Date | null>[][];
  routeEndOccupancy: Position<Date | null>[][];
  routeBeginOccupancy: Position<Date | null>[][];
  routeAspects: RouteAspect<Date | null, string>[];
  signalAspects: SignalAspect<Date | null, string>[];
  areaBlock: MergedDataPoint<Date | null>[][];
  speed: PositionSpeed<Date | null>[];
  eco_headPosition?: Position<Date | null>[][];
  eco_tailPosition?: Position<Date | null>[][];
  eco_routeEndOccupancy?: Position<Date | null>[][];
  eco_routeBeginOccupancy?: Position<Date | null>[][];
  eco_routeAspects?: RouteAspect<Date | null, string>[];
  eco_signalAspects?: SignalAspect<Date | null, string>[];
  eco_areaBlock?: MergedDataPoint<Date | null>[][];
  eco_speed?: PositionSpeed<Date | null>[];
}

/**
 * Will do some formating & computation to get a trains to be displayed. Stored then with currentSimulation splitted reducer
 * @param {*} dispatch react action dispatcher
 * @param {*} keyValues what do we compare (times vs position vs speed vs slope etc...)
 * @param {*} simulationTrains simulation raw data
 * @param {*} t translation middle
 * @returns
 */
export default function createTrain(
  dispatch: Dispatch,
  keyValues: string[],
  simulationTrains: Train[],
  t: TFunction
) {
  // Prepare data
  const dataSimulation = simulationTrains.map((train: Train, trainNumber: number) => {
    const routeEndOccupancy = formatStepsWithTimeMulti(train.base.route_end_occupancy);
    const routeBeginOccupancy = formatStepsWithTimeMulti(train.base.route_begin_occupancy);
    const dataSimulationTrain: SimulationTrain = {
      id: train.id,
      isStdcm: train.isStdcm,
      name: train.name,
      trainNumber,
      headPosition: formatStepsWithTimeMulti(train.base.head_positions),
      tailPosition: formatStepsWithTimeMulti(train.base.tail_positions),
      routeEndOccupancy,
      routeBeginOccupancy,
      routeAspects: formatRouteAspects(train.base.route_aspects),
      signalAspects: formatSignalAspects(train.base.signal_aspects),
      areaBlock: mergeDatasArea<Date | null>(routeEndOccupancy, routeBeginOccupancy, keyValues),
      speed: formatStepsWithTime(train.base.speeds),
    };

    /* MARECO */
    if (train.eco && !train.eco.error) {
      return {
        ...dataSimulationTrain,
        eco_headPosition: formatStepsWithTimeMulti(train.eco.head_positions),
        eco_tailPosition: formatStepsWithTimeMulti(train.eco.tail_positions),
        eco_routeEndOccupancy: formatStepsWithTimeMulti(train.eco.route_end_occupancy),
        eco_routeBeginOccupancy: formatStepsWithTimeMulti(train.eco.route_begin_occupancy),
        eco_routeAspects: formatRouteAspects(train.eco.route_aspects),
        eco_signalAspects: formatSignalAspects(train.eco.signal_aspects),
        eco_areaBlock: mergeDatasArea<Date | null>(
          dataSimulationTrain.eco_routeEndOccupancy,
          dataSimulationTrain.eco_routeBeginOccupancy,
          keyValues
        ),
        eco_speed: formatStepsWithTime(train.eco.speeds),
      };
    }
    if (train.eco && train.eco.error) {
      // Tbe removed, useless
      dispatch(
        setFailure({
          name: t('errors.eco'),
          message: train.eco.error,
        })
      );
    }
    return dataSimulationTrain;
  });
  return dataSimulation;
}