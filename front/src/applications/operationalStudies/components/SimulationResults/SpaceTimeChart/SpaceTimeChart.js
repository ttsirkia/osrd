import * as d3 from 'd3';

import React, { useEffect, useRef, useState } from 'react';
import enableInteractivity, {
  traceVerticalLine,
} from 'applications/osrd/components/Simulation/enableInteractivity';
import { Rnd } from 'react-rnd';
import {
  handleWindowResize,
  interpolateOnTime,
  timeShiftTrain,
} from 'applications/osrd/components/Helpers/ChartHelpers';
import ORSD_GEV_SAMPLE_DATA from '../SpeedSpaceChart/sampleData';
import { CgLoadbar } from 'react-icons/cg';
import ChartModal from 'applications/osrd/components/Simulation/ChartModal';
import { GiResize } from 'react-icons/gi';
import { LIST_VALUES_NAME_SPACE_TIME } from 'applications/osrd/components/Simulation/consts';
import PropTypes from 'prop-types';
import createTrain from 'applications/osrd/components/Simulation/SpaceTimeChart/createTrain';

import { useTranslation } from 'react-i18next';

import {
  drawOPs,
  drawAllTrains,
} from 'applications/osrd/components/Simulation/SpaceTimeChart/d3Helpers';

const CHART_ID = 'SpaceTimeChart';
const CHART_MIN_HEIGHT = 200;
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
  const [heightOfSpaceTimeChart, setHeightOfSpaceTimeChart] = useState(CHART_MIN_HEIGHT);
  const [initialHeightOfSpaceTimeChart, setInitialHeightOfSpaceTimeChart] =
    useState(heightOfSpaceTimeChart);

  const handleKey = ({ key }) => {
    if (['+', '-'].includes(key)) {
      setShowModal(key);
    }
  };

  // ACTIONS

  // Everyhing should be done by Hoc, has no direct effect on Comp behavior
  const offsetTimeByDragging = (offset) => {
    // Data preop
    if (dataSimulation) {
      // better define default props
      const trains = Array.from(dataSimulation.trains);
      trains[selectedTrain] = timeShiftTrain(trains[selectedTrain], offset);
      setDataSimulation({ ...simulation, trains });
      console.log('Call offset time dragging', offset);
      onOffsetTimeByDragging(offset);
    }
  };

  // Ok, isolated
  const toggleRotation = () => {
    d3.select(`#${CHART_ID}`).remove();
    setChart({ ...chart, x: chart.y, y: chart.x });
    setRotate(!rotate);
    drawAllTrains(
      false,
      dataSimulation,
      false,
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
    //dispatchUpdateMustRedraw(true);
  };

  // D3 Operations
// HORREUR
  useEffect(() => {
    //setDataSimulation(createTrain(dispatch, keyValues, simulation.trains, t));
    setTimeout(() => {
      dispatchUpdateMustRedraw(true);
    }, 0);
  }, []);

  useEffect(() => {
    // ADN, entire fonction operation is subject to one condition, so aopply this condition before OR write clear and first condition to return (do nothing)
    console.log('dragOffset Change', dragOffset);
    offsetTimeByDragging(dragOffset);
  }, [dragOffset]);

  useEffect(() => {
    const trains = dataSimulation?.trains || simulation.trains;
    const newDataSimulation = createTrain(dispatch, keyValues, trains, t);

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
        true
      );

      // Reprogram !
      //handleWindowResize(CHART_ID, dispatch, drawAllTrains, isResizeActive, setResizeActive);
    }
  }, [mustRedraw, rotate, selectedTrain, dataSimulation, heightOfSpaceTimeChart]);

  // ADN: trigger a redraw on every simulation change. This is the right pattern.
  useEffect(() => {
    setDataSimulation(simulation);
    // Reprogram !
    // handleWindowResize(CHART_ID, dispatch, drawAllTrains, isResizeActive, setResizeActive);
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

  console.log(dataSimulation)

  return (
    <Rnd
      default={{
        x: 0,
        y: 0,
        width: '100%',
        height: `${heightOfSpaceTimeChart}px`,
      }}
      minHeight={CHART_MIN_HEIGHT}
      disableDragging
      enableResizing={{
        bottom: true,
      }}
      onResizeStart={() => setInitialHeightOfSpaceTimeChart(heightOfSpaceTimeChart)}
      onResize={(_e, _dir, _refToElement, delta) => {
        setHeightOfSpaceTimeChart(initialHeightOfSpaceTimeChart + delta.height);
      }}
      onResizeStop={() => {
        dispatchUpdateMustRedraw(true)
      }}
    >
      <div className="spacetimechart-container"></div>
      <div
        id={`container-${CHART_ID}`}
        className="spacetime-chart w-100"
        style={{ height: `${heightOfSpaceTimeChart}px` }}
      >
        {showModal !== '' ? (
          <ChartModal
            type={showModal}
            setShowModal={setShowModal}
            trainName={dataSimulation?.trains[selectedTrain]?.name}
            offsetTimeByDragging={offsetTimeByDragging}
          />
        ) : null}
        <div ref={ref} />
        <button
          type="button"
          className="btn-rounded btn-rounded-white box-shadow btn-rotate"
          onClick={() => toggleRotation(rotate, setRotate, dataSimulation)}
        >
          <i className="icons-refresh" />
        </button>
        <button
          type="button"
          className="btn-rounded btn-rounded-white box-shadow btn-rotate mr-5"
          onClick={() => {
            setResetChart(true);
            dispatchUpdateMustRedraw(true);
          }}
        >
          <GiResize />
        </button>
        <div className="handle-tab-resize">
          <CgLoadbar />
        </div>
      </div>
    </Rnd>
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
  allowancesSettings: ORSD_GEV_SAMPLE_DATA.allowancesSettings,
  dispatch: () => {},
  mustRedraw: ORSD_GEV_SAMPLE_DATA.mustRedraw,
  positionValues: ORSD_GEV_SAMPLE_DATA.positionValues,
  selectedTrain: ORSD_GEV_SAMPLE_DATA.selectedTrain,
  timePosition: ORSD_GEV_SAMPLE_DATA.timePosition,
  onOffsetTimeByDragging: ({ ...args }) => {
    console.log('onOffsetTimeByDragging called');
  },
  onDragEnding: () => {
    console.log('onDragEnding called');
  },
  dispatchUpdateMustRedraw: () => {
    console.log('dispatchUpdateMustRedraw called');
  },
  dispatchUpdateChart: () => {
    console.log('dispatchUpdateChart called');
  },
  dispatchUpdatePositionValues: () => {
    console.log('dispatchUpdatePositionValues called');
  },
  dispatchUpdateContextMenu: () => {
    console.log('dispatchUpdateContextMenu called');
  },
};