import React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import {
  updateMustRedraw,
  updateContextMenu,
  updateChart,
  updatePositionValues,
  updateTimePositionValues,
} from 'reducers/osrdsimulation/actions';
import {
  getAllowancesSettings,
  getPositionValues,
  getSelectedProjection,
  getSelectedTrain,
  getTimePosition,
  getPresentSimulation,
  getIsPlaying,
} from 'reducers/osrdsimulation/selectors';
import { persistentUpdateSimulation } from 'reducers/osrdsimulation/simulation';
import SpaceTimeChart from './SpaceTimeChart';

/**
 * HOC to provide store data
 * @param {RFC} Component
 * @returns RFC with OSRD Data. SignalSwitch
 */
const withOSRDData = (Component) =>
  function WrapperComponent(props) {
    const allowancesSettings = useSelector(getAllowancesSettings);
    const positionValues = useSelector(getPositionValues);
    const selectedTrain = useSelector(getSelectedTrain);
    // à quoi correspond selectedProjection ?
    const selectedProjection = useSelector(getSelectedProjection);
    const timePosition = useSelector(getTimePosition);
    const simulation = useSelector(getPresentSimulation);
    const isPlaying = useSelector(getIsPlaying);

    const dispatch = useDispatch();

    // Consequence of direct actions by component
    const onOffsetTimeByDragging = (trains) => {
      dispatch(persistentUpdateSimulation({ ...simulation, trains }));
    };

    const dispatchUpdatePositionValues = (newPositionValues) => {
      dispatch(updatePositionValues(newPositionValues));
    };

    // difference entre TimePositionValues et PositionValues (besoin cruel de typage)
    const dispatchUpdateTimePositionValues = (newTimePositionValues) => {
      dispatch(updateTimePositionValues(newTimePositionValues));
    };

    const dispatchUpdateMustRedraw = (newMustRedraw) => {
      dispatch(updateMustRedraw(newMustRedraw));
    };

    const dispatchUpdateContextMenu = (contextMenu) => {
      dispatch(updateContextMenu(contextMenu));
    };

    const dispatchUpdateChart = (chart) => {
      dispatch(updateChart(chart));
    };

    return (
      <Component
        {...props}
        allowancesSettings={allowancesSettings}
        dispatch={dispatch}
        dispatchUpdateContextMenu={dispatchUpdateContextMenu}
        dispatchUpdateChart={dispatchUpdateChart}
        dispatchUpdateMustRedraw={dispatchUpdateMustRedraw}
        dispatchUpdatePositionValues={dispatchUpdatePositionValues}
        dispatchUpdateTimePositionValues={dispatchUpdateTimePositionValues}
        inputSelectedTrain={selectedTrain}
        onOffsetTimeByDragging={onOffsetTimeByDragging}
        positionValues={positionValues}
        selectedProjection={selectedProjection}
        simulation={simulation}
        simulationIsPlaying={isPlaying}
        timePosition={timePosition}
      />
    );
  };

const OSRDSpaceTimeChart = withOSRDData(SpaceTimeChart);

export default OSRDSpaceTimeChart;
