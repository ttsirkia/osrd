import * as d3 from 'd3';
import { select as d3select } from 'd3-selection';

import { Chart, SimulationTrain } from 'reducers/osrdsimulation/types';
import { defineLinear, defineTime } from 'applications/osrd/components/Helpers/ChartHelpers';
import defineChart from 'applications/osrd/components/Simulation/defineChart';

export default function createChart(
  chart: Chart,
  chartID: string,
  dataSimulation: SimulationTrain[],
  heightOfSpaceTimeChart: number,
  keyValues: string[],
  ref: React.MutableRefObject<HTMLDivElement>,
  reset: boolean,
  rotate: boolean
): Chart {
  d3select(`#${chartID}`).remove();

  const dataSimulationTime = d3.extent(
    dataSimulation.map(
      (train) =>
        d3.extent(
          train.routeBeginOccupancy.map((section) =>
            d3.extent(section, (step: any) => step[keyValues[0]])
          ) as any
        ) as any
    )
  );

  const dataSimulationLinearMax = d3.max([
    d3.max(
      [].concat(
        ...dataSimulation.map((train) =>
          d3.max(
            train.routeEndOccupancy.map((section) =>
              d3.max(section.map((step: any) => step[keyValues[1]]))
            )
          )
        )
      )
    ) || '',
    d3.max(
      [].concat(
        ...dataSimulation.map((train) =>
          d3.max(
            train.routeBeginOccupancy.map((section) =>
              d3.max(section.map((step: any) => step[keyValues[1]]))
            )
          )
        )
      )
    ) || '',
  ]);

  const defineX = chart === undefined || reset ? defineTime(dataSimulationTime) : chart.x;
  const defineY =
    chart === undefined || reset ? defineLinear(dataSimulationLinearMax, 0.05) : chart.y;

  const width = parseInt(d3select(`#container-${chartID}`).style('width'), 10);
  const chartLocal = defineChart(
    width,
    heightOfSpaceTimeChart,
    defineX,
    defineY,
    ref,
    rotate,
    keyValues,
    chartID
  );
  return chart === undefined || reset ? chartLocal : { ...chartLocal, x: chart.x, y: chart.y };
}