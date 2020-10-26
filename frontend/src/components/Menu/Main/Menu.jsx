/* eslint-disable no-restricted-globals */
/* eslint-disable indent */
import { connect } from "react-redux";
import React, { useEffect, useState } from "react";

import Flatpickr from "react-flatpickr";
import "flatpickr/dist/themes/material_blue.css";

import { dispatch } from "../../../redux/store";
import { startSimulationDataUpdated } from "../../../redux/actions";
import PrepareMenu from "./PrepareMenu";
import CarsSubMenu from "./CarsSubMenu";
import SimulationStarter from "./SimulationStarter";
import "../../../styles/Menu.css";
import { StartState } from "../../../redux/models/startState";

const Menu = props => {
  const { wasStarted, shouldStart } = props;
  const [startTime, setTime] = useState(new Date());
  const onStart = () => {
    if (shouldStart === StartState.Invoke) {
      dispatch(startSimulationDataUpdated({ startTime }));
    }
  };
  useEffect(onStart, [shouldStart]);

  function evSetTime(newTime) {
    setTime(newTime[0]);
  }

  return (
    <div>
      <PrepareMenu />

      <form className="form-border">
        <CarsSubMenu />

        <div className="mt-3">
          <label htmlFor="simulationTime">Simulation time</label>
          <Flatpickr
            key="simulationTime"
            options={{
              enableTime: true,
              dateFormat: "M d H:i",
              time_24hr: true,
              allowInput: true,
              wrap: true,
              defaultDate: startTime,
            }}
            onChange={evSetTime}
          >
            <input type="text" className="form-control" disabled={wasStarted} placeholder="Select Date.." data-input />
          </Flatpickr>
        </div>
        <SimulationStarter />
      </form>
    </div>
  );
};

const mapStateToProps = (state /* , ownProps */) => {
  const { wasPrepared, wasStarted } = state.message;
  return {
    wasPrepared,
    wasStarted,
    shouldStart: state.interaction.shouldStart,
  };
};

export default connect(mapStateToProps)(React.memo(Menu));
