declare module "react-qr-scanner" {
  import * as React from "react";

  export interface QrScannerProps {
    onScan?: (data: string | null) => void;
    onError?: (err: any) => void;
    delay?: number;
    style?: React.CSSProperties;
    constraints?: MediaTrackConstraints;
  }

  export default class QrScanner extends React.Component<QrScannerProps> {}
}
