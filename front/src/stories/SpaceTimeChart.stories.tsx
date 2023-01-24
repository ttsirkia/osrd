import React from 'react';
import { ComponentStory, ComponentMeta } from '@storybook/react';
import SpaceTimeChart from 'applications/osrd/components/Simulation/SpaceTimeChart/SpaceTimeChart';

export default {
  /* 👇 The title prop is optional.
   * See https://storybook.js.org/docs/react/configure/overview#configure-story-loading
   * to learn how to generate automatic titles
   */
  title: 'TrainSimulation/SpaceTimeChart',
  component: SpaceTimeChart,
};

const Template: ComponentStory<typeof SpaceTimeChart> = (args) => (
  <div className="spacetimechart-container">
    <SpaceTimeChart heightOfSpaceTimeChart={400} {...args} />
  </div>
);

export const Standard = Template.bind({});
