import * as d3 from 'd3';

import React, { useEffect, useRef, useState } from 'react';
import enableInteractivity, {
  traceVerticalLine,
} from 'applications/osrd/components/Simulation/enableInteractivity';
import {
  handleWindowResize,
  interpolateOnTime,
  timeShiftTrain,
} from 'applications/osrd/components/Helpers/ChartHelpers';
import {
  updateChart,
  updateContextMenu,
  updateMustRedraw,
  updatePositionValues,
} from 'reducers/osrdsimulation/actions';
import { useDispatch, useSelector } from 'react-redux';
import ORSD_GEV_SAMPLE_DATA from '../SpeedSpaceChart/sampleData';
import { CgLoadbar } from 'react-icons/cg';
import ChartModal from 'applications/osrd/components/Simulation/ChartModal';
import { GiResize } from 'react-icons/gi';
import { LIST_VALUES_NAME_SPACE_TIME } from 'applications/osrd/components/Simulation/consts';
import PropTypes from 'prop-types';
import { changeTrain } from 'applications/osrd/components/TrainList/TrainListHelpers';
import createChart from 'applications/osrd/components/Simulation/SpaceTimeChart/createChart';
import createTrain from 'applications/osrd/components/Simulation/SpaceTimeChart/createTrain';
import drawTrain from 'applications/osrd/components/Simulation/SpaceTimeChart/drawTrain';
import { persistentUpdateSimulation } from 'reducers/osrdsimulation/simulation';
import { useTranslation } from 'react-i18next';

import {
  drawOPs,
  drawAllTrains,
} from 'applications/osrd/components/Simulation/SpaceTimeChart/d3Helpers';

const CHART_ID = 'SpaceTimeChart';
/*
const drawAxisTitle = (chart, rotate) => {
  chart.drawZone
    .append('text')
    .attr('class', 'axis-unit')
    .attr('text-anchor', 'end')
    .attr('transform', rotate ? 'rotate(0)' : 'rotate(-90)')
    .attr('x', rotate ? chart.width - 10 : -10)
    .attr('y', rotate ? chart.height - 10 : 20)
    .text('KM');
};
*/

export default function SpaceTimeChart(props) {
  const { heightOfSpaceTimeChart } = props;
  const ref = useRef();
  //const dispatch = useDispatch();
  const { t } = useTranslation(['allowances']);
  const {
    allowancesSettings,
    mustRedraw,
    positionValues,
    selectedProjection,
    selectedTrain,
    timePosition,
    consolidatedSimulation,
    simulation,
    dispatch,
    onOffsetTimeByDragging,
    onDragEnding,
    dispatchUpdateMustRedraw,
    dispatchUpdatePositionValues,
    dispatchUpdateChart,
    dispatchUpdateContextMenu,
  } = props;

  const keyValues = ['time', 'position'];
  const [rotate, setRotate] = useState(false);
  const [isResizeActive, setResizeActive] = useState(false);
  const [chart, setChart] = useState(undefined);
  const [resetChart, setResetChart] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [yPosition, setYPosition] = useState(0);
  const [dataSimulation, setDataSimulation] = useState(undefined);
  const [showModal, setShowModal] = useState('');
  const [dragOffset, setDragOffset] = useState(0);
  const [dragEnding, setDragEnding] = useState(false);

  const handleKey = ({ key }) => {
    if (['+', '-'].includes(key)) {
      setShowModal(key);
    }
  };

  // ACTIONS

  // Everyhing should be done by Hoc, has no direct effect on Comp behavior
  const offsetTimeByDragging = (offset) => {
    // Data preop
    const trains = Array.from(simulation.trains);
    trains[selectedTrain] = timeShiftTrain(trains[selectedTrain], offset);
    onOffsetTimeByDragging(trains);
  };

  // Ok, isolated
  const toggleRotation = () => {
    d3.select(`#${CHART_ID}`).remove();
    setChart({ ...chart, x: chart.y, y: chart.x });
    setRotate(!rotate);
    dispatchUpdateMustRedraw(true);
  };

  // D3 Operations

  // Ok, isolated but useless?
  /*
  const drawOPs = (chartLocal) => {
    const operationalPointsZone = chartLocal.drawZone
      .append('g')
      .attr('id', 'get-operationalPointsZone');
    simulation.trains[selectedTrain].base.stops.forEach((stop) => {
      operationalPointsZone
        .append('line')
        .datum(stop.position)
        .attr('id', `op-${stop.id}`)
        .attr('class', 'op-line')
        .attr('x1', rotate ? (d) => chartLocal.x(d) : 0)
        .attr('y1', rotate ? 0 : (d) => chartLocal.y(d))
        .attr('x2', rotate ? (d) => chartLocal.x(d) : chartLocal.width)
        .attr('y2', rotate ? chartLocal.height : (d) => chartLocal.y(d));
      operationalPointsZone
        .append('text')
        .datum(stop.position)
        .attr('class', 'op-text')
        .text(`${stop.name || 'Unknown'} ${Math.round(stop.position) / 1000}`)
        .attr('x', rotate ? (d) => chartLocal.x(d) : 0)
        .attr('y', rotate ? 0 : (d) => chartLocal.y(d))
        .attr('text-anchor', 'center')
        .attr('dx', 5)
        .attr('dy', rotate ? 15 : -5);
    });
  };
  */

  // eslint-disable-next-line default-param-last
  /*
  const drawAllTrains = (reset, forceRedraw = false, newDataSimulation) => {
    const currentDataSimulation = newDataSimulation || dataSimulation;

    if (mustRedraw || forceRedraw) {
      const chartLocal = createChart(
        chart,
        CHART_ID,
        currentDataSimulation,
        heightOfSpaceTimeChart,
        keyValues,
        ref,
        true, // We dont nor pass a reset trigger anymore, reset is an action or a effect
        rotate
      );

      chartLocal.svg.on('click', () => {
        dispatch(updateContextMenu(undefined));
      });

      drawOPs(chartLocal, simulation.trains[selectedTrain], rotate);

      drawAxisTitle(chartLocal, rotate);
      currentDataSimulation.forEach((train, idx) => {
        drawTrain(
          chartLocal,
          dispatch,
          train,
          train.id === selectedProjection?.id,
          idx === selectedTrain,
          keyValues,
          allowancesSettings,
          offsetTimeByDragging,
          rotate,
          setDragEnding,
          setDragOffset,
          simulation,
          train.isStdcm
        );
      });
      enableInteractivity(
        chartLocal,
        currentDataSimulation[selectedTrain],
        dispatch,
        keyValues,
        LIST_VALUES_NAME_SPACE_TIME,
        positionValues,
        rotate,
        setChart,
        setYPosition,
        setZoomLevel,
        yPosition,
        zoomLevel
      );
      // findConflicts(chartLocal, dataSimulation, rotate);
      setChart(chartLocal);
      dispatch(updateChart({ ...chartLocal, rotate }));
      dispatch(updateMustRedraw(false));
      setResetChart(false);
    }
  };
  */

  useEffect(() => {
    setDataSimulation(createTrain(dispatch, keyValues, simulation.trains, t));
    setTimeout(() => {
      dispatch(updateMustRedraw(true));
    }, 0);
  }, []);

  useEffect(() => {
    // ADN, entire fonction operation is subject to one condition, so aopply this condition before OR write clear and first condition to return (do nothing)
    offsetTimeByDragging(dragOffset);
  }, [dragOffset]);

  useEffect(() => {
    onDragEnding(dragEnding, setDragEnding);
  }, [dragEnding]);

  // DO NOT SEE THE POINT
  useEffect(() => {
    setResetChart(true);
  }, [consolidatedSimulation]);

  useEffect(() => {
    const newDataSimulation = createTrain(dispatch, keyValues, simulation.trains, t)
    setDataSimulation(newDataSimulation);
    if (newDataSimulation) {
      drawAllTrains(
        resetChart,
        newDataSimulation,
        mustRedraw,
        chart,
        heightOfSpaceTimeChart,
        keyValues,
        ref,
        rotate,
        dispatch,
        CHART_ID,
        simulation,
        selectedTrain,
        positionValues,
        setChart,
        setResetChart,
        setYPosition,
        setZoomLevel,
        setDragEnding,
        setDragOffset,
        yPosition,
        zoomLevel,
        selectedProjection,
        dispatchUpdateMustRedraw,
        dispatchUpdateChart,
        dispatchUpdateContextMenu,
        allowancesSettings,
        offsetTimeByDragging,
        false
      );

      // Reprogram !
      //handleWindowResize(CHART_ID, dispatch, drawAllTrains, isResizeActive, setResizeActive);
    }
  }, [mustRedraw, rotate, selectedTrain, consolidatedSimulation]);

  // ADN: trigger a redraw on every simulation change. This is the right pattern.
  useEffect(() => {
    setDataSimulation(createTrain(dispatch, keyValues, simulation.trains, t));
    const newDataSimulation = createTrain(dispatch, keyValues, simulation.trains, t);
    if (dataSimulation) {
      // ADN drawAllTrain already traceVerticalLines

      drawAllTrains(
        resetChart,
        newDataSimulation,
        dataSimulation,
        mustRedraw,
        chart,
        heightOfSpaceTimeChart,
        keyValues,
        ref,
        rotate,
        dispatch,
        CHART_ID,
        simulation,
        selectedTrain,
        positionValues,
        setChart,
        setResetChart,
        setYPosition,
        setZoomLevel,
        setDragEnding,
        setDragOffset,
        yPosition,
        zoomLevel,
        selectedProjection,
        dispatchUpdateMustRedraw,
        dispatchUpdateChart,
        dispatchUpdateContextMenu,
        allowancesSettings,
        offsetTimeByDragging,
        true
      );
      // Reprogram !
      // handleWindowResize(CHART_ID, dispatch, drawAllTrains, isResizeActive, setResizeActive);
    }
  }, [simulation.trains]);

  useEffect(() => {
    if (timePosition && dataSimulation && dataSimulation[selectedTrain]) {
      // ADN: too heavy, dispatch on release (dragEnd), careful with dispatch !
      const newPositionValues = interpolateOnTime(
        dataSimulation[selectedTrain],
        keyValues,
        LIST_VALUES_NAME_SPACE_TIME,
        timePosition
      );
      dispatchUpdatePositionValues(newPositionValues);
    }
  }, [chart, mustRedraw]);

  useEffect(() => {
    if (dataSimulation) {
      traceVerticalLine(
        chart,
        dataSimulation[selectedTrain],
        keyValues,
        LIST_VALUES_NAME_SPACE_TIME,
        positionValues,
        'headPosition',
        rotate,
        timePosition
      );
    }
  }, [positionValues]);

  useEffect(() => {
    window.addEventListener('keydown', handleKey);
    return () => {
      window.removeEventListener('keydown', handleKey);
    };
  }, []);

  return (
    <div
      id={`container-${CHART_ID}`}
      className="spacetime-chart w-100"
      style={{ height: `${heightOfSpaceTimeChart}px` }}
    >
      {showModal !== '' ? (
        <ChartModal
          type={showModal}
          setShowModal={setShowModal}
          trainName={dataSimulation[selectedTrain].name}
          offsetTimeByDragging={offsetTimeByDragging}
        />
      ) : null}
      <div ref={ref} />
      <button
        type="button"
        className="btn-rounded btn-rounded-white box-shadow btn-rotate"
        onClick={() => toggleRotation(rotate, setRotate)}
      >
        <i className="icons-refresh" />
      </button>
      <button
        type="button"
        className="btn-rounded btn-rounded-white box-shadow btn-rotate mr-5"
        onClick={() => {
          setResetChart(true);
          dispatch(updateMustRedraw(true));
        }}
      >
        <GiResize />
      </button>
      <div className="handle-tab-resize">
        <CgLoadbar />
      </div>
    </div>
  );
}

SpaceTimeChart.propTypes = {
  heightOfSpaceTimeChart: PropTypes.number.isRequired,
  onOffsetTimeByDragging: PropTypes.func,
  dispatchUpdateMustRedraw: PropTypes.func,
};

SpaceTimeChart.defaultProps = {
  heightOfSpeedSpaceChart: 250,
  simulation: ORSD_GEV_SAMPLE_DATA.simulation.present,
  chartXGEV: undefined,
  dispatch: () => {},
  mustRedraw: ORSD_GEV_SAMPLE_DATA.mustRedraw,
  positionValues: ORSD_GEV_SAMPLE_DATA.positionValues,
  selectedTrain: ORSD_GEV_SAMPLE_DATA.selectedTrain,
  speedSpaceSettings: ORSD_GEV_SAMPLE_DATA.speedSpaceSettings,
  timePosition: ORSD_GEV_SAMPLE_DATA.timePosition,
  consolidatedSimulation: ORSD_GEV_SAMPLE_DATA.consolidatedSimulation,
  onOffsetTimeByDragging: () => {
    console.log('onOffsetTimeByDragging called');
  },
  onDragEnding: () => {
    console.log('onDragEnding called');
  },
  dispatchUpdateMustRedraw: () => {
    console.log('dispatchUpdateMustRedraw called');
  },
};