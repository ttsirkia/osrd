/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useContext, useEffect, useState } from 'react';
import ModalHeaderSNCF from 'common/BootstrapSNCF/ModalSNCF/ModalHeaderSNCF';
import ModalBodySNCF from 'common/BootstrapSNCF/ModalSNCF/ModalBodySNCF';
import scenarioLogo from 'assets/pictures/views/studies.svg';
import { useTranslation } from 'react-i18next';
import InputSNCF from 'common/BootstrapSNCF/InputSNCF';
import TextareaSNCF from 'common/BootstrapSNCF/TextareaSNCF';
import ModalFooterSNCF from 'common/BootstrapSNCF/ModalSNCF/ModalFooterSNCF';
import { ModalContext } from 'common/BootstrapSNCF/ModalSNCF/ModalProvider';
import ChipsSNCF from 'common/BootstrapSNCF/ChipsSNCF';
import { FaPencilAlt, FaPlus, FaTrash } from 'react-icons/fa';
import { MdDescription, MdTitle } from 'react-icons/md';
import InfraSelectorModal from 'common/InfraSelector/InfraSelectorModal';
import { getInfraID, getProjectID, getStudyID } from 'reducers/osrdconf/selectors';
import { useSelector, useDispatch } from 'react-redux';
import { deleteRequest, get, patch, post } from 'common/requests';
import { useNavigate } from 'react-router-dom';
import { updateScenarioID } from 'reducers/osrdconf';
import { setSuccess } from 'reducers/main';
import { scenarioTypes } from 'applications/operationalStudies/components/operationalStudiesTypes';
import { GiElectric } from 'react-icons/gi';
import SelectImprovedSNCF from 'common/BootstrapSNCF/SelectImprovedSNCF';
import {
  PROJECTS_URI,
  SCENARIOS_URI,
  STUDIES_URI,
  ELECTRICAL_PROFILE_SET_URI,
} from '../operationalStudiesConsts';

const scenarioTypesDefaults = {
  name: '',
  description: '',
  infra: undefined,
  tags: [],
};

type Props = {
  editionMode: false;
  scenario?: scenarioTypes;
  getScenario?: any;
};

export default function AddOrEditScenarioModal({ editionMode, scenario, getScenario }: Props) {
  const { t } = useTranslation('operationalStudies/scenario');
  const { closeModal } = useContext(ModalContext);
  const [currentScenario, setCurrentScenario] = useState<scenarioTypes>(
    scenario || scenarioTypesDefaults
  );
  const [displayErrors, setDisplayErrors] = useState(false);
  const [electricalProfileSetOptions, setElectricalProfileSetOptions] = useState([
    {
      key: undefined,
      value: t('dontUseElectricalProfileSet'),
    },
  ]);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const projectID = useSelector(getProjectID);
  const studyID = useSelector(getStudyID);
  const infraID = useSelector(getInfraID);
  const [selectedValue, setSelectedValue] = useState();

  type ElectricalProfileSetType = { id: number | undefined; name: string };
  type SelectOptionsType = { key: number | undefined; value: string };
  const createElectricalProfileSetOptions = async () => {
    try {
      const results = await get(ELECTRICAL_PROFILE_SET_URI);
      results.sort((a: ElectricalProfileSetType, b: ElectricalProfileSetType) =>
        a.name.localeCompare(b.name)
      );
      const options = [
        {
          key: undefined,
          value: t('noElectricalProfileSet'),
        },
        ...results.map((option: ElectricalProfileSetType) => ({
          key: option.id,
          value: option.name,
        })),
      ];
      setSelectedValue(
        options.find((option) => option.key === currentScenario.electrical_profile_set)
      );
      setElectricalProfileSetOptions(options);
    } catch (error) {
      console.log(error);
    }
  };

  const rootURI = `${PROJECTS_URI}${projectID}${STUDIES_URI}${studyID}${SCENARIOS_URI}`;

  const removeTag = (idx: number) => {
    const newTags: string[] = Array.from(currentScenario.tags);
    newTags.splice(idx, 1);
    setCurrentScenario({ ...currentScenario, tags: newTags });
  };

  const addTag = (tag: string) => {
    const newTags: string[] = Array.from(currentScenario.tags);
    newTags.push(tag);
    setCurrentScenario({ ...currentScenario, tags: newTags });
  };

  const createScenario = async () => {
    if (!currentScenario.name || !currentScenario.infra) {
      setDisplayErrors(true);
    } else {
      try {
        const result = await post(`${rootURI}`, { ...currentScenario, infra: infraID });
        dispatch(updateScenarioID(result.id));
        navigate('/operational-studies/scenario');
        closeModal();
      } catch (error) {
        console.error(error);
      }
    }
  };

  const updateScenario = async () => {
    if (!currentScenario.name) {
      setDisplayErrors(true);
    } else if (scenario) {
      try {
        await patch(`${rootURI}${scenario.id}/`, currentScenario);
        getScenario(true);
        closeModal();
      } catch (error) {
        console.error(error);
      }
    }
  };

  const deleteScenario = async () => {
    if (scenario) {
      try {
        await deleteRequest(`${rootURI}${scenario.id}/`);
        dispatch(updateScenarioID(undefined));
        navigate('/operational-studies/study');
        closeModal();
        dispatch(
          setSuccess({
            title: t('scenarioDeleted'),
            text: t('scenarioDeletedDetails', { name: scenario.name }),
          })
        );
      } catch (error) {
        console.error(error);
      }
    }
  };

  useEffect(() => {
    setCurrentScenario({ ...currentScenario, infra: infraID });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [infraID]);

  useEffect(() => {
    createElectricalProfileSetOptions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return selectedValue && electricalProfileSetOptions ? (
    <div className="scenario-edition-modal">
      <ModalHeaderSNCF withCloseButton withBorderBottom>
        <h1 className="scenario-edition-modal-title">
          <img src={scenarioLogo} alt="scenario Logo" />
          {editionMode ? t('scenarioModificationTitle') : t('scenarioCreationTitle')}
        </h1>
      </ModalHeaderSNCF>
      <ModalBodySNCF>
        <div className="row">
          <div className={editionMode ? 'col-lg-12' : 'col-lg-6'}>
            <div className="scenario-edition-modal-name">
              <InputSNCF
                id="scenarioInputName"
                type="text"
                name="scenarioInputName"
                label={
                  <div className="d-flex align-items-center">
                    <span className="mr-2">
                      <MdTitle />
                    </span>
                    <span className="font-weight-bold">{t('scenarioName')}</span>
                  </div>
                }
                value={currentScenario.name}
                onChange={(e: any) =>
                  setCurrentScenario({ ...currentScenario, name: e.target.value })
                }
                isInvalid={displayErrors && !currentScenario.name}
                errorMsg={
                  displayErrors && !currentScenario.name ? t('scenarioNameMissing') : undefined
                }
              />
            </div>
            <div className="scenario-edition-modal-description">
              <TextareaSNCF
                id="scenarioDescription"
                label={
                  <div className="d-flex align-items-center">
                    <span className="mr-2">
                      <MdDescription />
                    </span>
                    {t('scenarioDescription')}
                  </div>
                }
                value={currentScenario.description}
                onChange={(e: any) =>
                  setCurrentScenario({ ...currentScenario, description: e.target.value })
                }
              />
            </div>
            <div className="scenario-edition-modal-description">
              <SelectImprovedSNCF
                title={
                  <div className="d-flex align-items-center">
                    <span className="mr-2">
                      <GiElectric />
                    </span>
                    {t('electricalProfileSet')}
                  </div>
                }
                selectedValue={selectedValue}
                options={electricalProfileSetOptions}
                onChange={(e: SelectOptionsType) =>
                  setCurrentScenario({ ...currentScenario, electrical_profile_set: e.key })
                }
              />
            </div>
            <ChipsSNCF
              addTag={addTag}
              tags={currentScenario.tags}
              removeTag={removeTag}
              title={t('scenarioTags')}
              color="secondary"
            />
          </div>
          {!editionMode && (
            <div className="col-lg-6">
              <div
                className={`scenario-edition-modal-infraselector ${
                  displayErrors && !currentScenario.infra
                    ? 'scenario-edition-modal-infraselector-missing'
                    : null
                }`}
              >
                <InfraSelectorModal onlySelectionMode />
              </div>
            </div>
          )}
        </div>
      </ModalBodySNCF>
      <ModalFooterSNCF>
        <div className="d-flex justify-content-end w-100 mt-3">
          {editionMode && (
            <button
              className="btn btn-sm btn-outline-danger mr-auto"
              type="button"
              onClick={deleteScenario}
            >
              <span className="mr-2">
                <FaTrash />
              </span>
              {t('scenarioDeleteButton')}
            </button>
          )}
          <button className="btn btn-sm btn-secondary mr-2" type="button" onClick={closeModal}>
            {t('scenarioCancel')}
          </button>
          {editionMode ? (
            <button className="btn btn-sm btn-warning" type="button" onClick={updateScenario}>
              <span className="mr-2">
                <FaPencilAlt />
              </span>
              {t('scenarioModifyButton')}
            </button>
          ) : (
            <button className="btn btn-sm btn-primary" type="button" onClick={createScenario}>
              <span className="mr-2">
                <FaPlus />
              </span>
              {t('scenarioCreateButton')}
            </button>
          )}
        </div>
      </ModalFooterSNCF>
    </div>
  ) : null;
}
