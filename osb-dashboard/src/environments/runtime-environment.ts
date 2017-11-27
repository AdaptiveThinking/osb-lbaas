// this var may be injected via index.html and server-side include
const injectedEnv = window['INJECTED_ENVIRONMENT'];
if (!injectedEnv) {
  // tslint:disable-next-line:no-console
  console.warn('DASHBOARD DEFAULT DEVELOPMENT ENV ACTIVE - DID YOU FORGET TO INJECT A RUNTIME ENV?');
}

export interface Environment {
  serviceInstanceId: string;
  token: string;
  production: boolean;
  baseUrls: {
    serviceBrokerUrl: string;
  };
  ui: {
    title: string;
    logoSrc: string;
  };
};

// we use quoutes here because that makes it easier to copy config to nginx.conf or cf manifest files
// tslint:disable:quotemark
const seedEnv = {
  serviceInstanceId: 'd82ca436-afc4-4a66-8a69-b7520cf0a7b7',
  token: 'bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImxlZ2FjeS10b2tlbi1rZXkiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiI4NmMxNzRmNjc5OWI0MjIzYjFhM2VkY2EyNjBkYTA0NCIsInN1YiI6ImIzYzUxOWFjLTQ4YWEtNDJiNS04NTljLTc5YTg4ZDQ3YjYzMyIsInNjb3BlIjpbInJvdXRpbmcucm91dGVyX2dyb3Vwcy5yZWFkIiwiY2xvdWRfY29udHJvbGxlci5yZWFkIiwicGFzc3dvcmQud3JpdGUiLCJjbG91ZF9jb250cm9sbGVyLndyaXRlIiwib3BlbmlkIiwibmV0d29yay5hZG1pbiIsImRvcHBsZXIuZmlyZWhvc2UiLCJzY2ltLndyaXRlIiwic2NpbS5yZWFkIiwiY2xvdWRfY29udHJvbGxlci5hZG1pbiIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiJiM2M1MTlhYy00OGFhLTQyYjUtODU5Yy03OWE4OGQ0N2I2MzMiLCJvcmlnaW4iOiJ1YWEiLCJ1c2VyX25hbWUiOiJhZG1pbiIsImVtYWlsIjoiYWRtaW4iLCJyZXZfc2lnIjoiZmMyMTRiMjgiLCJpYXQiOjE1MTE3OTA0ODgsImV4cCI6MTUxMTc5MTA4OCwiaXNzIjoiaHR0cHM6Ly91YWEuY2YuZGV2LmV1LWRlLWNlbnRyYWwubXNoLmhvc3Qvb2F1dGgvdG9rZW4iLCJ6aWQiOiJ1YWEiLCJhdWQiOlsic2NpbSIsImNsb3VkX2NvbnRyb2xsZXIiLCJwYXNzd29yZCIsImNmIiwidWFhIiwib3BlbmlkIiwiZG9wcGxlciIsInJvdXRpbmcucm91dGVyX2dyb3VwcyIsIm5ldHdvcmsiXX0.SFgX75wi0S4hujUlb6R17jmdR16demULIN5Vv_tzBakBNd-xK21c90giJ94R-tkzdPaC3GFokMNcvihj2sfOxcqLinz3FdjB018OP74jdC7m6BoKR0RGYu7X1USDXJguq4f4V8dcu_vBgw3jtGodFjlw7L_DcimI5QKNzzmPdus',
  production: false,
  baseUrls: {
    serviceBrokerUrl: 'https://lbaas-dev.cf.dev.eu-de-central.msh.host',
  },
  ui: {
    title: "Service Broker Panel",
    logoSrc: "./assets/core/sb-white.svg"
  }
};

// overwrite default env with injected vars
export const environment: Environment = Object.assign({}, seedEnv, injectedEnv);
