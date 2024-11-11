package br.com.armandoalmeida;

import br.com.armandoalmeida.pointer.MouseManager;
import br.com.armandoalmeida.util.CustomLogger;

import java.util.logging.Level;

public class MouseIdleStats {

    private static final CustomLogger log = CustomLogger.getLogger(Level.INFO, MouseIdleStats.class.getName());

    public static void main(String[] args) {
        boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");

        MouseManager mouseManager = new MouseManager(keepOsAlive);
        mouseManager.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mouseManager.stop();
            } finally {
                log.shutdown();
            }
        }));

    }

}
