/* eslint-disable no-restricted-globals */
/* eslint-disable indent */
import { connect } from "react-redux";
import React, { useEffect, useState } from "react";

import Flatpickr from "react-flatpickr";
import "flatpickr/dist/themes/material_blue.css";

import { dispatch } from "../../../redux/store";
import { startSimulationDataUpdated } from "../../../redux/core/actions";
import PrepareMenu from "./PrepareMenu";
import SubMenu from "./SubMenu";
import SimulationStarter from "./SimulationStarter";
import "../../../styles/Menu.css";
import { StartState } from "../../../redux/models/startState";
import CustomClock from "./CustomClock";
import { D_START_TIME } from "../../../constants/defaults";

const Menu = props => {
  const { wasStarted, shouldStart } = props;
  const [startTime, setTime] = useState(D_START_TIME);
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
      {!wasStarted && <PrepareMenu />}

      <form className="form-border">
        <SubMenu />

        <div className="mt-3">
          <label htmlFor="simulationTime">Simulation start time</label>
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

      {wasStarted && <CustomClock />}
    </div>
  );
};

const mapStateToProps = (state /* , ownProps */) => {
  const { wasStarted } = state.message;
  const { shouldStart } = state.interaction;
  return {
    wasStarted,
    shouldStart,
  };
};

export default connect(mapStateToProps)(React.memo(Menu));