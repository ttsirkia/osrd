import React from 'react';
import { ComponentStory, ComponentMeta } from '@storybook/react';
import Allowances from 'applications/operationalStudies/components/SimulationResults/Allowances/Allowances';
import ORSD_GEV_SAMPLE_DATA from 'applications/operationalStudies/components/SimulationResults/SpeedSpaceChart/sampleData';
import 'styles/main.css';
export default {
  /* 👇 The title prop is optional.
   * See https://storybook.js.org/docs/react/configure/overview#configure-story-loading
   * to learn how to generate automatic titles
   */
  title: 'TrainSimulation/Allowances',
  component: Allowances,
};

const Template: ComponentStory<typeof Allowances> = (args) => (
  <div className="simulation-results">
    <div className="speedspacechart-container">
      <Allowances {...args} />
    </div>
  </div>
);

export const Standard = Template.bind({});

Standard.args = {
  dispatch: () => {},
  toggleSetting: () => {},
  onSetSettings: () => {},
  dispatchUpdateMustRedraw: () => {},
  onSetBaseHeightOfSpeedSpaceChart: () => {},
};
