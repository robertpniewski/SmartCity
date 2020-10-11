import React from "react";
import { stationIcon } from "../../styles/icons";
import { Marker, Popup } from "react-leaflet";

const Station = props => {
  const { station } = props;

  return (
    <Marker position={station.location} opacity={0.95} icon={stationIcon} zIndexOffset={10}>
      <Popup>I am a station!</Popup>
    </Marker>
  );
};

export default Station;
