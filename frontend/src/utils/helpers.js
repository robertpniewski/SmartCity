import { useEffect, useRef } from "react";
import { notify } from "react-notify-toast";

// https://stackoverflow.com/a/1527820/6841224
export const getRandomInt = (min, max) => {
  const minC = Math.ceil(min);
  const maxF = Math.floor(max);
  return Math.floor(Math.random() * (maxF - minC + 1)) + minC;
};

// https://stackoverflow.com/a/5365036/6841224
export const generateRandomColor = (minLight = 0, maxLight = 7) => {
  let result = 0;
  for (let i = 0; i < 6; ++i) {
    let colorDigit;
    if (i % 2 === 1) {
      colorDigit = getRandomInt(minLight, maxLight);
    } else {
      colorDigit = getRandomInt(0, 0xf);
    }
    result |= colorDigit << (4 * i);
  }

  return `#${result.toString(16)}`;
};

const precision = 1000; // lat and long precision, boost to 1000 if need be
const latOffset = 200; // Anything above 180 would do

export const getLocationHash = loc => {
  return Number(loc.lat * precision * latOffset) + Number(loc.lng * precision);
};

export const setInvalid = htmlELem => {
  htmlELem.classList.add("invalid-input");
};

export const setValid = htmlELem => {
  htmlELem.classList.remove("invalid-input");
};

function setIfValid(elem, parseFunc, min, max, setFunc) {
  const val = parseFunc(elem.value);
  // eslint-disable-next-line no-restricted-globals
  if (!isNaN(val) && val >= min && val <= max) {
    setValid(elem);
    setFunc(val);
  } else {
    setInvalid(elem);
  }
}

export const setIfValidInt = (e, min, max, setFunc) => {
  setIfValid(e.target, parseInt, min, max, setFunc);
};

export const setIfValidFloat = (e, min, max, setFunc) => {
  setIfValid(e.target, parseFloat, min, max, setFunc);
};

export const boolToInt = b => (b ? 1 : 0);

export const usePrevious = value => {
  const ref = useRef();
  useEffect(() => {
    ref.current = value;
  });
  return ref.current;
};

function degToRad(degrees) {
  return degrees * (Math.PI / 180);
}

function radToDeg(rad) {
  return rad * (180 / Math.PI);
}

// https://stackoverflow.com/a/18738281/6841224
export const angleFromCoordinates = (loc1, loc2) => {
  if (!loc1 || !loc2) {
    return 0;
  }

  const lat1Rad = degToRad(loc1.lat);
  const lat2Rad = degToRad(loc2.lat);

  const dLng = degToRad(loc2.lng - loc1.lng);

  const y = Math.sin(dLng) * Math.cos(lat2Rad);
  const x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLng);

  let heading = Math.atan2(y, x);
  heading = radToDeg(heading);
  heading = (heading + 360) % 360;
  // heading = 360 - heading; // count degrees counter-clockwise - remove to make clockwise

  return heading;
};

export const showQueued = notify.createShowQueue();
