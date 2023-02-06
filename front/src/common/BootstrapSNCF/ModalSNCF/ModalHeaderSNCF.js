import React from 'react';
import PropTypes from 'prop-types';

export default class ModalHeaderSNCF extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.node), PropTypes.node]).isRequired,
  };

  render() {
    const { children } = this.props;
    return <div className="modal-header">{children}</div>;
  }
}