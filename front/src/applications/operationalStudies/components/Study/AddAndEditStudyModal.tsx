/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useContext, useState } from 'react';
import ModalHeaderSNCF from 'common/BootstrapSNCF/ModalSNCF/ModalHeaderSNCF';
import ModalBodySNCF from 'common/BootstrapSNCF/ModalSNCF/ModalBodySNCF';
import studyLogo from 'assets/pictures/views/studies.svg';
import { useTranslation } from 'react-i18next';
import InputSNCF from 'common/BootstrapSNCF/InputSNCF';
import TextareaSNCF from 'common/BootstrapSNCF/TextareaSNCF';
import ModalFooterSNCF from 'common/BootstrapSNCF/ModalSNCF/ModalFooterSNCF';
import { ModalContext } from 'common/BootstrapSNCF/ModalSNCF/ModalProvider';
import ChipsSNCF from 'common/BootstrapSNCF/ChipsSNCF';
import { FaPencilAlt, FaPlus, FaTrash } from 'react-icons/fa';
import { MdBusinessCenter, MdDescription, MdList, MdTitle } from 'react-icons/md';
import { RiCalendarLine, RiMoneyEuroCircleLine } from 'react-icons/ri';
import { useSelector, useDispatch } from 'react-redux';
import { getProjectID } from 'reducers/osrdconf/selectors';
import { deleteRequest, patch, post } from 'common/requests';
import { useNavigate } from 'react-router-dom';
import { updateStudyID } from 'reducers/osrdconf';
import { setSuccess } from 'reducers/main';
import { PROJECTS_URI, STUDIES_URI } from '../operationalStudiesConsts';

const configItemsDefaults = {
  name: '',
  type: '',
  description: '',
  service_code: '',
  business_code: '',
  start_date: null,
  expected_end_date: null,
  actual_end_date: null,
  state: '',
  tags: [],
  budget: 0,
};

type configItemsTypes = {
  id?: number;
  name: string;
  type: string;
  description: string;
  service_code: string;
  business_code: string;
  start_date: string | null;
  expected_end_date: string | null;
  actual_end_date: string | null;
  state: string;
  tags: string[];
  budget: number;
};

type Props = {
  editionMode: false;
  details?: configItemsTypes;
  getStudyDetail?: any;
};

export default function AddAndEditStudyModal({ editionMode, details, getStudyDetail }: Props) {
  const { t } = useTranslation('operationalStudies/study');
  const { closeModal } = useContext(ModalContext);
  const [configItems, setConfigItems] = useState<configItemsTypes>(details || configItemsDefaults);
  const [displayErrors, setDisplayErrors] = useState(false);
  const projectID = useSelector(getProjectID);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const formatDateForInput = (date: string | null) => {
    if (date === null) return '';
    return date.substr(0, 10);
  };

  const removeTag = (idx: number) => {
    const newTags: string[] = Array.from(configItems.tags);
    newTags.splice(idx, 1);
    setConfigItems({ ...configItems, tags: newTags });
  };

  const addTag = (tag: string) => {
    const newTags: string[] = Array.from(configItems.tags);
    newTags.push(tag);
    setConfigItems({ ...configItems, tags: newTags });
  };

  const createStudy = async () => {
    if (!configItems.name) {
      setDisplayErrors(true);
    } else {
      try {
        const result = await post(`${PROJECTS_URI}${projectID}${STUDIES_URI}`, configItems);
        dispatch(updateStudyID(result.id));
        navigate('/operational-studies/study');
        closeModal();
      } catch (error) {
        console.error(error);
      }
    }
  };

  const modifyStudy = async () => {
    if (!configItems.name) {
      setDisplayErrors(true);
    } else if (details) {
      try {
        await patch(`${PROJECTS_URI}${projectID}${STUDIES_URI}${details.id}/`, configItems);
        getStudyDetail(true);
        closeModal();
      } catch (error) {
        console.error(error);
      }
    }
  };

  const deleteStudy = async () => {
    if (details) {
      try {
        await deleteRequest(`${PROJECTS_URI}${projectID}${STUDIES_URI}${details.id}/`);
        dispatch(updateStudyID(undefined));
        navigate('/operational-studies/project');
        closeModal();
        dispatch(
          setSuccess({
            title: t('studyDeleted'),
            text: t('studyDeletedDetails', { name: details.name }),
          })
        );
      } catch (error) {
        console.error(error);
      }
    }
  };

  return (
    <div className="study-edition-modal">
      <ModalHeaderSNCF withCloseButton withBorderBottom>
        <h1 className="study-edition-modal-title">
          <img src={studyLogo} alt="Study Logo" />
          {editionMode ? t('studyModificationTitle') : t('studyCreationTitle')}
        </h1>
      </ModalHeaderSNCF>
      <ModalBodySNCF>
        <div className="study-edition-modal-name">
          <InputSNCF
            id="studyInputName"
            type="text"
            name="studyInputName"
            label={
              <div className="d-flex align-items-center">
                <span className="mr-2">
                  <MdTitle />
                </span>
                <span className="font-weight-bold">{t('studyName')}</span>
              </div>
            }
            value={configItems.name}
            onChange={(e: any) => setConfigItems({ ...configItems, name: e.target.value })}
            isInvalid={displayErrors && !configItems.name}
            errorMsg={displayErrors && !configItems.name ? t('studyNameMissing') : undefined}
          />
        </div>
        <div className="row">
          <div className="col-lg-8">
            <div className="study-edition-modal-type">
              <InputSNCF
                id="studyInputType"
                type="text"
                name="studyInputType"
                label={
                  <div className="d-flex align-items-center">
                    <span className="mr-2">
                      <MdList />
                    </span>
                    {t('studyType')}
                  </div>
                }
                value={configItems.type}
                onChange={(e: any) => setConfigItems({ ...configItems, type: e.target.value })}
              />
            </div>
            <div className="study-edition-modal-description">
              <TextareaSNCF
                id="studyDescription"
                label={
                  <div className="d-flex align-items-center">
                    <span className="mr-2">
                      <MdDescription />
                    </span>
                    {t('studyDescription')}
                  </div>
                }
                value={configItems.description}
                onChange={(e: any) =>
                  setConfigItems({ ...configItems, description: e.target.value })
                }
              />
            </div>
          </div>
          <div className="col-lg-4">
            <InputSNCF
              id="studyInputStartDate"
              type="date"
              name="studyInputStartDate"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2 text-success">
                    <RiCalendarLine />
                  </span>
                  {t('studyStartDate')}
                </div>
              }
              value={formatDateForInput(configItems.start_date)}
              onChange={(e: any) => setConfigItems({ ...configItems, start_date: e.target.value })}
            />
            <InputSNCF
              id="studyInputEstimatedEndingDate"
              type="date"
              name="studyInputEstimatedEndingDate"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2 text-warning">
                    <RiCalendarLine />
                  </span>
                  {t('studyEstimatedEndingDate')}
                </div>
              }
              value={formatDateForInput(configItems.expected_end_date)}
              onChange={(e: any) =>
                setConfigItems({ ...configItems, expected_end_date: e.target.value })
              }
            />
            <InputSNCF
              id="studyInputRealEndingDate"
              type="date"
              name="studyInputRealEndingDate"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2 text-danger">
                    <RiCalendarLine />
                  </span>
                  {t('studyRealEndingDate')}
                </div>
              }
              value={formatDateForInput(configItems.actual_end_date)}
              onChange={(e: any) =>
                setConfigItems({ ...configItems, actual_end_date: e.target.value })
              }
            />
          </div>
        </div>
        <div className="row">
          <div className="col-lg-4">
            <InputSNCF
              id="studyInputServiceCode"
              type="text"
              name="studyInputServiceCode"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2">
                    <MdBusinessCenter />
                  </span>
                  {t('studyServiceCode')}
                </div>
              }
              value={configItems.service_code}
              onChange={(e: any) =>
                setConfigItems({ ...configItems, service_code: e.target.value })
              }
            />
          </div>
          <div className="col-lg-4">
            <InputSNCF
              id="studyInputBusinessCode"
              type="text"
              name="studyInputBusinessCode"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2">
                    <MdBusinessCenter />
                  </span>
                  {t('studyBusinessCode')}
                </div>
              }
              value={configItems.business_code}
              onChange={(e: any) =>
                setConfigItems({ ...configItems, business_code: e.target.value })
              }
            />
          </div>
          <div className="col-lg-4">
            <InputSNCF
              id="studyInputBudget"
              type="number"
              name="studyInputBudget"
              unit="€"
              label={
                <div className="d-flex align-items-center">
                  <span className="mr-2">
                    <RiMoneyEuroCircleLine />
                  </span>
                  {t('studyBudget')}
                </div>
              }
              value={configItems.budget}
              onChange={(e: any) => setConfigItems({ ...configItems, budget: e.target.value })}
            />
          </div>
        </div>
        <ChipsSNCF
          addTag={addTag}
          tags={configItems.tags}
          removeTag={removeTag}
          title={t('studyTags')}
          color="primary"
        />
      </ModalBodySNCF>
      <ModalFooterSNCF>
        <div className="d-flex justify-content-end w-100">
          {editionMode && (
            <button className="btn btn-outline-danger mr-auto" type="button" onClick={deleteStudy}>
              <span className="mr-2">
                <FaTrash />
              </span>
              {t('studyDeleteButton')}
            </button>
          )}
          <button className="btn btn-secondary mr-2" type="button" onClick={closeModal}>
            {t('studyCancel')}
          </button>
          {editionMode ? (
            <button className="btn btn-warning" type="button" onClick={modifyStudy}>
              <span className="mr-2">
                <FaPencilAlt />
              </span>
              {t('studyModifyButton')}
            </button>
          ) : (
            <button className="btn btn-primary" type="button" onClick={createStudy}>
              <span className="mr-2">
                <FaPlus />
              </span>
              {t('studyCreateButton')}
            </button>
          )}
        </div>
      </ModalFooterSNCF>
    </div>
  );
}