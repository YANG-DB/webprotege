package edu.stanford.bmir.protege.web.client.frame;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 27/02/2014
 */
public interface PropertyValueValueChangedHandler extends EventHandler {

    void handlePropertyValueChanged(PropertyValueValueChangedEvent event);
}
