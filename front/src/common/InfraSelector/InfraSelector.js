import React, { useState, useEffect } from 'react';
import { PropTypes } from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { useTranslation } from 'react-i18next';
import { setFailure } from 'reducers/main';
import { get } from 'common/requests';
import icon from 'assets/pictures/tracks.png';
import InfraSelectorModal from 'common/InfraSelector/InfraSelectorModal';
import nextId from 'react-id-generator';
import { getInfraID } from 'reducers/osrdconf/selectors';
import { FaLock } from 'react-icons/fa';
import './InfraSelector.scss';
import { INFRA_URL } from './Consts';

export default function InfraSelector(props) {
  const { modalOnly, modalID } = props;
  const dispatch = useDispatch();
  const [selectedInfra, setSelectedInfra] = useState(undefined);
  const infraID = useSelector(getInfraID);

  const { t } = useTranslation(['infraMnagement']);

  const getInfra = async (id) => {
    try {
      const infraQuery = await get(`${INFRA_URL}${id}/`, {});
      setSelectedInfra(infraQuery);
    } catch (e) {
      dispatch(
        setFailure({
          name: t('errorMessages.unableToRetrieveInfra'),
          message: e.message,
        })
      );
      console.log('ERROR', e);
    }
  };

  useEffect(() => {
    if (infraID !== undefined) {
      getInfra(infraID);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [infraID]);

  if (modalOnly) return <InfraSelectorModal modalID={modalID} />;

  return (
    <>
      <div className="osrd-config-item mb-2">
        <div
          className="osrd-config-item-container osrd-config-item-clickable"
          role="button"
          tabIndex="-1"
          data-toggle="modal"
          data-target={`#${modalID}`}
        >
          <div className="infraselector-button">
            <img width="32px" className="mr-2" src={icon} alt="infraIcon" />
            {selectedInfra !== undefined ? (
              <>
                <span className="">{selectedInfra.name}</span>
                <span className="ml-1 small align-self-center">({selectedInfra.id})</span>
                {selectedInfra.locked ? (
                  <span className="infra-lock ml-auto">
                    <FaLock />
                  </span>
                ) : null}
              </>
            ) : (
              t('infraManagement:chooseInfrastructure')
            )}
          </div>
        </div>
      </div>
      <InfraSelectorModal modalID={modalID} />
    </>
  );
}

InfraSelector.defaultProps = {
  modalOnly: false,
  modalID: `infra-selector-modal-${nextId()}`,
};
InfraSelector.propTypes = {
  modalOnly: PropTypes.bool,
  modalID: PropTypes.string,
};