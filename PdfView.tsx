import React, {useEffect, useRef} from 'react';
import {
  PixelRatio,
  UIManager,
  findNodeHandle,
  Dimensions,
} from 'react-native';

import {PdfViewManager} from './PdfViewManager';

const createFragment = viewId =>
  UIManager.dispatchViewManagerCommand(
    viewId,
    // we are calling the 'create' command
    UIManager.PdfViewManager.Commands.create.toString(),
    [viewId],
  );

export const PdfView = () => {
  const ref = useRef(null);

  useEffect(() => {
    const viewId = findNodeHandle(ref.current);
    createFragment(viewId);
  }, []);

  return (
    <PdfViewManager
      style={{
        // converts dpi to px, provide desired height
        height: PixelRatio.getPixelSizeForLayoutSize(Dimensions.get('window').height),
        // converts dpi to px, provide desired width
        width: PixelRatio.getPixelSizeForLayoutSize(Dimensions.get('window').width),
      }}
      ref={ref}
      documentURL = "https://www.africau.edu/images/default/sample.pdf"
      onChange={event => {
        console.log(event.nativeEvent);
      }}
    />
  );
};