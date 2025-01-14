import { test, expect } from '@playwright/test';
import { PlaywrightHomePage } from './home-page-model';
import projectJSON from './assets/operationStudies/project.json';
import studyJSON from './assets/operationStudies/study.json';
import scenarioJSON from './assets/operationStudies/scenario.json';
import timetableJSON from './assets/operationStudies/timetable.json';

test.describe('Testing if all mandatory elements simulation configuration are loaded in operationnal studies app', () => {
  // Declare the necessary variable for the test
  let playwrightHomePage: PlaywrightHomePage;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    playwrightHomePage = new PlaywrightHomePage(page);
    await playwrightHomePage.goToHomePage();
    await playwrightHomePage.page.route('**/projects/*/', async (route) => {
      route.fulfill({
        status: 200,
        body: JSON.stringify(projectJSON),
      });
    });
    await playwrightHomePage.page.route('**/projects/*/studies/*/', async (route) => {
      route.fulfill({
        status: 200,
        body: JSON.stringify(studyJSON),
      });
    });
    await playwrightHomePage.page.route('**/projects/*/studies/*/scenarios/*/', async (route) => {
      route.fulfill({
        status: 200,
        body: JSON.stringify(scenarioJSON),
      });
    });
    await playwrightHomePage.page.route('**/timetable/*/', async (route) => {
      route.fulfill({
        status: 200,
        body: JSON.stringify(timetableJSON),
      });
    });
    await playwrightHomePage.page.goto('/operational-studies/scenario');
    await page.getByTestId('scenarios-filter-button').click();
  });

  test('RollingStockSelector is displayed', async () => {
    expect(playwrightHomePage.page.getByTestId('rollingstock-selector')).not.toEqual(null);
  });

  test('SpeedLimitSelector is displayed', async () => {
    expect(playwrightHomePage.page.getByTestId('speed-limit-by-tag-selector')).not.toEqual(null);
  });
  test('Itinerary module and subcomponents are displayed', async () => {
    // Here is how to create a locator for a specific element
    const itinerary = playwrightHomePage.page.getByTestId('itinerary');
    expect(itinerary).not.toEqual(null);
    // here is how get locator inside another locator
    expect(itinerary.getByTestId('display-itinerary')).not.toEqual(null);
    // here is how you can chain locators
    expect(itinerary.getByTestId('display-itinerary').getByTestId('itinerary-origin')).not.toEqual(
      null
    );
    expect(itinerary.getByTestId('display-itinerary').getByTestId('itinerary-vias')).not.toEqual(
      null
    );
    expect(
      itinerary.getByTestId('display-itinerary').getByTestId('itinerary-destination')
    ).not.toEqual(null);
  });

  test('TrainLabels is displayed', async () => {
    expect(playwrightHomePage.page.getByTestId('add-train-labels')).not.toEqual(null);
  });

  test('TrainSchedules is displayed', async () => {
    expect(playwrightHomePage.page.getByTestId('add-train-schedules')).not.toEqual(null);
  });

  test('Map module is displayed', async () => {
    expect(playwrightHomePage.page.getByTestId('map')).not.toEqual(null);
  });
});
