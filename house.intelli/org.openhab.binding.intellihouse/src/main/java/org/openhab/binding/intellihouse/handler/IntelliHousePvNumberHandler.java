package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntelliHousePvNumberHandler extends IntelliHousePvHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHousePvNumberHandler.class);

    public IntelliHousePvNumberHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected String getAcceptedItemType() {
        return "Number";
    }
}
