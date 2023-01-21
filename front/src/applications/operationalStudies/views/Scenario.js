import React, { useEffect, useState } from 'react';
import NavBarSNCF from 'common/BootstrapSNCF/NavBarSNCF';
import logo from 'assets/pictures/home/operationalStudies.svg';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import Timetable from 'applications/operationalStudies/components/Scenario/Timetable';
import infraLogo from 'assets/pictures/components/tracks.svg';
import ScenarioLoader from 'applications/operationalStudies/components/Scenario/ScenarioLoader';
import { useSelector } from 'react-redux';
import OSRDSimulation from './OSRDSimulation/OSRDSimulation';
import { projectJSON, scenarioJSON, studyJSON } from '../components/Helpers/genFakeDataForProjects';

function BreadCrumbs(props) {
  const { t } = useTranslation('operationalStudies/project');
  const { projectName, studyName, scenarioName } = props;
  return (
    <div className="navbar-breadcrumbs">
      <Link to="/operational-studies">{t('projectsList')}</Link>
      <i className="icons-arrow-next icons-size-x75 text-muted" />
      <Link to="/operational-studies/project">{projectName}</Link>
      <i className="icons-arrow-next icons-size-x75 text-muted" />
      <Link to="/operational-studies/study">{studyName}</Link>
      <i className="icons-arrow-next icons-size-x75 text-muted" />
      {scenarioName}
    </div>
  );
}

export default function Scenario() {
  const { t } = useTranslation('operationalStudies/scenario');
  const isUpdating = useSelector((state) => state.osrdsimulation.isUpdating);
  const [projectDetails, setProjectDetails] = useState();
  const [studyDetails, setStudyDetails] = useState();
  const [scenarioDetails, setScenarioDetails] = useState();

  useEffect(() => {
    setProjectDetails(projectJSON());
    setStudyDetails(studyJSON());
    setScenarioDetails(scenarioJSON());
  }, []);
  return (
    <>
      <NavBarSNCF
        appName={
          <BreadCrumbs
            projectName={projectDetails ? projectDetails.name : null}
            studyName={studyDetails ? studyDetails.name : null}
            scenarioName={scenarioDetails ? scenarioDetails.name : null}
          />
        }
        logo={logo}
      />
      <main className="mastcontainer mastcontainer-no-mastnav">
        <div className="scenario">
          {isUpdating && <ScenarioLoader msg={t('isUpdating')} />}
          <div className="row">
            <div className="col-lg-4">
              <div className="scenario-sidemenu">
                {scenarioDetails && (
                  <div className="scenario-details">
                    <div className="scenario-details-name">{scenarioDetails.name}</div>
                    <div className="scenario-details-infra-name">
                      <img src={infraLogo} alt="Infra logo" className="mr-2" />
                      {scenarioDetails.infra_name}
                    </div>
                    <div className="scenario-details-description">
                      {scenarioDetails.description}
                    </div>
                  </div>
                )}
                <Timetable />
              </div>
            </div>
            <div className="col-lg-8">
              <div className="scenario-results">
                <OSRDSimulation />
              </div>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}
