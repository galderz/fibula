package org.mendrugo.fibula.extension.runtime;

import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

@Recorder
public class FibulaRecorder
{
    static final Logger log = Logger.getLogger(FibulaRecorder.class);

    public void log(String name)
    {
        System.out.println(name);
        log.info(name);
    }
}
