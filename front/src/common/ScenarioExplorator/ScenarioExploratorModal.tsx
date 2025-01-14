import React, { useEffect, useMemo, useState } from 'react';
import ModalHeaderSNCF from 'common/BootstrapSNCF/ModalSNCF/ModalHeaderSNCF';
import ModalBodySNCF from 'common/BootstrapSNCF/ModalSNCF/ModalBodySNCF';
import scenarioExploratorLogo from 'assets/pictures/views/scenarioExplorator.svg';
import projectsLogo from 'assets/pictures/views/projects.svg';
import studiesLogo from 'assets/pictures/views/studies.svg';
import scenariosLogo from 'assets/pictures/views/scenarios.svg';
import {
  PROJECTS_URI,
  STUDIES_URI,
  SCENARIOS_URI,
} from 'applications/operationalStudies/components/operationalStudiesConsts';
import {
  projectTypes,
  studyTypes,
  scenarioTypes,
} from 'applications/operationalStudies/components/operationalStudiesTypes';
import { useTranslation } from 'react-i18next';
import { MdArrowRight } from 'react-icons/md';
import { get } from 'common/requests';
import nextId from 'react-id-generator';
import { useSelector } from 'react-redux';
import { getProjectID, getScenarioID, getStudyID } from 'reducers/osrdconf/selectors';
import { FilterParams } from './ScenarioExploratorTypes';
import ProjectMiniCard from './ScenarioExploratorModalProjectMiniCard';
import StudyMiniCard from './ScenarioExploratorModalStudyMiniCard';
import ScenarioMiniCard from './ScenarioExploratorModalScenarioMiniCard';

export default function ScenarioExploratorModal() {
  const { t } = useTranslation('common/scenarioExplorator');

  const globalProjectID = useSelector(getProjectID);
  const globalStudyID = useSelector(getStudyID);
  const globalScenarioID = useSelector(getScenarioID);

  const [projectID, setProjectID] = useState<number | undefined>(globalProjectID);
  const [studyID, setStudyID] = useState<number | undefined>(globalStudyID);
  const [scenarioID, setScenarioID] = useState<number | undefined>(globalScenarioID);
  const [projectsList, setProjectsList] = useState<projectTypes[]>();
  const [studiesList, setStudiesList] = useState<studyTypes[]>();
  const [scenariosList, setScenariosList] = useState<scenarioTypes[]>();

  const grabItemsList = async (
    url: string,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setFunction: (arg0: any) => void,
    params?: FilterParams
  ) => {
    try {
      const data = await get(url, { params });
      setFunction(data.results);
    } catch (error) {
      console.log(error);
    }
  };

  useEffect(() => {
    grabItemsList(PROJECTS_URI, setProjectsList, { ordering: 'name' });
  }, []);

  useEffect(() => {
    if (projectID) {
      grabItemsList(`${PROJECTS_URI}${projectID}${STUDIES_URI}`, setStudiesList, {
        ordering: 'name',
      });
    }
  }, [projectID]);

  useEffect(() => {
    setScenariosList(undefined);
  }, [studiesList]);

  useEffect(() => {
    if (projectID && studyID) {
      grabItemsList(
        `${PROJECTS_URI}${projectID}${STUDIES_URI}${studyID}${SCENARIOS_URI}`,
        setScenariosList,
        {
          ordering: 'name',
        }
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studyID]);

  return (
    <div className="scenario-explorator-modal">
      <ModalHeaderSNCF withCloseButton>
        <h1 className="scenario-explorator-modal-title">
          <img src={scenarioExploratorLogo} alt="Scenario explorator Logo" />
          {t('scenarioExplorator')}
        </h1>
      </ModalHeaderSNCF>
      <ModalBodySNCF>
        <div className="row">
          <div className="col-lg-4">
            <div className="scenario-explorator-modal-part projects">
              <div className="scenario-explorator-modal-part-title">
                <img src={projectsLogo} alt="projects logo" />
                <h2>{t('projects')}</h2>
                {projectsList && (
                  <span className="scenario-explorator-modal-part-title-count">
                    {projectsList.length}
                  </span>
                )}
              </div>
              <div className="scenario-explorator-modal-part-itemslist">
                {useMemo(
                  () =>
                    projectsList &&
                    projectsList.map((project) => (
                      <ProjectMiniCard
                        project={project}
                        setSelectedID={setProjectID}
                        isSelected={project.id === projectID}
                        key={nextId()}
                      />
                    )),
                  [projectsList, projectID]
                )}
              </div>
              <div className="scenario-explorator-modal-part-arrow">
                <MdArrowRight />
              </div>
            </div>
          </div>
          <div className="col-lg-4">
            <div className="scenario-explorator-modal-part studies">
              <div className="scenario-explorator-modal-part-title">
                <img src={studiesLogo} alt="studies logo" />
                <h2>{t('studies')}</h2>
                {studiesList && (
                  <span className="scenario-explorator-modal-part-title-count">
                    {studiesList.length}
                  </span>
                )}
              </div>
              <div className="scenario-explorator-modal-part-itemslist">
                {useMemo(
                  () =>
                    studiesList &&
                    studiesList.map((study) => (
                      <StudyMiniCard
                        study={study}
                        setSelectedID={setStudyID}
                        isSelected={study.id === studyID}
                        key={nextId()}
                      />
                    )),
                  [studiesList, studyID]
                )}
              </div>
              <div className="scenario-explorator-modal-part-arrow">
                <MdArrowRight />
              </div>
            </div>
          </div>
          <div className="col-lg-4">
            <div className="scenario-explorator-modal-part scenarios">
              <div className="scenario-explorator-modal-part-title">
                <img src={scenariosLogo} alt="scenarios logo" />
                <h2>{t('scenarios')}</h2>
                {scenariosList && (
                  <span className="scenario-explorator-modal-part-title-count">
                    {scenariosList.length}
                  </span>
                )}
              </div>
              <div className="scenario-explorator-modal-part-itemslist">
                {useMemo(
                  () =>
                    projectID &&
                    studyID &&
                    scenariosList &&
                    scenariosList.map((scenario) => (
                      <ScenarioMiniCard
                        scenario={scenario}
                        setSelectedID={setScenarioID}
                        isSelected={scenario.id === scenarioID}
                        projectID={projectID}
                        studyID={studyID}
                        key={nextId()}
                      />
                    )),
                  [scenariosList, scenarioID]
                )}
              </div>
            </div>
          </div>
        </div>
      </ModalBodySNCF>
    </div>
  );
}
