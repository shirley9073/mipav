import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.plugins.*; // needed to load PlugInAlgorithm / PlugInView / PlugInFile interface

import gov.nih.mipav.view.*;

import java.awt.*;


/**
 * PlugIn to take the absolute value of the image.
 *
 * @see  PlugInAlgorithm
 */
public class PlugInISN implements PlugInAlgorithm {

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * defines body of run method, which was declared in the interface. This sample run method creates a new image in a
     * new frame and calls methods in PlugInSampleDrawing to add a few VOIs.
     *
     * @param  parentFrame  parent frame
     * @param  image        current ModelImage - this is an image already loaded into MIPAV. Can be null.
     *
     * @see    ModelImage
     * @see    ViewJFrameImage
     */
    public void run(Frame parentFrame, ModelImage image) {

        if (parentFrame instanceof ViewJFrameImage) {
            new PlugInDialogISN(parentFrame, image);
        } else {
            MipavUtil.displayError("PlugIn Wrap Fix only runs on an image frame.");
        }
    }
}
